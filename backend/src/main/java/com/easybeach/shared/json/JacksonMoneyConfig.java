package com.easybeach.shared.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los montos viajan como string decimal en JSON (etapa 02 §5: "en JSON viaja
 * como string decimal (\"1500.00\") para evitar float"), nunca como number.
 */
@Configuration
public class JacksonMoneyConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer moneyAsStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
            builder.modulesToInstall(m -> m.add(module));
        };
    }
}
