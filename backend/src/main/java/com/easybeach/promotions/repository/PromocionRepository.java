package com.easybeach.promotions.repository;

import com.easybeach.promotions.domain.EstadoPromocion;
import com.easybeach.promotions.domain.Promocion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    List<Promocion> findByBalnearioIdOrderByIdDesc(Long balnearioId);

    /** Universo a evaluar en cada pedido/menú: activas, no borradas (soft-delete ya filtra). */
    List<Promocion> findByBalnearioIdAndEstado(Long balnearioId, EstadoPromocion estado);

    Optional<Promocion> findByIdAndBalnearioId(Long id, Long balnearioId);
}
