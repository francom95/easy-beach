package com.easybeach.promotions.domain;

import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/** A qué producto o categoría aplica un descuento %/happy hour. */
@Entity
@Table(name = "promocion_alcance")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class PromocionAlcance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promocion_id", nullable = false)
    private Long promocionId;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alcance", nullable = false, length = 12)
    private TipoAlcance tipoAlcance;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;
}
