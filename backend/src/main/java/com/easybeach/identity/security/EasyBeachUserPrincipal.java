package com.easybeach.identity.security;

import com.easybeach.identity.domain.RolCodigo;
import com.easybeach.identity.domain.TipoUsuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal resuelto a partir de los claims del access token (etapa 05
 * §1.2). {@code balnearioId} viaja SOLO para staff - es la única fuente del
 * tenant operativo (ADR-001), nunca un parámetro de request.
 */
public class EasyBeachUserPrincipal implements UserDetails {

    private final String usuarioPublicId;
    private final TipoUsuario tipo;
    private final RolCodigo rol;
    private final Long balnearioId;

    public EasyBeachUserPrincipal(String usuarioPublicId, TipoUsuario tipo, RolCodigo rol, Long balnearioId) {
        this.usuarioPublicId = usuarioPublicId;
        this.tipo = tipo;
        this.rol = rol;
        this.balnearioId = balnearioId;
    }

    public String usuarioPublicId() {
        return usuarioPublicId;
    }

    public TipoUsuario tipo() {
        return tipo;
    }

    public RolCodigo rol() {
        return rol;
    }

    /** {@code null} para cliente y super admin: su tenant no viaja en el token. */
    public Long balnearioId() {
        return balnearioId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return usuarioPublicId;
    }
}
