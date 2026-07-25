package com.easybeach.platform.web;

import com.easybeach.platform.service.TemporadaService;
import com.easybeach.platform.web.dto.CambiarEstadoTemporadaRequest;
import com.easybeach.platform.web.dto.TemporadaRequest;
import com.easybeach.platform.web.dto.TemporadaResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin/temporadas")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminTemporadaController {

    private final TemporadaService temporadaService;

    public SuperAdminTemporadaController(TemporadaService temporadaService) {
        this.temporadaService = temporadaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemporadaResponse crear(@Valid @RequestBody TemporadaRequest request) {
        return temporadaService.crear(request);
    }

    @GetMapping
    public List<TemporadaResponse> listar() {
        return temporadaService.listar();
    }

    @PutMapping("/{id}/estado")
    public TemporadaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoTemporadaRequest request) {
        return temporadaService.cambiarEstado(id, request.estado());
    }
}
