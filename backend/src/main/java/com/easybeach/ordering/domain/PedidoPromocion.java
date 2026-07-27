package com.easybeach.ordering.domain;

import com.easybeach.shared.audit.Auditable;
import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 *
 * <p>{@code pedido} es {@code @ManyToOne} (mismo patrón que
 * {@link PedidoItem#getPedido()}) - <b>no</b> un {@code @JoinColumn}
 * unidireccional del lado de {@code Pedido}: eso hace que Hibernate
 * inserte primero sin el FK y lo actualice después, lo que revienta porque
 * {@code pedido_id} es {@code NOT NULL} (bug real, encontrado al verificar
 * esta etapa - en la 13 nunca se ejecutó porque no había promociones).
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(name = "promocion_id")
    private Long promocionId;

    @Column(name = "nombre_promocion", nullable = false, length = 120)
    private String nombrePromocion;

    @Column(name = "monto_descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDescuento;
}
