package com.easybeach.promotions;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PromocionesConfig {

    /** Se usa solo si la etapa 14 todavía no publicó su calculadora real. */
    @Bean
    @ConditionalOnMissingBean(CalculadoraPromociones.class)
    public CalculadoraPromociones calculadoraPromociones() {
        return new SinPromocionesCalculadora();
    }
}
