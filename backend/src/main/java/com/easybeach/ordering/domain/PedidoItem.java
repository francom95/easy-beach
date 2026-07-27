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
 * Snapshot <b>congelado e inmutable</b> (etapa 03 §3.6): nombre y precio se
 * copian al momento del pedido y no vuelven a mirar el catálogo. Si mañana
 * el balneario sube el precio o borra el producto, este histórico no cambia.
 *
 * <p>{@code productoId}/{@code productoVarianteId} son referencias blandas,
 * solo para trazabilidad y reportes - jamás para leer el precio.
 */
@Entity
@Table(name = "pedido_item")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class PedidoItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "producto_variante_id")
    private Long productoVarianteId;

    @Column(name = "nombre_producto", nullable = false, length = 120)
    private String nombreProducto;

    @Column(name = "nombre_variante", length = 80)
    private String nombreVariante;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "subtotal_linea", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalLinea;
}
