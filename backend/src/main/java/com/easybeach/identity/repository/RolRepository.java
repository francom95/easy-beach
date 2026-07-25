package com.easybeach.identity.repository;

import com.easybeach.identity.domain.Rol;
import com.easybeach.shared.security.RolCodigo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByCodigo(RolCodigo codigo);
}
