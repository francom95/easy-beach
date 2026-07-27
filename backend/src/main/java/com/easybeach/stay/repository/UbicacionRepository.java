package com.easybeach.stay.repository;

import com.easybeach.stay.domain.EstadoUbicacion;
import com.easybeach.stay.domain.Ubicacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    List<Ubicacion> findByBalnearioIdOrderByIdentificadorAsc(Long balnearioId);

    /** Para "elegir ubicación" del cliente (etapa 16): solo lo que puede ocupar hoy. */
    List<Ubicacion> findByBalnearioIdAndEstadoOrderByIdentificadorAsc(Long balnearioId, EstadoUbicacion estado);

    Optional<Ubicacion> findByBalnearioIdAndIdentificador(Long balnearioId, String identificador);
}
