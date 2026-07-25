package com.easybeach.shared.pagination;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envoltorio de paginación offset (etapa 04 §1.5): page (0-based) / size
 * (máx. 100) / totalElements / totalPages.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static final int MAX_SIZE = 100;

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public static int clampSize(int requested) {
        if (requested <= 0) {
            return 20;
        }
        return Math.min(requested, MAX_SIZE);
    }
}
