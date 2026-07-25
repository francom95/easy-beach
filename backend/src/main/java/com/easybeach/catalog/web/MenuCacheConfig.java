package com.easybeach.catalog.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * {@code ETag} solo para el menú público (no globalmente - el resto de la
 * API no se beneficia y calcular el hash del body en cada response sería
 * costo sin propósito).
 */
@Configuration
public class MenuCacheConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> menuEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/v1/balnearios/*/menu");
        return registration;
    }
}
