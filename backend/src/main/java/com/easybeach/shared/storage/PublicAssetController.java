package com.easybeach.shared.storage;

import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sirve los assets subidos: SOLO lectura de bytes de disco, jamás interpreta
 * ni ejecuta el contenido (etapa 05 §6: "servir por URL... sin ejecución").
 * Público - la app cliente lo consume sin autenticarse. En {@code shared}
 * desde la etapa 17 (movido de {@code branding}, ver Javadoc de
 * {@link AssetStorageProperties}).
 */
@RestController
public class PublicAssetController {

    private final AssetStorageProperties properties;

    public PublicAssetController(AssetStorageProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/public/assets/balnearios/{balnearioId}/branding/{fileName}")
    public ResponseEntity<ByteArrayResource> getBranding(@PathVariable Long balnearioId, @PathVariable String fileName) {
        return servir(balnearioId, "branding", fileName);
    }

    /** Fotos de producto (etapa 17), mismo storage que branding en su propio subdirectorio. */
    @GetMapping("/public/assets/balnearios/{balnearioId}/productos/{fileName}")
    public ResponseEntity<ByteArrayResource> getProducto(@PathVariable Long balnearioId, @PathVariable String fileName) {
        return servir(balnearioId, "productos", fileName);
    }

    private ResponseEntity<ByteArrayResource> servir(Long balnearioId, String subfolder, String fileName) {
        // fileName siempre es un UUID generado por el servidor (AssetStorageService);
        // igual se rechaza cualquier intento de path traversal por las dudas.
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        Path path = Path.of(properties.getRootDir(), "balnearios", String.valueOf(balnearioId), subfolder, fileName);
        if (!Files.exists(path)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        MediaType contentType = contentTypeFor(fileName);
        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                .body(new ByteArrayResource(bytes));
    }

    private MediaType contentTypeFor(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".svg")) {
            return MediaType.valueOf("image/svg+xml");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
