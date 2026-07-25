package com.easybeach.stay.domain;

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
import org.hibernate.annotations.SQLRestriction;

/**
 * Ubicación física de entrega (carpa/sombrilla/mesa/sector). Adelantada del
 * módulo {@code stay} (etapa 12 construye el resto: estadía, validación por
 * carpero) porque la etapa 11 necesita administrarlas ya.
 *
 * <p>{@code @SQLRestriction}: exclusión de soft-deleted SIEMPRE activa (no es
 * session-scoped como el filtro de tenant - acá no hay "a veces mostrar
 * borrados"). Compone con {@code @Filter} sin conflicto: ambas condiciones
 * se agregan con AND en el SQL generado.
 */
@Entity
@Table(name = "ubicacion")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@SQLRestriction("deleted_at is null")
@Getter
@Setter
@NoArgsConstructor
public class Ubicacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TipoUbicacion tipo;

    @Column(nullable = false, length = 40)
    private String identificador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoUbicacion estado = EstadoUbicacion.ACTIVA;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
