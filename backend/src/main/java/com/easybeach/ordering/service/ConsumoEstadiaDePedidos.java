package com.easybeach.ordering.service;

import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.repository.PedidoRepository;
import com.easybeach.stay.service.ConsumoEstadiaProvider;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra la inversión de dependencia que dejó planteada la etapa 12: la
 * interfaz vive en {@code stay}, la implementación real llega desde
 * {@code ordering} (dirección permitida por ADR-002).
 *
 * <p>Al existir este bean, {@code SinPedidosConsumoProvider}
 * ({@code @ConditionalOnMissingBean}) deja de registrarse solo - sin tocar
 * una línea de {@code stay}, tal como se había previsto.
 */
@Service
public class ConsumoEstadiaDePedidos implements ConsumoEstadiaProvider {

    private static final List<EstadoPedido> EN_CURSO = Arrays.stream(EstadoPedido.values())
            .filter(EstadoPedido::estaEnCurso)
            .toList();

    private final PedidoRepository pedidoRepository;

    public ConsumoEstadiaDePedidos(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    /** Decisión de la etapa 12: con pedidos en curso, el cierre de la estadía se BLOQUEA. */
    @Override
    @Transactional(readOnly = true)
    public boolean tienePedidosEnCurso(Long estadiaId) {
        return pedidoRepository.existsByEstadiaIdAndEstadoIn(estadiaId, EN_CURSO);
    }

    /** Solo cuenta lo ENTREGADO: el resumen de cierre refleja consumo real, no intenciones. */
    @Override
    @Transactional(readOnly = true)
    public ResumenConsumo obtenerResumen(Long estadiaId) {
        long cantidad = pedidoRepository.contarPorEstadiaYEstado(estadiaId, EstadoPedido.ENTREGADO);
        BigDecimal total = pedidoRepository.sumarTotalPorEstadiaYEstado(estadiaId, EstadoPedido.ENTREGADO);
        return new ResumenConsumo((int) cantidad, total == null ? BigDecimal.ZERO : total);
    }
}
