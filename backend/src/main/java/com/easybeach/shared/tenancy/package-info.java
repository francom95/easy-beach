/**
 * {@code @FilterDef} declarado UNA sola vez a nivel de paquete: Hibernate
 * rechaza (`AnnotationException: Multiple '@FilterDef' annotations define a
 * filter named 'tenantFilter'`) que el mismo filtro se declare de nuevo en
 * cada {@code @Entity} - cada entidad {@code @TenantScoped} debe llevar
 * SOLO {@code @Filter(name = TenantScoped.FILTER_NAME, condition = "...")},
 * nunca su propio {@code @FilterDef}.
 */
@FilterDef(name = TenantScoped.FILTER_NAME, parameters = @ParamDef(name = TenantScoped.FILTER_PARAM, type = Long.class))
package com.easybeach.shared.tenancy;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
