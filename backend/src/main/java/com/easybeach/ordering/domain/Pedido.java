package com.easybeach.ordering.domain;

import com.easybeach.shared.audit.Auditable;
import com.easybeach.shared.id.UlidGenerator;
import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * El corazón transaccional: acá se concreta la venta.
 *
 * <p>{@code subtotal}/{@code descuentoTotal}/{@code total} los calcula
 * SIEMPRE el servidor (etapa 03 §3.6); lo que mande el cliente se ignora.
 */
@Entity
@Table(name = "pedido")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class Pedido extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "CHAR(26)")
    private String publicId;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(name = "estadia_id", nullable = false)
    private Long estadiaId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    /** Clave del canal SSE del cliente (ver comentario en la migración V008). */
    @Column(name = "cliente_public_id", nullable = false, columnDefinition = "CHAR(26)")
    private String clientePublicId;

    @Column(name = "ubicacion_id", nullable = false)
    private Long ubicacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.CREADO;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "descuento_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "motivo_cancelacion", length = 300)
    private String motivoCancelacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<PedidoItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<PedidoPromocion> promociones = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (publicId == null) {
            publicId = UlidGenerator.generate();
        }
    }

    public void agregarItem(PedidoItem item) {
        item.setPedido(this);
        items.add(item);
    }

    public void agregarPromocion(PedidoPromocion promocion) {
        promocion.setPedido(this);
        promociones.add(promocion);
    }

    /** Única vía de cambio de estado; el registro en {@code pedido_evento} lo hace el service. */
    public void transicionarA(EstadoPedido nuevoEstado) {
        if (!estado.puedeTransicionarA(nuevoEstado)) {
            throw new IllegalStateException("Transición inválida: " + estado + " -> " + nuevoEstado);
        }
        this.estado = nuevoEstado;
    }
}
