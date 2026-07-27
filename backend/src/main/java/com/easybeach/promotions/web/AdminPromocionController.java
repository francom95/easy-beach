package com.easybeach.promotions.web;

import com.easybeach.promotions.service.PromocionService;
import com.easybeach.promotions.web.dto.CambiarEstadoPromocionRequest;
import com.easybeach.promotions.web.dto.PromocionRequest;
import com.easybeach.promotions.web.dto.PromocionResponse;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ABM de promociones del admin de balneario (etapa 14). */
@RestController
@RequestMapping("/api/v1/admin/promociones")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminPromocionController {

    private final PromocionService service;

    public AdminPromocionController(PromocionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromocionResponse crear(@Valid @RequestBody PromocionRequest request,
                                    @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.crear(principal.balnearioId(), request);
    }

    @PutMapping("/{id}")
    public PromocionResponse actualizar(@PathVariable Long id, @Valid @RequestBody PromocionRequest request,
                                         @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.actualizar(principal.balnearioId(), id, request);
    }

    @GetMapping
    public List<PromocionResponse> listar(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.listar(principal.balnearioId());
    }

    @PutMapping("/{id}/estado")
    public PromocionResponse cambiarEstado(@PathVariable Long id,
                                            @Valid @RequestBody CambiarEstadoPromocionRequest request,
                                            @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.cambiarEstado(principal.balnearioId(), id, request.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        service.eliminar(principal.balnearioId(), id);
    }
}
