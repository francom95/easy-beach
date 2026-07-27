package com.easybeach.stay.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StayConsumoConfig {

    /** Se usa solo si {@code ordering} (etapa 13) todavía no publicó la suya. */
    @Bean
    @ConditionalOnMissingBean(ConsumoEstadiaProvider.class)
    public ConsumoEstadiaProvider consumoEstadiaProvider() {
        return new SinPedidosConsumoProvider();
    }
}
