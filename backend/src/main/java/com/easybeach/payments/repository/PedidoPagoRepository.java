package com.easybeach.payments.repository;

import com.easybeach.payments.domain.EstadoPago;
import com.easybeach.payments.domain.PedidoPago;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoPagoRepository extends JpaRepository<PedidoPago, Long> {

    /**
     * Lookup del webhook: llega solo con el {@code payment_id} de MP, sin
     * contexto de tenant - excepción documentada al filtro, igual que el
     * callback OAuth.
     */
    Optional<PedidoPago> findByMpPaymentId(String mpPaymentId);

    List<PedidoPago> findByPedidoIdOrderByIdAsc(Long pedidoId);

    Optional<PedidoPago> findByPedidoIdAndEstado(Long pedidoId, EstadoPago estado);

    boolean existsByPedidoIdAndEstado(Long pedidoId, EstadoPago estado);

    /** Job de reconciliación (ADR-004): pagos con webhook perdido o demorado. */
    List<PedidoPago> findByEstadoAndCreatedAtBefore(EstadoPago estado, Instant limite);
}
