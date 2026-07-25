package com.easybeach.branding.storage;

import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Etapa 05 §6 / amenaza #11: valida tipo real (magic bytes, no extensión),
 * tamaño máximo, renombra y guarda fuera del web root.
 */
@Service
public class AssetStorageService {

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final Pattern SVG_SCRIPT_TAG = Pattern.compile("<\\s*script", Pattern.CASE_INSENSITIVE);

    private final AssetStorageProperties properties;

    public AssetStorageService(AssetStorageProperties properties) {
        this.properties = properties;
    }

    public record StoredAsset(String publicUrl) {
    }

    public StoredAsset store(Long balnearioId, AssetType type, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA, "El archivo está vacío");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA,
                    "El archivo supera el tamaño máximo permitido (" + properties.getMaxSizeBytes() / 1024 / 1024 + " MB)");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String extension = detectRealFormat(bytes);

        String fileName = UUID.randomUUID() + "." + extension;
        Path dir = Path.of(properties.getRootDir(), "balnearios", String.valueOf(balnearioId), "branding");
        Path target = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String publicUrl = "/public/assets/balnearios/" + balnearioId + "/branding/" + fileName;
        return new StoredAsset(publicUrl);
    }

    /** @return la extensión real detectada (png/jpg/svg); nunca confía en el nombre/extensión declarados por el cliente. */
    private String detectRealFormat(byte[] bytes) {
        if (startsWith(bytes, PNG_MAGIC)) {
            return "png";
        }
        if (startsWith(bytes, JPEG_MAGIC)) {
            return "jpg";
        }
        if (looksLikeSafeSvg(bytes)) {
            return "svg";
        }
        throw new ApiException(ErrorCode.VALIDACION_FALLIDA,
                "Formato de imagen no soportado (solo PNG, JPEG o SVG)");
    }

    private boolean looksLikeSafeSvg(byte[] bytes) {
        String head = new String(bytes, 0, Math.min(bytes.length, 512), StandardCharsets.UTF_8).trim();
        boolean pareceSvg = head.regionMatches(true, 0, "<?xml", 0, 5) || head.regionMatches(true, 0, "<svg", 0, 4);
        if (!pareceSvg) {
            return false;
        }
        String full = new String(bytes, StandardCharsets.UTF_8);
        return !SVG_SCRIPT_TAG.matcher(full).find();
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
