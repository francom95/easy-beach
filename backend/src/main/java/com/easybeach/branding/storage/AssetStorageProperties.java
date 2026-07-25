package com.easybeach.branding.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Etapa 05 §6: archivos subidos se validan (magic bytes + tamaño), se
 * renombran y se guardan FUERA del web root, servidos por URL pública sin
 * ejecución (ver {@link PublicAssetController}, que solo lee bytes de disco,
 * nunca interpreta el contenido).
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
