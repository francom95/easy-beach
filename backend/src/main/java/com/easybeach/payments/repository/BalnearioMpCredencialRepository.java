package com.easybeach.payments.repository;

import com.easybeach.payments.domain.BalnearioMpCredencial;
import com.easybeach.payments.domain.EstadoCredencialMp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalnearioMpCredencialRepository extends JpaRepository<BalnearioMpCredencial, Long> {

    Optional<BalnearioMpCredencial> findByBalnearioId(Long balnearioId);

    boolean existsByBalnearioIdAndEstado(Long balnearioId, EstadoCredencialMp estado);

    /**
     * Job de refresh anticipado (ADR-004): credenciales vinculadas cuyo access
     * token vence antes del límite. Cross-tenant a propósito - lo corre el
     * sistema, no un request de usuario.
     */
    List<BalnearioMpCredencial> findByEstadoAndTokenExpiraAtBefore(EstadoCredencialMp estado, Instant limite);
}
