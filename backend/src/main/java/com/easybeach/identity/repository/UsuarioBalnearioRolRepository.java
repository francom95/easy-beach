package com.easybeach.identity.repository;

import com.easybeach.identity.domain.UsuarioBalnearioRol;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioBalnearioRolRepository extends JpaRepository<UsuarioBalnearioRol, Long> {

    /**
     * ÚNICA excepción documentada a la convención de
     * {@link com.easybeach.shared.tenancy.TenantFilterService}: durante el
     * login de staff todavía no sabemos a qué balneario pertenece el
     * usuario (es justamente lo que esta consulta resuelve), así que se
     * ejecuta intencionalmente SIN habilitar el filtro de tenant. Ningún
     * otro caller de este repositorio debería omitir
     * {@code applyCurrentTenant()}.
     */
    List<UsuarioBalnearioRol> findByUsuarioId(Long usuarioId);

    List<UsuarioBalnearioRol> findByBalnearioId(Long balnearioId);
}
