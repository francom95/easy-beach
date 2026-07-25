package com.easybeach.catalog.web;

import com.easybeach.catalog.service.ProductoVarianteService;
import com.easybeach.catalog.web.dto.ProductoVarianteRequest;
import com.easybeach.catalog.web.dto.ProductoVarianteResponse;
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
@RequestMapping("/api/v1/admin/productos/{productoId}/variantes")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminProductoVarianteController {

    private final ProductoVarianteService service;

    public AdminProductoVarianteController(ProductoVarianteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoVarianteResponse crear(@PathVariable Long productoId, @Valid @RequestBody ProductoVarianteRequest request,
                                           @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.crear(principal.balnearioId(), productoId, request);
    }

    @GetMapping
    public List<ProductoVarianteResponse> listar(@PathVariable Long productoId,
                                                  @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.listar(principal.balnearioId(), productoId);
    }

    @PutMapping("/{id}")
    public ProductoVarianteResponse actualizar(@PathVariable Long productoId, @PathVariable Long id,
                                                @Valid @RequestBody ProductoVarianteRequest request,
                                                @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.actualizar(principal.balnearioId(), productoId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long productoId, @PathVariable Long id,
                          @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        service.eliminar(principal.balnearioId(), productoId, id);
    }
}
