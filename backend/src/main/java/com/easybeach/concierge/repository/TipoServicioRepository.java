package com.easybeach.concierge.repository;

import com.easybeach.concierge.domain.TipoServicio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoServicioRepository extends JpaRepository<TipoServicio, Long> {

    List<TipoServicio> findByBalnearioIdOrderByOrdenAsc(Long balnearioId);

    List<TipoServicio> findByBalnearioIdAndActivoTrueOrderByOrdenAsc(Long balnearioId);

    Optional<TipoServicio> findByBalnearioIdAndNombre(Long balnearioId, String nombre);
}
