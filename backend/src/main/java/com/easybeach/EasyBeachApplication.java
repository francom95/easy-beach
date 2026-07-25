package com.easybeach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EasyBeachApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyBeachApplication.class, args);
    }
}
