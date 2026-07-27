package com.easybeach.concierge.service;

import com.easybeach.concierge.domain.TipoServicio;
import com.easybeach.concierge.repository.TipoServicioRepository;
import com.easybeach.concierge.web.dto.TipoServicioRequest;
import com.easybeach.concierge.web.dto.TipoServicioResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.tenancy.TenantFilterService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ABM simple del catálogo de servicios (etapa 14): sin precio, admin de balneario. */
@Service
public class TipoServicioService {

    private final TipoServicioRepository repository;
    private final TenantFilterService tenantFilterService;

    public TipoServicioService(TipoServicioRepository repository, TenantFilterService tenantFilterService) {
        this.repository = repository;
        this.tenantFilterService = tenantFilterService;
    }

    @Transactional
    public TipoServicioResponse crear(Long balnearioId, TipoServicioRequest request) {
        tenantFilterService.applyCurrentTenant();
        repository.findByBalnearioIdAndNombre(balnearioId, request.nombre()).ifPresent(existente -> {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Ya existe un tipo de servicio con ese nombre");
        });
        TipoServicio tipo = new TipoServicio();
        tipo.setBalnearioId(balnearioId);
        tipo.setNombre(request.nombre());
        tipo.setActivo(request.activo());
        tipo.setOrden(request.orden());
        return toResponse(repository.save(tipo));
    }

    @Transactional
    public TipoServicioResponse actualizar(Long balnearioId, Long id, TipoServicioRequest request) {
        tenantFilterService.applyCurrentTenant();
        TipoServicio tipo = obtenerPropio(balnearioId, id);
        tipo.setNombre(request.nombre());
        tipo.setActivo(request.activo());
        tipo.setOrden(request.orden());
        return toResponse(repository.save(tipo));
    }

    @Transactional(readOnly = true)
    public List<TipoServicioResponse> listar(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return repository.findByBalnearioIdOrderByOrdenAsc(balnearioId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TipoServicio> listarActivos(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return repository.findByBalnearioIdAndActivoTrueOrderByOrdenAsc(balnearioId);
    }

    @Transactional
    public void eliminar(Long balnearioId, Long id) {
        tenantFilterService.applyCurrentTenant();
        TipoServicio tipo = obtenerPropio(balnearioId, id);
        tipo.setDeletedAt(Instant.now());
        repository.save(tipo);
    }

    private TipoServicio obtenerPropio(Long balnearioId, Long id) {
        return repository.findById(id)
                .filter(t -> t.getBalnearioId().equals(balnearioId))
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
    }

    private TipoServicioResponse toResponse(TipoServicio tipo) {
        return new TipoServicioResponse(tipo.getId(), tipo.getNombre(), tipo.isActivo(), tipo.getOrden());
    }
}
