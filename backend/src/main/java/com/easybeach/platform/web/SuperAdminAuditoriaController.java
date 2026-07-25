package com.easybeach.platform.web;

import com.easybeach.platform.service.AuditoriaPlataformaService;
import com.easybeach.platform.web.dto.AuditoriaResponse;
import com.easybeach.shared.pagination.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin/auditoria")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminAuditoriaController {

    private final AuditoriaPlataformaService auditoriaService;

    public SuperAdminAuditoriaController(AuditoriaPlataformaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public PageResponse<AuditoriaResponse> listar(@RequestParam(required = false) Long balnearioId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        var resultado = auditoriaService.listar(balnearioId, PageRequest.of(page, PageResponse.clampSize(size)))
                .map(a -> new AuditoriaResponse(a.getId(), a.getActorUsuarioId(), a.getAccion(), a.getEntidadTipo(),
                        a.getEntidadId(), a.getBalnearioId(), a.getCreatedAt()));
        return PageResponse.from(resultado);
    }
}
