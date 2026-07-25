package com.easybeach.identity.repository;

import com.easybeach.identity.domain.EstadoSesion;
import com.easybeach.identity.domain.SesionRefresh;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionRefreshRepository extends JpaRepository<SesionRefresh, Long> {

    Optional<SesionRefresh> findByTokenHash(String tokenHash);

    List<SesionRefresh> findByFamiliaIdAndEstado(String familiaId, EstadoSesion estado);

    List<SesionRefresh> findByUsuarioIdAndEstado(Long usuarioId, EstadoSesion estado);
}
