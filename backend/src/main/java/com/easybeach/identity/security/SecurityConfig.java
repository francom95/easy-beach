package com.easybeach.identity.security;

import com.easybeach.identity.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

/**
 * API stateless: sin sesión de servidor, sin CSRF (no hay cookies de sesión
 * que proteger). Autorización declarativa por endpoint ({@code @PreAuthorize}
 * en los controllers) - la capa service SIEMPRE verifica ownership/tenant
 * además (etapa 05 §2, "la anotación no alcanza para ownership").
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Etapa 05 §5: "hash con argon2id (o bcrypt cost ≥ 12 si argon2 no está disponible)". */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    /**
     * OJO: {@code JwtAuthenticationFilter} NO se expone como {@code @Bean}
     * de tipo {@code Filter} - Spring Boot auto-registra todo bean
     * {@code Filter} como filtro de servlet vía
     * {@code ServletContextInitializerBeans}, y esa recolección corre
     * temprano en {@code onRefresh()} (antes de que terminen de
     * inicializarse otros singletons), lo que fuerza la creación prematura
     * de {@code UsuarioRepository} y rompe con
     * "Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'".
     * Construirlo a mano acá evita el registro automático y el problema de
     * orden de arranque.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                     UsuarioRepository usuarioRepository) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sin esto, Spring Security devuelve 403 tanto para "sin token" como para
                // "token válido pero rol insuficiente" - etapa 05 exige distinguirlos:
                // 401 sin credenciales válidas, 403 autenticado pero sin permiso.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // El preflight CORS (OPTIONS) del navegador llega SIN el header
                        // Authorization - nunca lo tiene, es solo la negociación previa
                        // (etapa 17: gap real, invisible hasta que un browser real - no
                        // mobile, cuyo fetch no hace preflight - habló con el backend).
                        // Sin este permitAll, anyRequest().authenticated() de más abajo
                        // rechaza el OPTIONS con 401 antes de que CorsFilter responda.
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        // Auth público: registro/login/refresh/logout. cambiar-password NO está acá
                        // a propósito - necesita autenticación (ver AuthController).
                        .requestMatchers("/api/v1/auth/registro", "/api/v1/auth/login/**",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        // Navegación pública de la app cliente (etapa 04 §2: sin auth).
                        .requestMatchers("/api/v1/balnearios/**").permitAll()
                        .requestMatchers("/public/assets/**").permitAll()
                        // Callback de OAuth de Mercado Pago: el navegador llega sin JWT.
                        .requestMatchers("/api/v1/mercadopago/oauth/callback").permitAll()
                        // Webhook de MP: llega servidor-a-servidor, sin JWT. Su
                        // autenticidad la da la firma x-signature (etapa 05 §4.1),
                        // no un token nuestro.
                        .requestMatchers("/api/v1/mercadopago/webhook").permitAll()
                        // /actuator/prometheus lo scrapea Prometheus, que no tiene JWT -
                        // permitAll acá es seguro porque Caddyfile NO expone /actuator/**
                        // al dominio público (etapa 20): sólo es alcanzable dentro de la
                        // red interna de docker-compose.
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
