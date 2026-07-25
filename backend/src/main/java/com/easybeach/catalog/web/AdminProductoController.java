package com.easybeach.catalog.web;

import com.easybeach.catalog.service.ProductoService;
import com.easybeach.catalog.web.dto.DisponibilidadRequest;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
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

@RestController
@RequestMapping("/api/v1/admin/productos")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminProductoController {

    private final ProductoService service;

    public AdminProductoController(ProductoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request,
                                   @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.crear(principal.balnearioId(), request);
    }

    @GetMapping
    public List<ProductoResponse> listar(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.listar(principal.balnearioId());
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request,
                                        @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.actualizar(principal.balnearioId(), id, request);
    }

    /** "Se terminó el hielo → el producto desaparece del menú al instante" (etapa 11). */
    @PutMapping("/{id}/disponibilidad")
    public ProductoResponse cambiarDisponibilidad(@PathVariable Long id, @RequestBody DisponibilidadRequest request,
                                                   @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.cambiarDisponibilidad(principal.balnearioId(), id, request.disponible());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        service.eliminar(principal.balnearioId(), id);
    }
}
