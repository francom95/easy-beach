package com.easybeach.catalog.service;

import java.util.List;
import java.util.Map;

/** Vigente hasta que {@code promotions} publique la real (etapa 14). */
public class SinPromocionesPublicasProvider implements PromocionesPublicasProvider {

    @Override
    public Map<Long, List<PromocionResumen>> resolverPorProducto(Long balnearioId,
                                                                   List<ProductoParaPromo> productos) {
        return Map.of();
    }

    @Override
    public List<PromocionResumen> listarVigentes(Long balnearioId) {
        return List.of();
    }
}
