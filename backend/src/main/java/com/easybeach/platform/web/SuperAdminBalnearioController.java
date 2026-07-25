package com.easybeach.platform.web;

import com.easybeach.identity.web.CurrentUserResolver;
import com.easybeach.platform.service.BalnearioService;
import com.easybeach.platform.web.dto.ActualizarBalnearioRequest;
import com.easybeach.platform.web.dto.BalnearioResponse;
import com.easybeach.platform.web.dto.CrearBalnearioRequest;
import com.easybeach.platform.web.dto.CrearBalnearioResponse;
import com.easybeach.platform.web.dto.MotivoRequest;
import com.easybeach.shared.pagination.PageResponse;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ABM de balnearios (etapa 10). Cross-tenant por diseño: solo Super Admin. */
@RestController
@RequestMapping("/api/v1/super-admin/balnearios")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminBalnearioController {

    private final BalnearioService balnearioService;
    private final CurrentUserResolver currentUserResolver;

    public SuperAdminBalnearioController(BalnearioService balnearioService, CurrentUserResolver currentUserResolver) {
        this.balnearioService = balnearioService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrearBalnearioResponse crear(@Valid @RequestBody CrearBalnearioRequest request,
                                         @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return balnearioService.crear(currentUserResolver.resolveId(principal), request);
    }

    @PutMapping("/{id}")
    public BalnearioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarBalnearioRequest request) {
        return balnearioService.actualizar(id, request);
    }

    @GetMapping
    public PageResponse<BalnearioResponse> listar(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(balnearioService.listar(PageRequest.of(page, PageResponse.clampSize(size))));
    }

    @GetMapping("/{id}")
    public BalnearioResponse obtener(@PathVariable Long id) {
        return balnearioService.obtener(id);
    }

    @PostMapping("/{id}/activar")
    public BalnearioResponse activar(@PathVariable Long id, @Valid @RequestBody MotivoRequest request,
                                      @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return balnearioService.activar(currentUserResolver.resolveId(principal), id, request.motivo());
    }

    @PostMapping("/{id}/suspender")
    public BalnearioResponse suspender(@PathVariable Long id, @Valid @RequestBody MotivoRequest request,
                                        @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return balnearioService.suspender(currentUserResolver.resolveId(principal), id, request.motivo());
    }
}
