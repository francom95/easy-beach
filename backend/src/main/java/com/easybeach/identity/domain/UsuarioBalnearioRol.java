package com.easybeach.identity.domain;

import com.easybeach.shared.audit.Auditable;
import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * El staff pertenece a un balneario con un rol (etapa 03 §3.2). Primera
 * entidad {@code @TenantScoped} del proyecto: valida el mecanismo de
 * ADR-001/{@link com.easybeach.shared.tenancy.TenantFilterService}.
 */
@Entity
@Table(name = "usuario_balneario_rol")
@TenantScoped
@FilterDef(name = TenantScoped.FILTER_NAME, parameters = @ParamDef(name = TenantScoped.FILTER_PARAM, type = Long.class))
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class UsuarioBalnearioRol extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;
}
