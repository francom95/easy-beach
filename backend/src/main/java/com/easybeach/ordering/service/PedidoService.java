package com.easybeach.ordering.service;

import com.easybeach.catalog.domain.Producto;
import com.easybeach.catalog.domain.ProductoVariante;
import com.easybeach.catalog.repository.ProductoRepository;
import com.easybeach.catalog.repository.ProductoVarianteRepository;
import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.domain.Pedido;
import com.easybeach.ordering.domain.PedidoEvento;
import com.easybeach.ordering.domain.PedidoItem;
import com.easybeach.ordering.domain.PedidoPromocion;
import com.easybeach.ordering.repository.PedidoEventoRepository;
import com.easybeach.ordering.repository.PedidoRepository;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.payments.service.PagoService;
import com.easybeach.promotions.CalculadoraPromociones;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.realtime.TiempoRealService;
import com.easybeach.shared.tenancy.TenantContext;
import com.easybeach.shared.tenancy.TenantFilterService;
import com.easybeach.stay.domain.Estadia;
import com.easybeach.stay.domain.EstadoEstadia;
import com.easybeach.stay.repository.EstadiaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El corazón transaccional (etapa 13). Reglas que no se negocian:
 * <ul>
 *   <li>Solo una estadía {@code ACTIVA} (validada por carpero) puede pedir.</li>
 *   <li><b>Los precios los pone el servidor</b>, leídos del catálogo al
 *       momento del pedido y congelados en {@code pedido_item}.</li>
 *   <li>Idempotencia por {@code Idempotency-Key}: el reintento por mala señal
 *       devuelve el pedido original, no crea uno nuevo.</li>
 *   <li>Un pedido no entra a la cola operativa sin pago aprobado.</li>
 * </ul>
 */
@Service
public class PedidoService {

    /** ARS con 2 decimales; HALF_UP es lo esperable para precios en pesos. */
    private static final int ESCALA_MONTO = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final PedidoRepository pedidoRepository;
    private final PedidoEventoRepository eventoRepository;
    private final ProductoRepository productoRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final EstadiaRepository estadiaRepository;
    private final PagoService pagoService;
    private final CalculadoraPromociones calculadoraPromociones;
    private final TiempoRealService tiempoRealService;
    private final TenantFilterService tenantFilterService;

    public PedidoService(PedidoRepository pedidoRepository, PedidoEventoRepository eventoRepository,
                          ProductoRepository productoRepository, ProductoVarianteRepository varianteRepository,
                          EstadiaRepository estadiaRepository, PagoService pagoService,
                          CalculadoraPromociones calculadoraPromociones, TiempoRealService tiempoRealService,
                          TenantFilterService tenantFilterService) {
        this.pedidoRepository = pedidoRepository;
        this.eventoRepository = eventoRepository;
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.estadiaRepository = estadiaRepository;
        this.pagoService = pagoService;
        this.calculadoraPromociones = calculadoraPromociones;
        this.tiempoRealService = tiempoRealService;
        this.tenantFilterService = tenantFilterService;
    }

    @Transactional
    public Pedido crear(Long clienteId, String clientePublicId, String idempotencyKey, CrearPedidoRequest request) {
        Estadia estadia = estadiaRepository.findByPublicId(request.estadiaPublicId())
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!estadia.getClienteId().equals(clienteId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        if (estadia.getEstado() != EstadoEstadia.ACTIVA) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "La estadía debe estar ACTIVA para pedir (estado actual: " + estadia.getEstado() + ")");
        }

        Long balnearioId = estadia.getBalnearioId();
        TenantContext.set(balnearioId);
        tenantFilterService.applyCurrentTenant();

        // Idempotencia: si ya existe un pedido con esta clave, se devuelve tal cual.
        var existente = pedidoRepository.findByBalnearioIdAndIdempotencyKey(balnearioId, idempotencyKey);
        if (existente.isPresent()) {
            return existente.get();
        }

        Pedido pedido = new Pedido();
        pedido.setBalnearioId(balnearioId);
        pedido.setEstadiaId(estadia.getId());
        pedido.setClienteId(clienteId);
        pedido.setClientePublicId(clientePublicId);
        pedido.setUbicacionId(estadia.getUbicacionId());
        pedido.setIdempotencyKey(idempotencyKey);
        pedido.setEstado(EstadoPedido.CREADO);

        List<CalculadoraPromociones.LineaPedido> lineasParaPromos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CrearPedidoRequest.ItemRequest item : request.items()) {
            Producto producto = productoRepository.findById(item.productoId())
                    .filter(p -> p.getBalnearioId().equals(balnearioId))
                    .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO,
                            "Producto inexistente o de otro balneario"));
            if (!producto.isDisponible()) {
                throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                        "El producto '" + producto.getNombre() + "' no está disponible");
            }

            BigDecimal precioUnitario;
            ProductoVariante variante = null;
            List<ProductoVariante> variantesDelProducto =
                    varianteRepository.findByProductoIdAndDisponibleTrueOrderByOrdenAsc(producto.getId());

            if (item.productoVarianteId() != null) {
                variante = varianteRepository.findById(item.productoVarianteId())
                        .filter(v -> v.getBalnearioId().equals(balnearioId)
                                && v.getProducto().getId().equals(producto.getId()))
                        .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO,
                                "Variante inexistente para ese producto"));
                if (!variante.isDisponible()) {
                    throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                            "La variante '" + variante.getNombre() + "' no está disponible");
                }
                precioUnitario = variante.getPrecio();
            } else {
                // Etapa 03 §3.4: con variantes disponibles, elegir una es obligatorio.
                if (!variantesDelProducto.isEmpty()) {
                    throw new ApiException(ErrorCode.VALIDACION_FALLIDA,
                            "El producto '" + producto.getNombre() + "' requiere elegir una variante");
                }
                precioUnitario = producto.getPrecioBase();
            }

            BigDecimal subtotalLinea = redondear(precioUnitario.multiply(BigDecimal.valueOf(item.cantidad())));

            PedidoItem pedidoItem = new PedidoItem();
            pedidoItem.setBalnearioId(balnearioId);
            pedidoItem.setProductoId(producto.getId());
            pedidoItem.setProductoVarianteId(variante == null ? null : variante.getId());
            // Congelados: el histórico sigue legible aunque el catálogo cambie.
            pedidoItem.setNombreProducto(producto.getNombre());
            pedidoItem.setNombreVariante(variante == null ? null : variante.getNombre());
            pedidoItem.setPrecioUnitario(redondear(precioUnitario));
            pedidoItem.setCantidad(item.cantidad());
            pedidoItem.setSubtotalLinea(subtotalLinea);
            pedido.agregarItem(pedidoItem);

            subtotal = subtotal.add(subtotalLinea);
            lineasParaPromos.add(new CalculadoraPromociones.LineaPedido(producto.getId(),
                    producto.getCategoria().getId(), variante == null ? null : variante.getId(),
                    precioUnitario, item.cantidad()));
        }

        subtotal = redondear(subtotal);
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        for (var descuento : calculadoraPromociones.calcular(balnearioId, lineasParaPromos)) {
            PedidoPromocion promo = new PedidoPromocion();
            promo.setBalnearioId(balnearioId);
            promo.setPromocionId(descuento.promocionId());
            promo.setNombrePromocion(descuento.nombre());
            promo.setMontoDescuento(redondear(descuento.monto()));
            pedido.agregarPromocion(promo);
            descuentoTotal = descuentoTotal.add(promo.getMontoDescuento());
        }
        descuentoTotal = redondear(descuentoTotal);
        // Un descuento nunca puede dejar el total en negativo.
        if (descuentoTotal.compareTo(subtotal) > 0) {
            descuentoTotal = subtotal;
        }

        pedido.setSubtotal(subtotal);
        pedido.setDescuentoTotal(descuentoTotal);
        pedido.setTotal(redondear(subtotal.subtract(descuentoTotal)));

        try {
            pedido = pedidoRepository.saveAndFlush(pedido);
        } catch (DataIntegrityViolationException e) {
            // Carrera de dos reintentos simultáneos con la misma clave: gana el
            // UK, y el perdedor devuelve el pedido que ya existe.
            //
            // Etapa 19 (QA, intento de fix real revertido): se probó aislar
            // este insert en su propia transacción REQUIRES_NEW para blindar
            // el fallback contra la sesión de Hibernate que queda en estado
            // indefinido tras un flush fallido - pero el resto de este método
            // (registrarEvento, aplicarTransicion, iniciarPago) sigue
            // trabajando sobre `pedido` en la sesión de ESTE método, y una vez
            // que el insert vive en OTRA transacción/EntityManager, `pedido`
            // vuelve detached para esta sesión: eso rompía la creación NORMAL
            // (sin ninguna carrera) con un StaleObjectStateException al
            // primer save() posterior. Se revirtió: bajo una carrera realmente
            // simultánea (poco común - en la práctica el segundo reintento
            // casi siempre llega después de que el primero ya commiteó, y
            // entra por el chequeo `existente.isPresent()` de arriba, no por
            // acá) el perdedor puede ver una excepción cruda de Hibernate acá
            // en vez de la respuesta idempotente esperada. Documentado como
            // hallazgo de backlog, no bloqueante.
            return pedidoRepository.findByBalnearioIdAndIdempotencyKey(balnearioId, idempotencyKey)
                    .orElseThrow(() -> new ApiException(ErrorCode.CONFLICTO_DE_ESTADO, "No se pudo crear el pedido"));
        }
        registrarEvento(pedido, null, EstadoPedido.CREADO, null, "CLIENTE", null);

        // El pedido pasa a "esperando pago" ANTES de cobrar: si MP resuelve
        // sincrónicamente, el listener de PagoResuelto encuentra el pedido ya
        // en PAGO_PENDIENTE y puede confirmarlo. Invertir este orden deja el
        // pedido colgado en PAGO_PENDIENTE para siempre.
        aplicarTransicion(pedido, EstadoPedido.PAGO_PENDIENTE, null, "SISTEMA", null);

        // Cobro: el monto es el total calculado por el servidor, nunca el del cliente.
        pagoService.iniciarPago(pedido.getId(), balnearioId, pedido.getTotal(),
                idempotencyKey, "Pedido " + pedido.getPublicId(), request.cardToken());

        // Si el pago ya se resolvió, el listener movió el pedido; se recarga
        // para devolver el estado real.
        return pedidoRepository.findById(pedido.getId()).orElse(pedido);
    }

    /** Llamado por el listener de {@code PagoResuelto} (evento de {@code payments}). */
    @Transactional
    public void aplicarResultadoDePago(Long pedidoId, boolean aprobado) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null || pedido.getEstado() != EstadoPedido.PAGO_PENDIENTE) {
            return; // fuera de orden o ya resuelto: no se pisa nada
        }
        EstadoPedido destino = aprobado ? EstadoPedido.CONFIRMADO : EstadoPedido.PAGO_RECHAZADO;
        aplicarTransicion(pedido, destino, null, "SISTEMA", null);

        if (aprobado) {
            // Recién ahora el pedido existe para la cocina.
            tiempoRealService.emitirAOperativo(pedido.getBalnearioId(), "pedido.nuevo",
                    Map.of("pedidoPublicId", pedido.getPublicId(), "total", pedido.getTotal().toString()));
        }
    }

    @Transactional
    public Pedido transicionarPorStaff(Long balnearioId, Long actorUsuarioId, String publicId,
                                        EstadoPedido destino, String motivo) {
        Pedido pedido = obtenerDelBalneario(balnearioId, publicId);
        if (destino == EstadoPedido.CANCELADO) {
            if (motivo == null || motivo.isBlank()) {
                throw new ApiException(ErrorCode.VALIDACION_FALLIDA, "Cancelar exige un motivo");
            }
            if (!pedido.getEstado().cancelablePorLocal()) {
                throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                        "No se puede cancelar un pedido " + pedido.getEstado());
            }
            boolean requiereReembolso = pedido.getEstado().tienePagoAprobado();
            aplicarTransicion(pedido, EstadoPedido.CANCELADO, actorUsuarioId, "STAFF", motivo);
            pedido.setMotivoCancelacion(motivo);
            if (requiereReembolso) {
                pagoService.reembolsar(pedido.getId());
            }
            return pedidoRepository.save(pedido);
        }
        exigirTransicion(pedido, destino);
        aplicarTransicion(pedido, destino, actorUsuarioId, "STAFF", null);
        return pedido;
    }

    @Transactional
    public Pedido cancelarPorCliente(Long clienteId, String publicId, String motivo) {
        Pedido pedido = obtenerPropioDelCliente(clienteId, publicId);
        if (!pedido.getEstado().cancelablePorCliente()) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Ya no podés cancelar este pedido (estado: " + pedido.getEstado() + "); pedile al personal");
        }
        aplicarTransicion(pedido, EstadoPedido.CANCELADO, clienteId, "CLIENTE", motivo);
        pedido.setMotivoCancelacion(motivo);
        return pedidoRepository.save(pedido);
    }

    /** Cola operativa: solo lo que ya tiene pago aprobado. */
    @Transactional(readOnly = true)
    public List<Pedido> colaOperativa(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return pedidoRepository.findByBalnearioIdAndEstadoInOrderByCreatedAtAsc(balnearioId,
                List.of(EstadoPedido.CONFIRMADO, EstadoPedido.EN_PREPARACION, EstadoPedido.EN_CAMINO));
    }

    @Transactional(readOnly = true)
    public List<Pedido> misPedidosDeEstadia(Long clienteId, String estadiaPublicId) {
        Estadia estadia = estadiaRepository.findByPublicId(estadiaPublicId)
                .filter(e -> e.getClienteId().equals(clienteId))
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        return pedidoRepository.findByEstadiaIdOrderByCreatedAtDesc(estadia.getId());
    }

    @Transactional(readOnly = true)
    public Pedido obtenerPropioDelCliente(Long clienteId, String publicId) {
        return pedidoRepository.findByPublicId(publicId)
                .filter(p -> p.getClienteId().equals(clienteId))
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
    }

    @Transactional(readOnly = true)
    public List<PedidoEvento> historial(Long pedidoId) {
        return eventoRepository.findByPedidoIdOrderByCreatedAtAsc(pedidoId);
    }

    /**
     * Etapa 19 (QA): antes era privado y solo lo usaba {@code transicionarPorStaff}
     * - el endpoint de historial operativo resolvía el pedido filtrando
     * {@link #colaOperativa}, que a propósito excluye estados terminales
     * (ENTREGADO/CANCELADO). Resultado real encontrado en vivo: en cuanto un
     * pedido se entregaba, el staff perdía para siempre la posibilidad de ver
     * su historial. Se expone público para que el controller pueda buscar
     * por publicId+balneario sin pasar por la cola activa.
     */
    @Transactional(readOnly = true)
    public Pedido obtenerDelBalneario(Long balnearioId, String publicId) {
        return pedidoRepository.findByPublicId(publicId)
                .filter(p -> p.getBalnearioId().equals(balnearioId))
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
    }

    private void exigirTransicion(Pedido pedido, EstadoPedido destino) {
        if (!pedido.getEstado().puedeTransicionarA(destino)) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Transición inválida: " + pedido.getEstado() + " -> " + destino);
        }
    }

    /** Toda transición: cambia el estado, deja rastro en el historial y avisa por SSE. */
    private void aplicarTransicion(Pedido pedido, EstadoPedido destino, Long actorUsuarioId,
                                    String actorTipo, String motivo) {
        EstadoPedido anterior = pedido.getEstado();
        pedido.transicionarA(destino);
        pedidoRepository.save(pedido);
        registrarEvento(pedido, anterior, destino, actorUsuarioId, actorTipo, motivo);
        notificar(pedido, destino);
    }

    private void registrarEvento(Pedido pedido, EstadoPedido anterior, EstadoPedido nuevo,
                                  Long actorUsuarioId, String actorTipo, String motivo) {
        PedidoEvento evento = new PedidoEvento();
        evento.setPedidoId(pedido.getId());
        evento.setBalnearioId(pedido.getBalnearioId());
        evento.setEstadoAnterior(anterior);
        evento.setEstadoNuevo(nuevo);
        evento.setActorUsuarioId(actorUsuarioId);
        evento.setActorTipo(actorTipo);
        evento.setMotivo(motivo);
        eventoRepository.save(evento);
    }

    private void notificar(Pedido pedido, EstadoPedido destino) {
        Map<String, Object> payload = Map.of(
                "pedidoPublicId", pedido.getPublicId(),
                "estado", destino.name());
        // Al cliente por su propio canal (indexado por ULID, no por id numérico);
        // al staff solo lo que ya entró a la cola o se canceló.
        tiempoRealService.emitirACliente(pedido.getClientePublicId(), "pedido.estado", payload);
        if (destino.entroACola() || destino == EstadoPedido.CANCELADO) {
            tiempoRealService.emitirAOperativo(pedido.getBalnearioId(), "pedido.estado", payload);
        }
    }

    private BigDecimal redondear(BigDecimal monto) {
        return monto.setScale(ESCALA_MONTO, REDONDEO);
    }
}
