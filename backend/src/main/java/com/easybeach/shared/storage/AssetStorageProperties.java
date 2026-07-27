package com.easybeach.shared.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Etapa 05 §6: archivos subidos se validan (magic bytes + tamaño), se
 * renombran y se guardan FUERA del web root, servidos por URL pública sin
 * ejecución (ver {@link PublicAssetController}, que solo lee bytes de disco,
 * nunca interpreta el contenido).
 *
 * <p>En {@code shared} desde la etapa 17 (movido de {@code branding}): la
 * foto de producto (etapa 17, {@code catalog}) necesita el mismo storage y
 * {@code catalog -> branding} no es una dependencia permitida (ADR-002) -
 * el storage de archivos es infraestructura cruzada, no un concepto propio
 * de theming.
 */
@ConfigurationProperties(prefix = "easybeach.storage")
public class AssetStorageProperties {

    /** Directorio raíz en disco, fuera de src/main/resources (nunca en el classpath/webroot). */
    private String rootDir = "./data/assets";

    private long maxSizeBytes = 5 * 1024 * 1024; // 5 MB

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }
}
