package com.easybeach.catalog.web;

import com.easybeach.catalog.service.PromocionesPublicasProvider;
import com.easybeach.catalog.service.PromocionesPublicasProvider.PromocionResumen;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sección de promociones del cliente (etapa 07), incluidos los combos - que
 * el menú no embebe por producto. Vive en {@code catalog} porque este
 * módulo ya resuelve balneario por slug (igual que el menú); la
 * implementación real de {@link PromocionesPublicasProvider} la aporta
 * {@code promotions}.
 */
@RestController
@RequestMapping("/api/v1/balnearios/{slug}/promociones")
public class PublicPromocionesController {

    private final PromocionesPublicasProvider promocionesPublicasProvider;
    private final BalnearioRepository balnearioRepository;

    public PublicPromocionesController(PromocionesPublicasProvider promocionesPublicasProvider,
                                        BalnearioRepository balnearioRepository) {
        this.promocionesPublicasProvider = promocionesPublicasProvider;
        this.balnearioRepository = balnearioRepository;
    }

    @GetMapping
    public List<PromocionResumen> listar(@PathVariable String slug) {
        Long balnearioId = balnearioRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO))
                .getId();
        return promocionesPublicasProvider.listarVigentes(balnearioId);
    }
}
