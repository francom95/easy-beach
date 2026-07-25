package com.easybeach.catalog.web;

import com.easybeach.catalog.service.MenuPublicoService;
import com.easybeach.catalog.web.dto.MenuCategoriaResponse;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El endpoint más consultado de la plataforma (etapa 11). Público, sin
 * auth. {@code Cache-Control} corto (evita pegarle a la DB en cada apertura
 * de la app) + {@code ETag} (via {@code ShallowEtagHeaderFilter}, registrado
 * solo en esta ruta - ver {@code MenuCacheConfig}) para que un cliente con
 * el mismo menú reciba {@code 304} sin recalcular nada del lado del cliente.
 */
@RestController
@RequestMapping("/api/v1/balnearios/{slug}/menu")
public class PublicMenuController {

    private final MenuPublicoService menuPublicoService;
    private final BalnearioRepository balnearioRepository;

    public PublicMenuController(MenuPublicoService menuPublicoService, BalnearioRepository balnearioRepository) {
        this.menuPublicoService = menuPublicoService;
        this.balnearioRepository = balnearioRepository;
    }

    @GetMapping
    public ResponseEntity<List<MenuCategoriaResponse>> obtenerMenu(@PathVariable String slug) {
        Long balnearioId = balnearioRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO))
                .getId();
        List<MenuCategoriaResponse> menu = menuPublicoService.obtenerMenu(balnearioId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(15)).cachePublic())
                .body(menu);
    }
}
