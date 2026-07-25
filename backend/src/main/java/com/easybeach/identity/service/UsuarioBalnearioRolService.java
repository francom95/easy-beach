package com.easybeach.identity.service;

import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.repository.UsuarioBalnearioRolRepository;
import com.easybeach.shared.tenancy.TenantFilterService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primer service tenant-scoped del proyecto: demuestra la convención
 * obligatoria de {@link TenantFilterService} (etapa 09, criterio de
 * aceptación "multitenancy operativo").
 */
@Service
public class UsuarioBalnearioRolService {

    private final TenantFilterService tenantFilterService;
    private final UsuarioBalnearioRolRepository repository;

    public UsuarioBalnearioRolService(TenantFilterService tenantFilterService,
                                       UsuarioBalnearioRolRepository repository) {
        this.tenantFilterService = tenantFilterService;
        this.repository = repository;
    }

    /**
     * {@code findAll()} sin ningún {@code WHERE balneario_id} explícito en
     * el código: el aislamiento lo garantiza el filtro Hibernate habilitado
     * por {@link TenantFilterService#applyCurrentTenant()}, no la query.
     */
    @Transactional(readOnly = true)
    public List<UsuarioBalnearioRol> listarMiembrosDelBalnearioActual() {
        tenantFilterService.applyCurrentTenant();
        return repository.findAll();
    }
}
