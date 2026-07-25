package com.easybeach.branding.theming;

import com.easybeach.branding.web.dto.BrandingUpdateRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Valida contraste, deriva {@code on-*}/{@code muted}/{@code border} y arma
 * el JSON completo de tokens (etapa 06 / docs/design/tokens.md). Ningún
 * color "on-*" llega desde el request - siempre se calcula acá.
 */
@Component
public class ThemeTokenAssembler {

    private static final double CONTRASTE_MINIMO = 4.5;
    private static final double CONTRASTE_MINIMO_UI = 3.0;

    public record Resultado(boolean cumple, Map<String, String> ajustesPropuestos, Map<String, Object> tokens) {
    }

    /**
     * @param assets URLs actuales de assets (logo/portada/splash/etc.), se preservan tal cual - esta operación no las toca.
     */
    public Resultado assemble(BrandingUpdateRequest request, AssetUrls assets) {
        Map<String, String> ajustes = new LinkedHashMap<>();
        String primary = ajustarSiHaceFalta(request.colorPrimary(), ajustes, ThemeTokenKeys.COLOR_PRIMARY);
        String secondary = ajustarSiHaceFalta(request.colorSecondary(), ajustes, ThemeTokenKeys.COLOR_SECONDARY);
        String success = ajustarSiHaceFalta(request.colorSuccess(), ajustes, ThemeTokenKeys.COLOR_SUCCESS);
        String warning = ajustarSiHaceFalta(request.colorWarning(), ajustes, ThemeTokenKeys.COLOR_WARNING);
        String error = ajustarSiHaceFalta(request.colorError(), ajustes, ThemeTokenKeys.COLOR_ERROR);
        String info = ajustarSiHaceFalta(request.colorInfo(), ajustes, ThemeTokenKeys.COLOR_INFO);
        // background/surface son fondos de página/card: se validan contra su propio
        // "on-*" con el mismo criterio, pero nunca contra blanco/tinta ajenos entre sí.
        String background = ajustarSiHaceFalta(request.colorBackground(), ajustes, ThemeTokenKeys.COLOR_BACKGROUND);
        String surface = ajustarSiHaceFalta(request.colorSurface(), ajustes, ThemeTokenKeys.COLOR_SURFACE);

        if (!ajustes.isEmpty() && !request.aceptarSugerencia()) {
            return new Resultado(false, ajustes, null);
        }
        if (!ajustes.isEmpty()) {
            // aceptarSugerencia=true: se guarda con los tonos ajustados, no con los originales.
            primary = ajustes.getOrDefault(ThemeTokenKeys.COLOR_PRIMARY, primary);
            secondary = ajustes.getOrDefault(ThemeTokenKeys.COLOR_SECONDARY, secondary);
            success = ajustes.getOrDefault(ThemeTokenKeys.COLOR_SUCCESS, success);
            warning = ajustes.getOrDefault(ThemeTokenKeys.COLOR_WARNING, warning);
            error = ajustes.getOrDefault(ThemeTokenKeys.COLOR_ERROR, error);
            info = ajustes.getOrDefault(ThemeTokenKeys.COLOR_INFO, info);
            background = ajustes.getOrDefault(ThemeTokenKeys.COLOR_BACKGROUND, background);
            surface = ajustes.getOrDefault(ThemeTokenKeys.COLOR_SURFACE, surface);
        }

        String onPrimary = ColorMath.pickOnColor(primary, ThemeDefaults.TINTA_OSCURA);
        String onSecondary = ColorMath.pickOnColor(secondary, ThemeDefaults.TINTA_OSCURA);
        String onBackground = ColorMath.pickOnColor(background, ThemeDefaults.TINTA_OSCURA);
        String onSurface = ColorMath.pickOnColor(surface, ThemeDefaults.TINTA_OSCURA);
        String onSuccess = ColorMath.pickOnColor(success, ThemeDefaults.TINTA_OSCURA);
        String onWarning = ColorMath.pickOnColor(warning, ThemeDefaults.TINTA_OSCURA);
        String onError = ColorMath.pickOnColor(error, ThemeDefaults.TINTA_OSCURA);
        String onInfo = ColorMath.pickOnColor(info, ThemeDefaults.TINTA_OSCURA);
        String onSurfaceMuted = ColorMath.deriveMuted(onSurface, surface, CONTRASTE_MINIMO_UI);
        String border = ColorMath.deriveBorder(surface, background);

        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put(ThemeTokenKeys.CONTRACT, ThemeDefaults.CONTRACT_VERSION);
        tokens.put(ThemeTokenKeys.NAME, request.themeName());
        tokens.put(ThemeTokenKeys.COLOR_PRIMARY, primary);
        tokens.put(ThemeTokenKeys.COLOR_ON_PRIMARY, onPrimary);
        tokens.put(ThemeTokenKeys.COLOR_SECONDARY, secondary);
        tokens.put(ThemeTokenKeys.COLOR_ON_SECONDARY, onSecondary);
        tokens.put(ThemeTokenKeys.COLOR_BACKGROUND, background);
        tokens.put(ThemeTokenKeys.COLOR_ON_BACKGROUND, onBackground);
        tokens.put(ThemeTokenKeys.COLOR_SURFACE, surface);
        tokens.put(ThemeTokenKeys.COLOR_ON_SURFACE, onSurface);
        tokens.put(ThemeTokenKeys.COLOR_ON_SURFACE_MUTED, onSurfaceMuted);
        tokens.put(ThemeTokenKeys.COLOR_BORDER, border);
        tokens.put(ThemeTokenKeys.COLOR_SUCCESS, success);
        tokens.put(ThemeTokenKeys.COLOR_ON_SUCCESS, onSuccess);
        tokens.put(ThemeTokenKeys.COLOR_WARNING, warning);
        tokens.put(ThemeTokenKeys.COLOR_ON_WARNING, onWarning);
        tokens.put(ThemeTokenKeys.COLOR_ERROR, error);
        tokens.put(ThemeTokenKeys.COLOR_ON_ERROR, onError);
        tokens.put(ThemeTokenKeys.COLOR_INFO, info);
        tokens.put(ThemeTokenKeys.COLOR_ON_INFO, onInfo);
        tokens.put(ThemeTokenKeys.TYPOGRAPHY_FAMILY,
                (request.typographyFamily() == null ? ThemeDefaults.TYPOGRAPHY_FAMILY : request.typographyFamily()).toJson());
        tokens.put(ThemeTokenKeys.TYPOGRAPHY_SCALE, typographyScale());
        tokens.put(ThemeTokenKeys.ASSET_LOGO, assets.logo());
        tokens.put(ThemeTokenKeys.ASSET_LOGO_COMPACT, assets.logoCompact());
        tokens.put(ThemeTokenKeys.ASSET_COVER, assets.cover());
        tokens.put(ThemeTokenKeys.ASSET_SPLASH, assets.splash());
        tokens.put(ThemeTokenKeys.ASSET_PRODUCT_PLACEHOLDER, assets.productPlaceholder());

        return new Resultado(true, ajustes.isEmpty() ? Map.of() : ajustes, tokens);
    }

    /** Theme completo con los defaults de EasyBeach - semilla de un balneario nuevo. */
    public Map<String, Object> defaults() {
        BrandingUpdateRequest request = new BrandingUpdateRequest(
                ThemeDefaults.NAME, ThemeDefaults.COLOR_PRIMARY, ThemeDefaults.COLOR_SECONDARY,
                ThemeDefaults.COLOR_BACKGROUND, ThemeDefaults.COLOR_SURFACE, ThemeDefaults.COLOR_SUCCESS,
                ThemeDefaults.COLOR_WARNING, ThemeDefaults.COLOR_ERROR, ThemeDefaults.COLOR_INFO,
                ThemeDefaults.TYPOGRAPHY_FAMILY, true);
        AssetUrls assets = new AssetUrls(ThemeDefaults.ASSET_LOGO, ThemeDefaults.ASSET_LOGO_COMPACT,
                ThemeDefaults.ASSET_COVER, ThemeDefaults.ASSET_SPLASH, ThemeDefaults.ASSET_PRODUCT_PLACEHOLDER);
        return assemble(request, assets).tokens();
    }

    private String ajustarSiHaceFalta(String colorHex, Map<String, String> ajustes, String tokenKey) {
        String ajustado = ColorMath.nearestCompliantTone(colorHex, ThemeDefaults.TINTA_OSCURA, CONTRASTE_MINIMO);
        if (!ajustado.equalsIgnoreCase(colorHex)) {
            ajustes.put(tokenKey, ajustado);
        }
        return colorHex;
    }

    private Map<String, Object> typographyScale() {
        Map<String, Object> scale = new LinkedHashMap<>();
        scale.put("display", 30);
        scale.put("title", 22);
        scale.put("body", 17);
        scale.put("label", 14);
        scale.put("price", 20);
        return scale;
    }

    public record AssetUrls(String logo, String logoCompact, String cover, String splash, String productPlaceholder) {
    }
}
