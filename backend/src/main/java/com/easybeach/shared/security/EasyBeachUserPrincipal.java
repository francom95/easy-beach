package com.easybeach.shared.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal resuelto a partir de los claims del access token (etapa 05
 * §1.2). {@code balnearioId} viaja SOLO para staff - es la única fuente del
 * tenant operativo (ADR-001), nunca un parámetro de request. Vive en
 * {@code shared} (no {@code identity}): cualquier controller de cualquier
 * módulo necesita leer "quién llama" vía {@code @AuthenticationPrincipal}.
 */
public class EasyBeachUserPrincipal implements UserDetails {

    private final String usuarioPublicId;
    private final Long usuarioId;
    private final TipoUsuario tipo;
    private final RolCodigo rol;
    private final Long balnearioId;

    public EasyBeachUserPrincipal(String usuarioPublicId, Long usuarioId, TipoUsuario tipo,
                                   RolCodigo rol, Long balnearioId) {
        this.usuarioPublicId = usuarioPublicId;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.rol = rol;
        this.balnearioId = balnearioId;
    }

    public String usuarioPublicId() {
        return usuarioPublicId;
    }

    /**
     * Id numérico interno, tomado del claim {@code uid} del token. Evita que
     * cada módulo tenga que consultar {@code identity} solo para traducir
     * ULID → id (dependencia que ADR-002 no permite desde {@code ordering}),
     * y ahorra un lookup a la base por request.
     *
     * <p>No es información sensible: el cliente ya conoce su propia identidad,
     * el token está firmado y ningún endpoint acepta ids numéricos ajenos -
     * todos los recursos se direccionan por {@code public_id} (ULID).
     */
    public Long usuarioId() {
        return usuarioId;
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
