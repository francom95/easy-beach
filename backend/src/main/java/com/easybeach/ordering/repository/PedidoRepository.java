package com.easybeach.ordering.repository;

import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.domain.Pedido;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByPublicId(String publicId);

    /** Idempotencia (etapa 04 §1.6): misma clave + mismo balneario = mismo pedido. */
    Optional<Pedido> findByBalnearioIdAndIdempotencyKey(Long balnearioId, String idempotencyKey);

    /** Cola operativa: solo pedidos que ya entraron (pago aprobado), más antiguos primero. */
    List<Pedido> findByBalnearioIdAndEstadoInOrderByCreatedAtAsc(Long balnearioId, List<EstadoPedido> estados);

    List<Pedido> findByEstadiaIdOrderByCreatedAtDesc(Long estadiaId);

    List<Pedido> findByClienteIdOrderByCreatedAtDesc(Long clienteId);

    boolean existsByEstadiaIdAndEstadoIn(Long estadiaId, List<EstadoPedido> estados);

    @Query("select count(p) from Pedido p where p.estadiaId = :estadiaId and p.estado = :estado")
    long contarPorEstadiaYEstado(@Param("estadiaId") Long estadiaId, @Param("estado") EstadoPedido estado);

    @Query("select coalesce(sum(p.total), 0) from Pedido p where p.estadiaId = :estadiaId and p.estado = :estado")
    BigDecimal sumarTotalPorEstadiaYEstado(@Param("estadiaId") Long estadiaId, @Param("estado") EstadoPedido estado);
}
