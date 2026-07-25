package com.easybeach.platform.domain;

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
 * Auditoría de acciones de Super Admin (etapa 05 §7 amenaza #12,
 * mitigación de repudio). {@code balnearioId} es nullable (hay acciones sin
 * balneario, ej. ABM de planes) - el filtro de tenant simplemente no
 * matchea esas filas cuando está habilitado. Super Admin consulta esta
 * tabla SIN habilitar el filtro (cross-tenant intencional, ver
 * {@code AuditoriaPlataformaService}); queda {@code @TenantScoped} por
 * consistencia con ADR-001 y como mecanismo disponible si en el futuro un
 * admin de balneario necesita ver la auditoría de SU propio balneario.
 */
@Entity
@Table(name = "auditoria_plataforma")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaPlataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_usuario_id", nullable = false)
    private Long actorUsuarioId;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(name = "entidad_tipo", nullable = false, length = 40)
    private String entidadTipo;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "balneario_id")
    private Long balnearioId;

    @Column(columnDefinition = "json")
    private String detalle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
