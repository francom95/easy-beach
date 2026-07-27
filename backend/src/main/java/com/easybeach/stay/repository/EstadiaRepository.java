package com.easybeach.stay.repository;

import com.easybeach.stay.domain.Estadia;
import com.easybeach.stay.domain.EstadoEstadia;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstadiaRepository extends JpaRepository<Estadia, Long> {

    Optional<Estadia> findByPublicId(String publicId);

    /**
     * La estadía que ocupa cupo del cliente en ESE balneario (pendiente o
     * activa). Se apoya en el mismo invariante que el UK: a lo sumo hay una.
     */
    Optional<Estadia> findByBalnearioIdAndActivaUk(Long balnearioId, Long clienteId);

    /** "Mis estadías": todas las del cliente, en todos los balnearios (cross-tenant por diseño - es del cliente). */
    List<Estadia> findByClienteIdOrderByFechaSolicitudDesc(Long clienteId);

    /** Estadías del cliente que ocupan cupo, en cualquier balneario (la app muestra dónde está "adentro"). */
    @Query("select e from Estadia e where e.clienteId = :clienteId and e.activaUk is not null")
    List<Estadia> findVigentesDelCliente(@Param("clienteId") Long clienteId);

    /** Cola de validación del carpero (etapa 17), más antiguas primero. */
    List<Estadia> findByBalnearioIdAndEstadoOrderByFechaSolicitudAsc(Long balnearioId, EstadoEstadia estado);

    boolean existsByUbicacionIdAndEstadoIn(Long ubicacionId, List<EstadoEstadia> estados);

    /**
     * Solicitudes que superaron el TTL de validación. Cross-tenant a
     * propósito: lo ejecuta un job del sistema, no un request de usuario.
     */
    @Query("""
            select e from Estadia e
            where e.estado = com.easybeach.stay.domain.EstadoEstadia.PENDIENTE_VALIDACION
              and e.fechaSolicitud < :limite
            """)
    List<Estadia> findPendientesVencidas(@Param("limite") Instant limite);
}
