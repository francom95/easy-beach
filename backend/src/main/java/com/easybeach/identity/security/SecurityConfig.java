package com.easybeach.identity.security;

import com.easybeach.identity.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
