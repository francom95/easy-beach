package com.easybeach.ordering.repository;

import com.easybeach.ordering.domain.PedidoEvento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoEventoRepository extends JpaRepository<PedidoEvento, Long> {

    List<PedidoEvento> findByPedidoIdOrderByCreatedAtAsc(Long pedidoId);
}
