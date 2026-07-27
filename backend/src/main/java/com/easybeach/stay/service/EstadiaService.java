package com.easybeach.stay.service;

import com.easybeach.platform.domain.Balneario;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.platform.service.BalnearioService;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.tenancy.TenantContext;
import com.easybeach.shared.tenancy.TenantFilterService;
import com.easybeach.stay.domain.Estadia;
import com.easybeach.stay.domain.EstadiaUbicacionHistorial;
import com.easybeach.stay.domain.EstadoEstadia;
import com.easybeach.stay.domain.EstadoUbicacion;
import com.easybeach.stay.domain.Ubicacion;
import com.easybeach.stay.event.EstadiaAbierta;
import com.easybeach.stay.event.EstadiaCerrada;
import com.easybeach.stay.repository.EstadiaRepository;
import com.easybeach.stay.repository.EstadiaUbicacionHistorialRepository;
import com.easybeach.stay.repository.UbicacionRepository;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.ResumenCierreResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida de la estadía activa (etapa 12). Concentra las reglas de
 * negocio más finas del producto; ver {@link EstadoEstadia} para la máquina
 * de estados.
 *
 * <p><b>Sobre el tenant:</b> el cliente NO lleva {@code balneario_id} en su
 * token (etapa 05 §1.2) - su tenant sale del recurso validado en servidor.
 * Por eso los métodos del cliente resuelven el balneario desde el slug o
 * desde la propia estadía y setean {@link TenantContext} a mano antes de
 * tocar entidades tenant-scoped.
 */
@Service
public class EstadiaService {

    /** Decisión de negocio (etapa 12): una solicitud sin validar expira a los 60 minutos. */
    public static final Duration TTL_VALIDACION = Duration.ofMinutes(60);

    private final EstadiaRepository estadiaRepository;
    private final EstadiaUbicacionHistorialRepository historialRepository;
    private final UbicacionRepository ubicacionRepository;
    private final BalnearioRepository balnearioRepository;
    private final BalnearioService balnearioService;
    private final ConsumoEstadiaProvider consumoProvider;
    private final TenantFilterService tenantFilterService;
    private final ApplicationEventPublisher eventPublisher;

    public EstadiaService(EstadiaRepository estadiaRepository,
                           EstadiaUbicacionHistorialRepository historialRepository,
                           UbicacionRepository ubicacionRepository,
                           BalnearioRepository balnearioRepository,
                           BalnearioService balnearioService,
                           ConsumoEstadiaProvider consumoProvider,
                           TenantFilterService tenantFilterService,
                           ApplicationEventPublisher eventPublisher) {
        this.estadiaRepository = estadiaRepository;
        this.historialRepository = historialRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.balnearioRepository = balnearioRepository;
        this.balnearioService = balnearioService;
        this.consumoProvider = consumoProvider;
        this.tenantFilterService = tenantFilterService;
        this.eventPublisher = eventPublisher;
    }

    // ------------------------------------------------------------------ cliente

    /**
     * Paso 1 de la apertura en dos pasos: el cliente solicita, queda
     * {@code PENDIENTE_VALIDACION} hasta que un carpero la confirme.
     */
    @Transactional
    public EstadiaResponse solicitar(Long clienteId, String balnearioSlug, Long ubicacionId) {
        Balneario balneario = balnearioRepository.findBySlug(balnearioSlug)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!balnearioService.esOperativo(balneario.getId())) {
            throw new ApiException(ErrorCode.BALNEARIO_NO_OPERATIVO,
                    "El balneario no está operativo: no acepta estadías nuevas");
        }

        TenantContext.set(balneario.getId());
        tenantFilterService.applyCurrentTenant();

        Ubicacion ubicacion = ubicacionRepository.findById(ubicacionId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!ubicacion.getBalnearioId().equals(balneario.getId())) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        if (ubicacion.getEstado() != EstadoUbicacion.ACTIVA) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO, "Esa ubicación no está disponible");
        }

        // Chequeo amable para dar un error claro; la garantía REAL es el UK de
        // la DB (ver catch de abajo), que es lo que sostiene la regla bajo
        // concurrencia. El chequeo previo no es la defensa, es la cortesía.
        estadiaRepository.findByBalnearioIdAndActivaUk(balneario.getId(), clienteId).ifPresent(existente -> {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Ya tenés una estadía " + existente.getEstado() + " en este balneario");
        });

        Estadia estadia = new Estadia();
        estadia.setBalnearioId(balneario.getId());
        estadia.setClienteId(clienteId);
        estadia.setUbicacionId(ubicacionId);
        estadia.setEstado(EstadoEstadia.PENDIENTE_VALIDACION);
        estadia.setFechaSolicitud(Instant.now());

        try {
            estadia = estadiaRepository.saveAndFlush(estadia);
        } catch (DataIntegrityViolationException e) {
            // Dos solicitudes simultáneas del mismo cliente en el mismo balneario:
            // el UK (balneario_id, activa_uk) deja pasar una sola. La perdedora
            // llega acá - la regla se cumple aunque el chequeo previo no la haya visto.
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Ya tenés una estadía abierta en este balneario");
        }

        EstadiaUbicacionHistorial tramo = new EstadiaUbicacionHistorial();
        tramo.setEstadiaId(estadia.getId());
        tramo.setBalnearioId(balneario.getId());
        tramo.setUbicacionId(ubicacionId);
        tramo.setDesde(Instant.now());
        historialRepository.save(tramo);

        return toResponse(estadia, balneario, ubicacion);
    }

    /**
     * "Mi estadía actual": el re-ingreso diario a la app no debe tener
     * fricción (etapa 07).
     *
     * <p><b>Excepción documentada al filtro de tenant</b> (como el login de
     * staff en {@code UsuarioBalnearioRolRepository} o el callback de MP):
     * el cliente puede tener estadías en VARIOS balnearios a la vez (etapa
     * 01), así que no hay un tenant único que aplicar. El aislamiento acá lo
     * da el ownership: la query filtra por {@code clienteId}, que sale del
     * token, nunca de un parámetro.
     */
    @Transactional(readOnly = true)
    public List<EstadiaResponse> misEstadiasVigentes(Long clienteId) {
        return estadiaRepository.findVigentesDelCliente(clienteId).stream()
                .map(this::toResponseResolviendoRelaciones)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EstadiaResponse> miHistorial(Long clienteId) {
        return estadiaRepository.findByClienteIdOrderByFechaSolicitudDesc(clienteId).stream()
                .map(this::toResponseResolviendoRelaciones)
                .toList();
    }

    /** Hoy carpa 12, mañana carpa 15: preserva el historial cerrando el tramo anterior. */
    @Transactional
    public EstadiaResponse cambiarUbicacion(Long clienteId, String publicId, Long nuevaUbicacionId) {
        Estadia estadia = obtenerPropiaDelCliente(clienteId, publicId);
        if (estadia.getEstado() != EstadoEstadia.ACTIVA) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Solo una estadía ACTIVA puede cambiar de ubicación");
        }
        TenantContext.set(estadia.getBalnearioId());
        tenantFilterService.applyCurrentTenant();

        Ubicacion nueva = ubicacionRepository.findById(nuevaUbicacionId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!nueva.getBalnearioId().equals(estadia.getBalnearioId())) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        if (nueva.getEstado() != EstadoUbicacion.ACTIVA) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO, "Esa ubicación no está disponible");
        }

        Instant ahora = Instant.now();
        historialRepository.findByEstadiaIdAndHastaIsNull(estadia.getId()).ifPresent(tramoAbierto -> {
            tramoAbierto.setHasta(ahora);
            historialRepository.save(tramoAbierto);
        });
        EstadiaUbicacionHistorial nuevoTramo = new EstadiaUbicacionHistorial();
        nuevoTramo.setEstadiaId(estadia.getId());
        nuevoTramo.setBalnearioId(estadia.getBalnearioId());
        nuevoTramo.setUbicacionId(nuevaUbicacionId);
        nuevoTramo.setDesde(ahora);
        historialRepository.save(nuevoTramo);

        estadia.setUbicacionId(nuevaUbicacionId);
        return toResponseResolviendoRelaciones(estadiaRepository.save(estadia));
    }

    /**
     * Cierre explícito del cliente, con resumen de consumo. Decisión de
     * negocio (etapa 12): si quedan pedidos en curso, el cierre se BLOQUEA.
     */
    @Transactional
    public ResumenCierreResponse cerrar(Long clienteId, String publicId) {
        Estadia estadia = obtenerPropiaDelCliente(clienteId, publicId);
        if (estadia.getEstado() != EstadoEstadia.ACTIVA) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Solo una estadía ACTIVA puede cerrarse (estado actual: " + estadia.getEstado() + ")");
        }
        if (consumoProvider.tienePedidosEnCurso(estadia.getId())) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "No podés cerrar la estadía con pedidos en curso; esperá a que se entreguen o cancelalos");
        }
        // Recién acá sabemos a qué balneario pertenece: se setea el tenant antes
        // de tocar el historial (tenant-scoped) por defensa en profundidad.
        TenantContext.set(estadia.getBalnearioId());
        tenantFilterService.applyCurrentTenant();
        return cerrarInterno(estadia, EstadoEstadia.CERRADA);
    }

    // ------------------------------------------------------------------ carpero

    /** Cola de validación del panel operativo (etapa 17), más antiguas primero. */
    @Transactional(readOnly = true)
    public List<EstadiaResponse> pendientesDeValidacion(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return estadiaRepository
                .findByBalnearioIdAndEstadoOrderByFechaSolicitudAsc(balnearioId, EstadoEstadia.PENDIENTE_VALIDACION)
                .stream()
                .map(this::toResponseResolviendoRelaciones)
                .toList();
    }

    /** Paso 2 de la apertura: el carpero confirma y recién ahí se habilitan los pedidos. */
    @Transactional
    public EstadiaResponse validar(Long balnearioId, Long carperoUsuarioId, String publicId) {
        Estadia estadia = obtenerDelBalneario(balnearioId, publicId);
        exigirTransicion(estadia, EstadoEstadia.ACTIVA);

        estadia.transicionarA(EstadoEstadia.ACTIVA);
        estadia.setValidadaPorUsuarioId(carperoUsuarioId);
        estadia.setFechaValidacion(Instant.now());
        estadia = estadiaRepository.save(estadia);

        eventPublisher.publishEvent(new EstadiaAbierta(estadia.getId(), estadia.getBalnearioId(),
                estadia.getClienteId(), Instant.now()));
        return toResponseResolviendoRelaciones(estadia);
    }

    @Transactional
    public EstadiaResponse rechazar(Long balnearioId, Long carperoUsuarioId, String publicId, String motivo) {
        Estadia estadia = obtenerDelBalneario(balnearioId, publicId);
        exigirTransicion(estadia, EstadoEstadia.RECHAZADA);

        estadia.transicionarA(EstadoEstadia.RECHAZADA);
        estadia.setValidadaPorUsuarioId(carperoUsuarioId);
        estadia.setFechaValidacion(Instant.now());
        estadia.setMotivoRechazo(motivo);
        return toResponseResolviendoRelaciones(estadiaRepository.save(estadia));
    }

    // ------------------------------------------------------------------ sistema

    /**
     * Job de expiración: libera el cupo de las solicitudes que nadie validó
     * dentro del TTL, para que el cliente pueda volver a solicitar.
     * Cross-tenant a propósito (lo corre el sistema, no un usuario).
     *
     * @return cuántas expiró.
     */
    @Transactional
    public int expirarPendientesVencidas() {
        Instant limite = Instant.now().minus(TTL_VALIDACION);
        List<Estadia> vencidas = estadiaRepository.findPendientesVencidas(limite);
        for (Estadia estadia : vencidas) {
            estadia.transicionarA(EstadoEstadia.EXPIRADA);
        }
        estadiaRepository.saveAll(vencidas);
        return vencidas.size();
    }

    /** Cierre administrativo (fin de temporada). Distinguible del cierre del cliente en reportes. */
    @Transactional
    public ResumenCierreResponse cerrarPorSistema(Estadia estadia) {
        return cerrarInterno(estadia, EstadoEstadia.CERRADA_POR_SISTEMA);
    }

    /** Regla de la etapa 11 diferida hasta acá: una ubicación con estadía viva no se desactiva. */
    @Transactional(readOnly = true)
    public boolean ubicacionTieneEstadiaVigente(Long ubicacionId) {
        return estadiaRepository.existsByUbicacionIdAndEstadoIn(ubicacionId,
                List.of(EstadoEstadia.PENDIENTE_VALIDACION, EstadoEstadia.ACTIVA));
    }

    // ------------------------------------------------------------------ interno

    private ResumenCierreResponse cerrarInterno(Estadia estadia, EstadoEstadia estadoFinal) {
        Instant ahora = Instant.now();
        estadia.transicionarA(estadoFinal);
        estadia.setFechaCierre(ahora);
        estadia = estadiaRepository.save(estadia);

        historialRepository.findByEstadiaIdAndHastaIsNull(estadia.getId()).ifPresent(tramoAbierto -> {
            tramoAbierto.setHasta(ahora);
            historialRepository.save(tramoAbierto);
        });

        ConsumoEstadiaProvider.ResumenConsumo consumo = consumoProvider.obtenerResumen(estadia.getId());
        eventPublisher.publishEvent(new EstadiaCerrada(estadia.getId(), estadia.getBalnearioId(),
                estadia.getClienteId(), estadoFinal, ahora));

        String balnearioNombre = balnearioRepository.findById(estadia.getBalnearioId())
                .map(Balneario::getNombre).orElse(null);
        long dias = Math.max(1, ChronoUnit.DAYS.between(estadia.getFechaSolicitud(), ahora) + 1);

        return new ResumenCierreResponse(estadia.getPublicId(), balnearioNombre, estadia.getFechaSolicitud(),
                ahora, dias, consumo.cantidadPedidos(), consumo.montoTotal());
    }

    private void exigirTransicion(Estadia estadia, EstadoEstadia destino) {
        if (!estadia.getEstado().puedeTransicionarA(destino)) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Transición inválida: " + estadia.getEstado() + " -> " + destino);
        }
    }

    /** Ownership del cliente: una estadía ajena es 404, no 403 (etapa 05 §2 regla 1). */
    private Estadia obtenerPropiaDelCliente(Long clienteId, String publicId) {
        Estadia estadia = estadiaRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!estadia.getClienteId().equals(clienteId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        return estadia;
    }

    private Estadia obtenerDelBalneario(Long balnearioId, String publicId) {
        Estadia estadia = estadiaRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!estadia.getBalnearioId().equals(balnearioId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        return estadia;
    }

    private EstadiaResponse toResponseResolviendoRelaciones(Estadia estadia) {
        Balneario balneario = balnearioRepository.findById(estadia.getBalnearioId()).orElse(null);
        Ubicacion ubicacion = ubicacionRepository.findById(estadia.getUbicacionId()).orElse(null);
        return toResponse(estadia, balneario, ubicacion);
    }

    private EstadiaResponse toResponse(Estadia estadia, Balneario balneario, Ubicacion ubicacion) {
        return new EstadiaResponse(
                estadia.getPublicId(),
                estadia.getBalnearioId(),
                Optional.ofNullable(balneario).map(Balneario::getNombre).orElse(null),
                estadia.getUbicacionId(),
                Optional.ofNullable(ubicacion).map(Ubicacion::getIdentificador).orElse(null),
                estadia.getEstado().name(),
                estadia.getEstado().permitePedidos(),
                estadia.getFechaSolicitud(),
                estadia.getFechaValidacion(),
                estadia.getFechaCierre(),
                estadia.getMotivoRechazo());
    }
}
