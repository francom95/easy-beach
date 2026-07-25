package com.easybeach.shared.tenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Capa 2 de la defensa en profundidad de ADR-001: habilita el filtro
 * Hibernate {@value TenantScoped#FILTER_NAME} sobre el {@link Session} de la
 * transacción actual.
 *
 * <p><b>Convención obligatoria</b> para todo service {@code @Transactional}
 * que toque una entidad {@code @TenantScoped}: llamar
 * {@link #applyCurrentTenant()} como PRIMERA línea del método. Al depender de
 * {@code @PersistenceContext} (no de un filtro de servlet ni de OSIV), esto
 * es determinístico: siempre resuelve el EntityManager de la transacción en
 * curso, sin importar el orden de filtros/interceptors HTTP.
 *
 * <p>Si no hay {@link TenantContext} resuelto, lanza
 * {@link TenantContextMissingException} en vez de dejar pasar una query sin
 * filtrar - "una query sin tenant falla" (criterio de aceptación etapa 09).
 */
@Component
public class TenantFilterService {

    @PersistenceContext
    private EntityManager entityManager;

    public void applyCurrentTenant() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new TenantContextMissingException();
        }
        entityManager.unwrap(Session.class)
                .enableFilter(TenantScoped.FILTER_NAME)
                .setParameter(TenantScoped.FILTER_PARAM, tenantId);
    }
}
