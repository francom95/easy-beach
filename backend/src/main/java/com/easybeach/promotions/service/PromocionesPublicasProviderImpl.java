package com.easybeach.promotions.service;

import com.easybeach.catalog.service.PromocionesPublicasProvider;
import com.easybeach.promotions.domain.EstadoPromocion;
import com.easybeach.promotions.domain.Promocion;
import com.easybeach.promotions.domain.PromocionAlcance;
import com.easybeach.promotions.domain.TipoAlcance;
import com.easybeach.promotions.domain.TipoPromocion;
import com.easybeach.promotions.repository.PromocionAlcanceRepository;
import com.easybeach.promotions.repository.PromocionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación real de {@code PromocionesPublicasProvider} (etapa 14). Al
 * existir este bean concreto, {@code SinPromocionesPublicasProvider}
 * (registrado por {@code @ConditionalOnMissingBean} en {@code catalog})
 * desaparece solo - sin tocar una línea de {@code catalog}.
 */
@Component
public class PromocionesPublicasProviderImpl implements PromocionesPublicasProvider {

    private final PromocionRepository promocionRepository;
    private final PromocionAlcanceRepository alcanceRepository;
    private final VigenciaPromocionChecker vigenciaChecker;

    public PromocionesPublicasProviderImpl(PromocionRepository promocionRepository,
                                            PromocionAlcanceRepository alcanceRepository,
                                            VigenciaPromocionChecker vigenciaChecker) {
        this.promocionRepository = promocionRepository;
        this.alcanceRepository = alcanceRepository;
        this.vigenciaChecker = vigenciaChecker;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<PromocionResumen>> resolverPorProducto(Long balnearioId,
                                                                   List<ProductoParaPromo> productos) {
        if (productos.isEmpty()) {
            return Map.of();
        }
        List<Promocion> vigentes = vigenciasDePorcentuales(balnearioId);
        if (vigentes.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = vigentes.stream().map(Promocion::getId).toList();
        List<PromocionAlcance> alcances = alcanceRepository.findByPromocionIdIn(ids);

        Map<Long, List<Long>> promosPorProducto = new HashMap<>();
        Map<Long, List<Long>> promosPorCategoria = new HashMap<>();
        for (PromocionAlcance alcance : alcances) {
            if (alcance.getTipoAlcance() == TipoAlcance.PRODUCTO) {
                promosPorProducto.computeIfAbsent(alcance.getReferenciaId(), k -> new ArrayList<>())
                        .add(alcance.getPromocionId());
            } else {
                promosPorCategoria.computeIfAbsent(alcance.getReferenciaId(), k -> new ArrayList<>())
                        .add(alcance.getPromocionId());
            }
        }
        Map<Long, Promocion> promocionPorId = vigentes.stream()
                .collect(Collectors.toMap(Promocion::getId, p -> p));

        Map<Long, List<PromocionResumen>> resultado = new HashMap<>();
        for (ProductoParaPromo producto : productos) {
            List<Long> promoIds = new ArrayList<>();
            promoIds.addAll(promosPorProducto.getOrDefault(producto.productoId(), List.of()));
            promoIds.addAll(promosPorCategoria.getOrDefault(producto.categoriaId(), List.of()));
            if (promoIds.isEmpty()) {
                continue;
            }
            List<PromocionResumen> resumenes = promoIds.stream().distinct()
                    .map(promocionPorId::get)
                    .filter(java.util.Objects::nonNull)
                    .map(this::toResumen)
                    .toList();
            if (!resumenes.isEmpty()) {
                resultado.put(producto.productoId(), resumenes);
            }
        }
        // productosConAlcanceDirecto queda sin uso directo fuera del cálculo de
        // arriba - se deja de lado; el filtro real ya está en promosPorProducto.
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromocionResumen> listarVigentes(Long balnearioId) {
        return promocionRepository.findByBalnearioIdAndEstado(balnearioId, EstadoPromocion.ACTIVA).stream()
                .filter(vigenciaChecker::estaVigenteAhora)
                .map(this::toResumen)
                .toList();
    }

    private List<Promocion> vigenciasDePorcentuales(Long balnearioId) {
        return promocionRepository.findByBalnearioIdAndEstado(balnearioId, EstadoPromocion.ACTIVA).stream()
                .filter(p -> p.getTipo() == TipoPromocion.DESCUENTO_PORCENTUAL || p.getTipo() == TipoPromocion.HAPPY_HOUR)
                .filter(vigenciaChecker::estaVigenteAhora)
                .toList();
    }

    private PromocionResumen toResumen(Promocion promocion) {
        return new PromocionResumen(promocion.getId(), promocion.getNombre(), promocion.getTipo().name(),
                promocion.getValor());
    }
}
