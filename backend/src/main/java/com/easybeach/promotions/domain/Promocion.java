package com.easybeach.promotions.domain;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Promoción básica del MVP (etapa 03 §3.9 / etapa 14): descuento
 * porcentual, combo a precio fijo o happy hour. {@code vigenciaDesde/Hasta}
 * y la franja horaria son opcionales - {@code null} significa "sin límite"
 * en esa dimensión.
 *
 * <p>{@code diasSemana} es una lista separada por comas de códigos de 3
 * letras en español ("LUN,MAR,MIE,JUE,VIE,SAB,DOM"); {@code null} o vacío
 * significa "todos los días". Solo tiene sentido para {@code HAPPY_HOUR}
 * (ver {@link com.easybeach.promotions.service.VigenciaPromocionChecker}).
 */
@Entity
@Table(name = "promocion")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@SQLRestriction("deleted_at is null")
@Getter
@Setter
@NoArgsConstructor
public class Promocion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TipoPromocion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoPromocion estado = EstadoPromocion.ACTIVA;

    /** % (descuento/happy hour) o precio fijo del combo, según {@code tipo}. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "vigencia_desde")
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Column(name = "franja_hora_desde")
    private LocalTime franjaHoraDesde;

    @Column(name = "franja_hora_hasta")
    private LocalTime franjaHoraHasta;

    @Column(name = "dias_semana", length = 20)
    private String diasSemana;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
