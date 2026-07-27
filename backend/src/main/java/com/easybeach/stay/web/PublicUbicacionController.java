package com.easybeach.stay.web;

import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.stay.service.UbicacionService;
import com.easybeach.stay.web.dto.UbicacionResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ubicaciones disponibles para que el cliente elija al abrir su estadía
 * (etapa 16, S05 "Elegir ubicación"). Sin autenticación, igual que el menú
 * público (etapa 11): el balneario se resuelve por slug.
 */
@RestController
@RequestMapping("/api/v1/balnearios/{slug}/ubicaciones")
public class PublicUbicacionController {

    private final UbicacionService ubicacionService;
    private final BalnearioRepository balnearioRepository;

    public PublicUbicacionController(UbicacionService ubicacionService, BalnearioRepository balnearioRepository) {
        this.ubicacionService = ubicacionService;
        this.balnearioRepository = balnearioRepository;
    }

    @GetMapping
    public List<UbicacionResponse> listar(@PathVariable String slug) {
        Long balnearioId = balnearioRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO))
                .getId();
        return ubicacionService.listarActivas(balnearioId);
    }
}
