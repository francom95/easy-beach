package com.easybeach.payments.repository;

import com.easybeach.payments.domain.BalnearioMpCredencial;
import com.easybeach.payments.domain.EstadoCredencialMp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalnearioMpCredencialRepository extends JpaRepository<BalnearioMpCredencial, Long> {

    Optional<BalnearioMpCredencial> findByBalnearioId(Long balnearioId);

    boolean existsByBalnearioIdAndEstado(Long balnearioId, EstadoCredencialMp estado);
}
