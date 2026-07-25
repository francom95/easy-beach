package com.easybeach.identity.security;

import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.identity.domain.RolCodigo;
import com.easybeach.identity.domain.TipoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.shared.tenancy.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Valida el access token, resuelve el {@link EasyBeachUserPrincipal} y
 * puebla {@link TenantContext} + MDC {@code balnearioId} (etapa 05 §1.2:
 * "balneario_id viaja en el token de staff y es la única fuente del tenant
 * operativo"). Rechaza tokens de usuarios dados de baja (§1.3 revocación).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String MDC_TENANT_KEY = "balnearioId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                authenticate(header.substring(BEARER_PREFIX.length()), request);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove(MDC_TENANT_KEY);
        }
    }

    private void authenticate(String token, HttpServletRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(token);
        } catch (JwtException e) {
            return; // token inválido/expirado: request sigue anónimo, Security lo rechaza si el endpoint lo exige
        }

        String usuarioPublicId = claims.getSubject();
        Optional<Usuario> usuario = usuarioRepository.findByPublicId(usuarioPublicId);
        if (usuario.isEmpty() || usuario.get().getEstado() == EstadoUsuario.BAJA) {
            return; // revocación por baja de staff/cliente (etapa 05 §1.3)
        }

        TipoUsuario tipo = TipoUsuario.valueOf(claims.get("tipo", String.class));
        RolCodigo rol = RolCodigo.valueOf(claims.get("rol", String.class));
        Long balnearioId = claims.get("balneario_id", Long.class);
        if (balnearioId == null) {
            Integer asInt = claims.get("balneario_id", Integer.class);
            balnearioId = asInt != null ? asInt.longValue() : null;
        }

        var principal = new EasyBeachUserPrincipal(usuarioPublicId, tipo, rol, balnearioId);
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (balnearioId != null) {
            TenantContext.set(balnearioId);
            MDC.put(MDC_TENANT_KEY, String.valueOf(balnearioId));
        }
    }
}
