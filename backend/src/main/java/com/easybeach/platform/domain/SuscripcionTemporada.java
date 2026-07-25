package com.easybeach.platform.domain;

import com.easybeach.shared.audit.Auditable;
import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Balneario × plan × temporada. Tenant-scoped por consistencia con ADR-001
 * (toda tabla con {@code balneario_id} lleva el mismo tratamiento, aunque el
 * acceso típico ya sea explícito por balneario) - defensa en profundidad,
 * no una excepción de conveniencia.
 */
@Entity
@Table(name = "suscripcion_temporada")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class SuscripcionTemporada extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "temporada_id", nullable = false)
    private Temporada temporada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSuscripcion estado = EstadoSuscripcion.PENDIENTE;

    @Column(name = "fecha_alta", nullable = false)
    private Instant fechaAlta = Instant.now();
}
