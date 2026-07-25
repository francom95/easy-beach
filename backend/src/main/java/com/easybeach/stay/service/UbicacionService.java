package com.easybeach.stay.service;

import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.tenancy.TenantFilterService;
import com.easybeach.stay.domain.EstadoUbicacion;
import com.easybeach.stay.domain.Ubicacion;
import com.easybeach.stay.repository.UbicacionRepository;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ABM de ubicaciones físicas (etapa 11). "No desactivar/borrar una ubicación
 * con estadía activa" (etapa 03 §3.3) queda como comentario-hook: la entidad
 * {@code Estadia} no existe todavía (se construye en la etapa 12), que debe
 * agregar esa validación acá al implementarse.
 */
@Service
public class UbicacionService {

    private final UbicacionRepository repository;
    private final TenantFilterService tenantFilterService;

    public UbicacionService(UbicacionRepository repository, TenantFilterService tenantFilterService) {
        this.repository = repository;
        this.tenantFilterService = tenantFilterService;
    }

    @Transactional
    public UbicacionResponse crear(Long balnearioId, UbicacionRequest request) {
        tenantFilterService.applyCurrentTenant();
        if (repository.findByBalnearioIdAndIdentificador(balnearioId, request.identificador()).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Ya existe una ubicación con ese identificador en este balneario");
        }
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setBalnearioId(balnearioId);
        ubicacion.setTipo(request.tipo());
        ubicacion.setIdentificador(request.identificador());
        ubicacion.setEstado(EstadoUbicacion.ACTIVA);
        return toResponse(repository.save(ubicacion));
    }

    @Transactional
    public UbicacionResponse actualizar(Long balnearioId, Long id, UbicacionRequest request) {
        tenantFilterService.applyCurrentTenant();
        Ubicacion ubicacion = obtenerPropia(balnearioId, id);
        repository.findByBalnearioIdAndIdentificador(balnearioId, request.identificador())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                            "Ya existe una ubicación con ese identificador en este balneario");
                });
        ubicacion.setTipo(request.tipo());
        ubicacion.setIdentificador(request.identificador());
        return toResponse(repository.save(ubicacion));
    }

    @Transactional(readOnly = true)
    public List<UbicacionResponse> listar(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return repository.findByBalnearioIdOrderByIdentificadorAsc(balnearioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UbicacionResponse cambiarEstado(Long balnearioId, Long id, EstadoUbicacion nuevoEstado) {
        tenantFilterService.applyCurrentTenant();
        Ubicacion ubicacion = obtenerPropia(balnearioId, id);
        // TODO(etapa 12): rechazar INACTIVA si la ubicación tiene una estadía ACTIVA.
        ubicacion.setEstado(nuevoEstado);
        return toResponse(repository.save(ubicacion));
    }

    @Transactional
    public void eliminar(Long balnearioId, Long id) {
        tenantFilterService.applyCurrentTenant();
        Ubicacion ubicacion = obtenerPropia(balnearioId, id);
        // TODO(etapa 12): rechazar si la ubicación tiene una estadía ACTIVA.
        ubicacion.setDeletedAt(Instant.now());
        repository.save(ubicacion);
    }

    private Ubicacion obtenerPropia(Long balnearioId, Long id) {
        Ubicacion ubicacion = repository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!ubicacion.getBalnearioId().equals(balnearioId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        return ubicacion;
    }

    private UbicacionResponse toResponse(Ubicacion ubicacion) {
        return new UbicacionResponse(ubicacion.getId(), ubicacion.getTipo().name(),
                ubicacion.getIdentificador(), ubicacion.getEstado().name());
    }
}
