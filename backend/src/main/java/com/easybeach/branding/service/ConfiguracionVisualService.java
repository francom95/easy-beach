package com.easybeach.branding.service;

import com.easybeach.branding.domain.ConfiguracionVisual;
import com.easybeach.branding.repository.ConfiguracionVisualRepository;
import com.easybeach.branding.storage.AssetType;
import com.easybeach.branding.theming.ThemeTokenAssembler;
import com.easybeach.branding.theming.ThemeTokenKeys;
import com.easybeach.branding.web.dto.BrandingUpdateRequest;
import com.easybeach.branding.web.dto.BrandingUpdateResult;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.storage.AssetStorageService;
import com.easybeach.shared.tenancy.TenantFilterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lee/actualiza el theme white-label completo de un balneario. La API
 * (pública o de admin) siempre sirve el JSON resuelto guardado en
 * {@link ConfiguracionVisual#getTokens()} - nunca recalcula on the fly.
 */
@Service
public class ConfiguracionVisualService {

    private final ConfiguracionVisualRepository repository;
    private final ThemeTokenAssembler assembler;
    private final AssetStorageService assetStorageService;
    private final TenantFilterService tenantFilterService;
    private final ObjectMapper objectMapper;

    public ConfiguracionVisualService(ConfiguracionVisualRepository repository,
                                       ThemeTokenAssembler assembler,
                                       AssetStorageService assetStorageService,
                                       TenantFilterService tenantFilterService,
                                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.assembler = assembler;
        this.assetStorageService = assetStorageService;
        this.tenantFilterService = tenantFilterService;
        this.objectMapper = objectMapper;
    }

    /** Alta de balneario (etapa 10): siembra el theme default de EasyBeach. Sin TenantContext - lo llama Super Admin. */
    @Transactional
    public void sembrarDefault(Long balnearioId) {
        ConfiguracionVisual entidad = new ConfiguracionVisual();
        entidad.setBalnearioId(balnearioId);
        entidad.setTokens(toJson(assembler.defaults()));
        repository.save(entidad);
    }

    /** Público: theme completo por balneario, sin autenticación. */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerPublico(Long balnearioId) {
        ConfiguracionVisual entidad = repository.findByBalnearioId(balnearioId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        return fromJson(entidad.getTokens());
    }

    /** Admin: theme de SU balneario (tenant resuelto por JWT). */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDelBalnearioActual() {
        tenantFilterService.applyCurrentTenant();
        return obtenerPublico(currentTenantId());
    }

    @Transactional
    public BrandingUpdateResult actualizar(BrandingUpdateRequest request) {
        tenantFilterService.applyCurrentTenant();
        Long balnearioId = currentTenantId();
        ConfiguracionVisual entidad = repository.findByBalnearioId(balnearioId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));

        ThemeTokenAssembler.AssetUrls assetsActuales = new ThemeTokenAssembler.AssetUrls(
                assetUrlOrDefault(entidad, ThemeTokenKeys.ASSET_LOGO),
                assetUrlOrDefault(entidad, ThemeTokenKeys.ASSET_LOGO_COMPACT),
                assetUrlOrDefault(entidad, ThemeTokenKeys.ASSET_COVER),
                assetUrlOrDefault(entidad, ThemeTokenKeys.ASSET_SPLASH),
                assetUrlOrDefault(entidad, ThemeTokenKeys.ASSET_PRODUCT_PLACEHOLDER));

        ThemeTokenAssembler.Resultado resultado = assembler.assemble(request, assetsActuales);
        if (!resultado.cumple()) {
            return new BrandingUpdateResult(false, null, resultado.ajustesPropuestos());
        }
        entidad.setTokens(toJson(resultado.tokens()));
        repository.save(entidad);
        return new BrandingUpdateResult(true, resultado.tokens(), Map.of());
    }

    @Transactional
    public Map<String, Object> actualizarAsset(AssetType tipo, MultipartFile file) {
        tenantFilterService.applyCurrentTenant();
        Long balnearioId = currentTenantId();
        ConfiguracionVisual entidad = repository.findByBalnearioId(balnearioId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));

        AssetStorageService.StoredAsset stored = assetStorageService.storeBranding(balnearioId, file);
        Map<String, Object> tokens = fromJson(entidad.getTokens());
        tokens.put(tipo.tokenKey(), stored.publicUrl());
        entidad.setTokens(toJson(tokens));
        repository.save(entidad);
        return tokens;
    }

    private String assetUrlOrDefault(ConfiguracionVisual entidad, String tokenKey) {
        Object valor = fromJson(entidad.getTokens()).get(tokenKey);
        return valor == null ? null : valor.toString();
    }

    private Long currentTenantId() {
        return com.easybeach.shared.tenancy.TenantContext.get();
    }

    private String toJson(Map<String, Object> tokens) {
        try {
            return objectMapper.writeValueAsString(tokens);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el theme", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Theme guardado con JSON inválido", e);
        }
    }
}
