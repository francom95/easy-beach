package com.easybeach.platform.web;

import com.easybeach.platform.service.BalnearioService;
import com.easybeach.platform.web.dto.BalnearioResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sin autenticación: selector de balneario de la app cliente. */
@RestController
@RequestMapping("/api/v1/balnearios")
public class PublicBalnearioController {

    private final BalnearioService balnearioService;

    public PublicBalnearioController(BalnearioService balnearioService) {
        this.balnearioService = balnearioService;
    }

    @GetMapping
    public List<BalnearioResponse> listar() {
        return balnearioService.listarPublico();
    }

    @GetMapping("/{slug}")
    public BalnearioResponse obtener(@PathVariable String slug) {
        return balnearioService.obtenerPublicoPorSlug(slug);
    }
}
