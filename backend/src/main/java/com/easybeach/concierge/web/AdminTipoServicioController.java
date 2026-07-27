package com.easybeach.concierge.web;

import com.easybeach.concierge.service.TipoServicioService;
import com.easybeach.concierge.web.dto.TipoServicioRequest;
import com.easybeach.concierge.web.dto.TipoServicioResponse;
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

/** ABM del catálogo de servicios al carpero (etapa 14). */
@RestController
@RequestMapping("/api/v1/admin/tipos-servicio")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminTipoServicioController {

    private final TipoServicioService service;

    public AdminTipoServicioController(TipoServicioService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TipoServicioResponse crear(@Valid @RequestBody TipoServicioRequest request,
                                       @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.crear(principal.balnearioId(), request);
    }

    @PutMapping("/{id}")
    public TipoServicioResponse actualizar(@PathVariable Long id, @Valid @RequestBody TipoServicioRequest request,
                                            @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.actualizar(principal.balnearioId(), id, request);
    }

    @GetMapping
    public List<TipoServicioResponse> listar(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.listar(principal.balnearioId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        service.eliminar(principal.balnearioId(), id);
    }
}
