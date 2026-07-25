package com.easybeach.platform.repository;

import com.easybeach.platform.domain.EstadoSuscripcion;
import com.easybeach.platform.domain.SuscripcionTemporada;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SuscripcionTemporadaRepository extends JpaRepository<SuscripcionTemporada, Long> {

    /**
     * Usado por Super Admin (cross-tenant intencional, sin
     * {@code TenantFilterService.applyCurrentTenant()} - ver ADR-001 §"acceso
     * cross-tenant solo Super Admin, siempre auditado") y por el listado
     * público de balnearios activos.
     */
    List<SuscripcionTemporada> findByBalnearioId(Long balnearioId);

    Optional<SuscripcionTemporada> findByBalnearioIdAndTemporadaId(Long balnearioId, Long temporadaId);

    boolean existsByBalnearioIdAndTemporadaIdAndEstado(Long balnearioId, Long temporadaId, EstadoSuscripcion estado);

    @Query("""
            select distinct s.balnearioId from SuscripcionTemporada s
            where s.estado = com.easybeach.platform.domain.EstadoSuscripcion.ACTIVA
              and s.temporada.estado = com.easybeach.platform.domain.EstadoTemporada.EN_CURSO
            """)
    List<Long> findBalnearioIdsConSuscripcionActivaEnTemporadaEnCurso();
}
