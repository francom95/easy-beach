package com.easybeach.payments.domain;

import com.easybeach.shared.audit.Auditable;
import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Intento de cobro contra la cuenta MP del balneario (ADR-004). 1:N con
 * pedido porque un pago rechazado se puede reintentar; a lo sumo uno queda
 * {@code APROBADO} (validado en el service).
 */
@Entity
@Table(name = "pedido_pago")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class PedidoPago extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "mp_preference_id", length = 80)
    private String mpPreferenceId;

    @Column(name = "mp_payment_id", length = 40)
    private String mpPaymentId;

    @Column(name = "mp_status_detail", length = 80)
    private String mpStatusDetail;

    @Column(length = 40)
    private String metodo;
}
