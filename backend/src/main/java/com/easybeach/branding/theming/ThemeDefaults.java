package com.easybeach.branding.theming;

/** Defaults del theme EasyBeach (tokens.md). Semilla de balnearios nuevos y fallback de faltantes. */
public final class ThemeDefaults {

    private ThemeDefaults() {
    }

    public static final String CONTRACT_VERSION = "1.0";
    public static final String NAME = "EasyBeach";

    public static final String COLOR_PRIMARY = "#C95100";
    public static final String COLOR_SECONDARY = "#17437B";
    public static final String COLOR_BACKGROUND = "#F5EFE2";
    public static final String COLOR_SURFACE = "#FFFFFF";
    public static final String COLOR_SUCCESS = "#1E7D3C";
    public static final String COLOR_WARNING = "#B25E00";
    public static final String COLOR_ERROR = "#C22F2F";
    public static final String COLOR_INFO = "#1D62B4";

    /** "Tinta oscura" candidata para derivar on-* (default on-background/on-surface del contrato). */
    public static final String TINTA_OSCURA = "#1B2B40";

    public static final TypographyFamily TYPOGRAPHY_FAMILY = TypographyFamily.CLARA;

    public static final String ASSET_LOGO = "/assets/easybeach/logo.svg";
    public static final String ASSET_LOGO_COMPACT = "/assets/easybeach/logo-compact.svg";
    public static final String ASSET_COVER = "/assets/easybeach/cover.jpg";
    public static final String ASSET_SPLASH = "/assets/easybeach/splash.png";
    public static final String ASSET_PRODUCT_PLACEHOLDER = "/assets/easybeach/product-placeholder.png";
}
