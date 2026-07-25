package com.easybeach.identity.web;

import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import org.springframework.stereotype.Component;

/**
 * El JWT solo lleva el {@code public_id} (ULID) del usuario (etapa 05 §1.2);
 * la auditoría de Super Admin necesita el id numérico interno como
 * {@code actorUsuarioId}. Vive en {@code identity} porque necesita
 * {@code UsuarioRepository}; solo la usan controllers de {@code platform}
 * (ADR-002: {@code platform -> identity} está permitido). Los controllers de
 * {@code branding}/{@code payments} (que NO pueden depender de
 * {@code identity}) no la necesitan: su tenant sale directo de
 * {@code principal.balnearioId()} y no auditan acciones de Super Admin.
 */
@Component
public class CurrentUserResolver {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserResolver(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Long resolveId(EasyBeachUserPrincipal principal) {
        return usuarioRepository.findByPublicId(principal.usuarioPublicId())
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO))
                .getId();
    }
}
