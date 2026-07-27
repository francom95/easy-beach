package com.easybeach.payments.domain;

import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Registro de cada notificación recibida de MP: da idempotencia (UK sobre
 * {@code mp_payment_id + tipo + payload_hash}) y auditoría del webhook.
 *
 * <p>{@code balnearioId} es nullable: el webhook llega sin contexto de tenant
 * (MP no sabe de nuestros balnearios) y recién se conoce tras resolver el
 * pago; una notificación de un pago desconocido queda sin balneario. Se marca
 * {@code @TenantScoped} igual, por la misma razón que
 * {@code AuditoriaPlataforma} (etapa 10): consistencia con ADR-001 y
 * mecanismo disponible si algún día un admin consulta las notificaciones de
 * SU balneario. El flujo del webhook no habilita el filtro, así que la
 * búsqueda por {@code payment_id} sigue funcionando sin tenant.
 */
@Entity
@Table(name = "mp_webhook_notificacion")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class MpWebhookNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balneario_id")
    private Long balnearioId;

    @Column(name = "mp_payment_id", nullable = false, length = 40)
    private String mpPaymentId;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(name = "payload_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String payloadHash;

    @Column(name = "recibido_at", nullable = false)
    private Instant recibidoAt = Instant.now();

    @Column(nullable = false)
    private boolean procesado = false;

    @Column(length = 40)
    private String resultado;
}
