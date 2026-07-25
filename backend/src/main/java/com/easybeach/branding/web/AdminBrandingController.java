package com.easybeach.branding.web;

import com.easybeach.branding.service.ConfiguracionVisualService;
import com.easybeach.branding.storage.AssetType;
import com.easybeach.branding.web.dto.BrandingUpdateRequest;
import com.easybeach.branding.web.dto.BrandingUpdateResult;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Configuración visual (theme white-label) del propio balneario del admin logueado. */
@RestController
@RequestMapping("/api/v1/admin/branding")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminBrandingController {

    private final ConfiguracionVisualService service;

    public AdminBrandingController(ConfiguracionVisualService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> obtener() {
        return service.obtenerDelBalnearioActual();
    }

    @PutMapping
    public ResponseEntity<BrandingUpdateResult> actualizar(@Valid @RequestBody BrandingUpdateRequest request) {
        BrandingUpdateResult resultado = service.actualizar(request);
        // 200 si se aplicó; 409 si hace falta aceptar la sugerencia de contraste o corregir.
        return resultado.aplicado()
                ? ResponseEntity.ok(resultado)
                : ResponseEntity.status(HttpStatus.CONFLICT).body(resultado);
    }

    @PostMapping(value = "/assets/{tipo}", consumes = "multipart/form-data")
    public Map<String, Object> actualizarAsset(@PathVariable AssetType tipo, @RequestParam("file") MultipartFile file) {
        return service.actualizarAsset(tipo, file);
    }
}
