package com.easybeach.branding.storage;

import com.easybeach.branding.theming.ThemeTokenKeys;

/** Qué asset de theming se está subiendo (tokens.md §Assets). */
public enum AssetType {
    LOGO(ThemeTokenKeys.ASSET_LOGO),
    LOGO_COMPACT(ThemeTokenKeys.ASSET_LOGO_COMPACT),
    COVER(ThemeTokenKeys.ASSET_COVER),
    SPLASH(ThemeTokenKeys.ASSET_SPLASH),
    PRODUCT_PLACEHOLDER(ThemeTokenKeys.ASSET_PRODUCT_PLACEHOLDER);

    private final String tokenKey;

    AssetType(String tokenKey) {
        this.tokenKey = tokenKey;
    }

    public String tokenKey() {
        return tokenKey;
    }
}
