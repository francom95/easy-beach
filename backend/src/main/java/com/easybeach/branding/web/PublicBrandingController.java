package com.easybeach.branding.web;

import com.easybeach.branding.service.ConfiguracionVisualService;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Theme white-label completo, público (etapa 10 criterio de aceptación:
 * "devuelve todos los tokens del contrato de la etapa 06, no un
 * subconjunto"). La app mobile lo consume ANTES de que el cliente esté
 * autenticado - transición de marca al elegir balneario (etapa 06/16).
 */
@RestController
@RequestMapping("/api/v1/balnearios/{slug}/branding")
public class PublicBrandingController {

    private final ConfiguracionVisualService configuracionVisualService;
    private final BalnearioRepository balnearioRepository;

    public PublicBrandingController(ConfiguracionVisualService configuracionVisualService,
                                     BalnearioRepository balnearioRepository) {
        this.configuracionVisualService = configuracionVisualService;
        this.balnearioRepository = balnearioRepository;
    }

    @GetMapping
    public Map<String, Object> obtener(@PathVariable String slug) {
        Long balnearioId = balnearioRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO))
                .getId();
        return configuracionVisualService.obtenerPublico(balnearioId);
    }
}
