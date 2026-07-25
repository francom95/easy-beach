package com.easybeach.shared.tenancy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una {@code @Entity} como perteneciente a un balneario
 * (columna {@code balneario_id} NOT NULL). Documenta la regla de ADR-001 y
 * es verificada por {@code architecture.ModuleDependencyRulesTest}
 * (ArchUnit): toda entidad con campo {@code balnearioId} debe llevar esta
 * anotación y declarar el filtro Hibernate {@value TenantScoped#FILTER_NAME}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TenantScoped {

    String FILTER_NAME = "tenantFilter";
    String FILTER_PARAM = "balnearioId";
}
