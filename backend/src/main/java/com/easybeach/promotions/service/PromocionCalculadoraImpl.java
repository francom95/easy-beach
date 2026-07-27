package com.easybeach.promotions.service;

import com.easybeach.promotions.domain.EstadoPromocion;
import com.easybeach.promotions.domain.Promocion;
import com.easybeach.promotions.domain.PromocionAlcance;
import com.easybeach.promotions.domain.PromocionComboItem;
import com.easybeach.promotions.domain.TipoAlcance;
import com.easybeach.promotions.domain.TipoPromocion;
import com.easybeach.promotions.repository.PromocionAlcanceRepository;
import com.easybeach.promotions.repository.PromocionComboItemRepository;
import com.easybeach.promotions.repository.PromocionRepository;
import com.easybeach.shared.tenancy.TenantFilterService;
import com.easybeach.promotions.CalculadoraPromociones;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación real de {@link CalculadoraPromociones} (etapa 14),
 * reemplazando a {@code SinPromocionesCalculadora} de la etapa 13 - Spring
 * la prefiere automáticamente por ser un {@code @Component} concreto frente
 * al {@code @ConditionalOnMissingBean} de la neutra.
 *
 * <p><b>Regla de combinación (decisión de negocio, etapa 14):</b> las
 * promociones <b>se acumulan</b> - todas las que apliquen a una línea suman
 * su descuento, sin elegir "la mejor". Cada promoción aporta su propia fila
 * en {@code pedido_promocion}, así el ticket muestra de dónde sale cada
 * descuento.
 */
@Component
public class PromocionCalculadoraImpl implements CalculadoraPromociones {

    private final PromocionRepository promocionRepository;
    private final PromocionAlcanceRepository alcanceRepository;
    private final PromocionComboItemRepository comboItemRepository;
    private final VigenciaPromocionChecker vigenciaChecker;
    private final TenantFilterService tenantFilterService;

    public PromocionCalculadoraImpl(PromocionRepository promocionRepository,
                                     PromocionAlcanceRepository alcanceRepository,
                                     PromocionComboItemRepository comboItemRepository,
                                     VigenciaPromocionChecker vigenciaChecker,
                                     TenantFilterService tenantFilterService) {
        this.promocionRepository = promocionRepository;
        this.alcanceRepository = alcanceRepository;
        this.comboItemRepository = comboItemRepository;
        this.vigenciaChecker = vigenciaChecker;
        this.tenantFilterService = tenantFilterService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DescuentoAplicado> calcular(Long balnearioId, List<LineaPedido> lineas) {
        tenantFilterService.applyCurrentTenant();
        if (lineas.isEmpty()) {
            return List.of();
        }

        List<Promocion> vigentes = promocionRepository.findByBalnearioIdAndEstado(balnearioId, EstadoPromocion.ACTIVA)
                .stream()
                .filter(vigenciaChecker::estaVigenteAhora)
                .toList();
        if (vigentes.isEmpty()) {
            return List.of();
        }

        List<Long> ids = vigentes.stream().map(Promocion::getId).toList();
        Map<Long, List<PromocionAlcance>> alcancesPorPromo = alcanceRepository.findByPromocionIdIn(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(PromocionAlcance::getPromocionId));
        Map<Long, List<PromocionComboItem>> comboItemsPorPromo = comboItemRepository.findByPromocionIdIn(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(PromocionComboItem::getPromocionId));

        List<DescuentoAplicado> descuentos = new ArrayList<>();
        for (Promocion promocion : vigentes) {
            DescuentoAplicado descuento = switch (promocion.getTipo()) {
                case DESCUENTO_PORCENTUAL, HAPPY_HOUR ->
                        calcularPorcentual(promocion, alcancesPorPromo.getOrDefault(promocion.getId(), List.of()), lineas);
                case COMBO ->
                        calcularCombo(promocion, comboItemsPorPromo.getOrDefault(promocion.getId(), List.of()), lineas);
            };
            if (descuento != null && descuento.monto().compareTo(BigDecimal.ZERO) > 0) {
                descuentos.add(descuento);
            }
        }
        return descuentos;
    }

    private DescuentoAplicado calcularPorcentual(Promocion promocion, List<PromocionAlcance> alcances,
                                                  List<LineaPedido> lineas) {
        if (alcances.isEmpty()) {
            return null;
        }
        var productosAlcanzados = alcances.stream()
                .filter(a -> a.getTipoAlcance() == TipoAlcance.PRODUCTO)
                .map(PromocionAlcance::getReferenciaId)
                .collect(java.util.stream.Collectors.toSet());
        var categoriasAlcanzadas = alcances.stream()
                .filter(a -> a.getTipoAlcance() == TipoAlcance.CATEGORIA)
                .map(PromocionAlcance::getReferenciaId)
                .collect(java.util.stream.Collectors.toSet());

        BigDecimal baseAlcanzada = lineas.stream()
                .filter(l -> productosAlcanzados.contains(l.productoId())
                        || categoriasAlcanzadas.contains(l.categoriaId()))
                .map(LineaPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (baseAlcanzada.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal monto = baseAlcanzada.multiply(promocion.getValor())
                .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        return new DescuentoAplicado(promocion.getId(), promocion.getNombre(), monto);
    }

    /**
     * Cuántas veces "cabe" el combo en el pedido (mínimo entre todos sus
     * ítems) y el descuento es la diferencia contra el precio normal de esa
     * combinación, usando el precio promedio ponderado del producto en el
     * pedido (soporta que el mismo producto aparezca en más de una línea,
     * ej. variantes distintas).
     */
    private DescuentoAplicado calcularCombo(Promocion promocion, List<PromocionComboItem> items,
                                             List<LineaPedido> lineas) {
        if (items.isEmpty()) {
            return null;
        }
        Map<Long, Integer> cantidadPorProducto = new HashMap<>();
        Map<Long, BigDecimal> subtotalPorProducto = new HashMap<>();
        for (LineaPedido linea : lineas) {
            cantidadPorProducto.merge(linea.productoId(), linea.cantidad(), Integer::sum);
            subtotalPorProducto.merge(linea.productoId(), linea.subtotal(), BigDecimal::add);
        }

        int veces = Integer.MAX_VALUE;
        for (PromocionComboItem item : items) {
            Integer disponible = cantidadPorProducto.get(item.getProductoId());
            if (disponible == null || disponible < item.getCantidad()) {
                return null; // no están todos los productos del combo en cantidad suficiente
            }
            veces = Math.min(veces, disponible / item.getCantidad());
        }
        if (veces <= 0 || veces == Integer.MAX_VALUE) {
            return null;
        }

        BigDecimal precioNormalUnaVez = BigDecimal.ZERO;
        for (PromocionComboItem item : items) {
            BigDecimal precioPromedio = subtotalPorProducto.get(item.getProductoId())
                    .divide(BigDecimal.valueOf(cantidadPorProducto.get(item.getProductoId())), 4,
                            java.math.RoundingMode.HALF_UP);
            precioNormalUnaVez = precioNormalUnaVez.add(precioPromedio.multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        BigDecimal descuentoUnaVez = precioNormalUnaVez.subtract(promocion.getValor());
        if (descuentoUnaVez.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // el combo no es más barato que el precio normal: no corresponde descuento
        }
        BigDecimal montoTotal = descuentoUnaVez.multiply(BigDecimal.valueOf(veces));
        return new DescuentoAplicado(promocion.getId(), promocion.getNombre(), montoTotal);
    }
}
