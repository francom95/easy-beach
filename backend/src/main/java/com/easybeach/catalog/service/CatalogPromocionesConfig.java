package com.easybeach.catalog.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogPromocionesConfig {

    /** Se usa solo si {@code promotions} (etapa 14) todavía no publicó la suya. */
    @Bean
    @ConditionalOnMissingBean(PromocionesPublicasProvider.class)
    public PromocionesPublicasProvider promocionesPublicasProvider() {
        return new SinPromocionesPublicasProvider();
    }
}
