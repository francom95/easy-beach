package com.easybeach.catalog.web.dto;

import java.util.List;

public record MenuCategoriaResponse(Long id, String nombre, List<MenuProductoResponse> productos) {
}
