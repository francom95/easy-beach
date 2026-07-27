package com.easybeach.ordering.domain;

import com.easybeach.shared.audit.Auditable;
import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Descuento aplicado a un pedido, <b>congelado</b>: el nombre y el monto
 * quedan grabados acá, así una promo vencida o borrada después no altera
 * pedidos históricos (etapa 14). {@code promocionId} es referencia blanda.
 */
@Entity
@Table(name = "pedido_promocion")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class PedidoPromocion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(name = "promocion_id")
    private Long promocionId;

    @Column(name = "nombre_promocion", nullable = false, length = 120)
    private String nombrePromocion;

    @Column(name = "monto_descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDescuento;
}
