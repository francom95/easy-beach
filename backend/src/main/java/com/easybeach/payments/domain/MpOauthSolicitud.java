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
 * Estado transitorio anti-CSRF del flujo OAuth (ADR-004). Se busca por
 * {@code state} SIN balneario conocido de antemano (el callback público de
 * MP no trae tenant) - mismo patrón de excepción documentado que el login de
 * staff en {@code UsuarioBalnearioRolRepository}.
 */
@Entity
@Table(name = "mp_oauth_solicitud")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class MpOauthSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(nullable = false, columnDefinition = "CHAR(43)")
    private String state;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "expira_at", nullable = false)
    private Instant expiraAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
