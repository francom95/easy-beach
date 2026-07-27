package com.easybeach.concierge.repository;

import com.easybeach.concierge.domain.EstadoSolicitudServicio;
import com.easybeach.concierge.domain.SolicitudServicio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudServicioRepository extends JpaRepository<SolicitudServicio, Long> {

    Optional<SolicitudServicio> findByPublicId(String publicId);

    /** Cola del carpero: activas (no terminales), por antigüedad. */
    List<SolicitudServicio> findByBalnearioIdAndEstadoInOrderByCreatedAtAsc(Long balnearioId,
                                                                            List<EstadoSolicitudServicio> estados);

    List<SolicitudServicio> findByEstadiaIdOrderByCreatedAtDesc(Long estadiaId);
}
