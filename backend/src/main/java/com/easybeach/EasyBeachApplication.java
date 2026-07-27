package com.easybeach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @EnableScheduling}: job de expiración de estadías (etapa 12). */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class EasyBeachApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyBeachApplication.class, args);
    }
}
