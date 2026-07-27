package com.easybeach.stay.domain;

import com.easybeach.shared.audit.Auditable;
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
 * Historial de ubicaciones de una estadía: "hoy carpa 12, mañana carpa 15"
 * (etapa 12). El tramo abierto es el que tiene {@code hasta = null}.
 */
@Entity
@Table(name = "estadia_ubicacion_historial")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class EstadiaUbicacionHistorial extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estadia_id", nullable = false)
    private Long estadiaId;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(name = "ubicacion_id", nullable = false)
    private Long ubicacionId;

    @Column(nullable = false)
    private Instant desde = Instant.now();

    @Column
    private Instant hasta;
}
