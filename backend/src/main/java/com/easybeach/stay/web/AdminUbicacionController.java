package com.easybeach.stay.web;

import com.easybeach.shared.security.EasyBeachUserPrincipal;
import com.easybeach.stay.service.UbicacionService;
import com.easybeach.stay.web.dto.CambiarEstadoUbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
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

/** ABM de ubicaciones del propio balneario del admin logueado (etapa 11). */
@RestController
@RequestMapping("/api/v1/admin/ubicaciones")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminUbicacionController {

    private final UbicacionService service;

    public AdminUbicacionController(UbicacionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UbicacionResponse crear(@Valid @RequestBody UbicacionRequest request,
                                    @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.crear(principal.balnearioId(), request);
    }

    @GetMapping
    public List<UbicacionResponse> listar(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.listar(principal.balnearioId());
    }

    @PutMapping("/{id}")
    public UbicacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody UbicacionRequest request,
                                         @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.actualizar(principal.balnearioId(), id, request);
    }

    @PutMapping("/{id}/estado")
    public UbicacionResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoUbicacionRequest request,
                                            @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.cambiarEstado(principal.balnearioId(), id, request.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        service.eliminar(principal.balnearioId(), id);
    }
}
