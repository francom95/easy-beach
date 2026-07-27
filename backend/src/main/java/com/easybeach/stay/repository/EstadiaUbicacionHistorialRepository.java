package com.easybeach.stay.repository;

import com.easybeach.stay.domain.EstadiaUbicacionHistorial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadiaUbicacionHistorialRepository extends JpaRepository<EstadiaUbicacionHistorial, Long> {

    List<EstadiaUbicacionHistorial> findByEstadiaIdOrderByDesdeAsc(Long estadiaId);

    /** El tramo abierto (sin {@code hasta}) - hay a lo sumo uno por estadía. */
    Optional<EstadiaUbicacionHistorial> findByEstadiaIdAndHastaIsNull(Long estadiaId);
}
