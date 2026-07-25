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
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Credencial OAuth del balneario en su propia cuenta de Mercado Pago
 * (ADR-004). Tokens SIEMPRE cifrados (etapa 05 §4.2) - nunca se exponen en
 * claro, ni en logs, ni en respuestas de API.
 */
@Entity
@Table(name = "balneario_mp_credencial")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class BalnearioMpCredencial extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(name = "mp_user_id", nullable = false, length = 40)
    private String mpUserId;

    @Column(name = "access_token_cifrado", nullable = false, columnDefinition = "VARBINARY(1024)")
    private byte[] accessTokenCifrado;

    @Column(name = "refresh_token_cifrado", nullable = false, columnDefinition = "VARBINARY(1024)")
    private byte[] refreshTokenCifrado;

    @Column(name = "token_expira_at", nullable = false)
    private Instant tokenExpiraAt;

    @Column(length = 120)
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCredencialMp estado = EstadoCredencialMp.VINCULADA;

    /** Nunca incluir en toString/logs: evita que un log accidental de la entidad exponga los bytes cifrados. */
    @Override
    public String toString() {
        return "BalnearioMpCredencial{balnearioId=" + balnearioId + ", estado=" + estado + "}";
    }
}
