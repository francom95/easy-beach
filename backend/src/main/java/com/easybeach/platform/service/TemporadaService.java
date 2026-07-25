package com.easybeach.platform.service;

import com.easybeach.platform.domain.EstadoTemporada;
import com.easybeach.platform.domain.Temporada;
import com.easybeach.platform.repository.TemporadaRepository;
import com.easybeach.platform.web.dto.TemporadaRequest;
import com.easybeach.platform.web.dto.TemporadaResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemporadaService {

    private static final Map<EstadoTemporada, Set<EstadoTemporada>> TRANSICIONES = Map.of(
            EstadoTemporada.PLANIFICADA, Set.of(EstadoTemporada.EN_CURSO),
            EstadoTemporada.EN_CURSO, Set.of(EstadoTemporada.CERRADA),
            EstadoTemporada.CERRADA, Set.of()
    );

    private final TemporadaRepository repository;

    public TemporadaService(TemporadaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TemporadaResponse crear(TemporadaRequest request) {
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA, "fechaFin debe ser posterior a fechaInicio");
        }
        Temporada temporada = new Temporada();
        temporada.setNombre(request.nombre());
        temporada.setFechaInicio(request.fechaInicio());
        temporada.setFechaFin(request.fechaFin());
        return toResponse(repository.save(temporada));
    }

    @Transactional(readOnly = true)
    public List<TemporadaResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public TemporadaResponse cambiarEstado(Long id, EstadoTemporada nuevoEstado) {
        Temporada temporada = repository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!TRANSICIONES.get(temporada.getEstado()).contains(nuevoEstado)) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA,
                    "Transición inválida: " + temporada.getEstado() + " -> " + nuevoEstado);
        }
        temporada.setEstado(nuevoEstado);
        return toResponse(repository.save(temporada));
    }

    private TemporadaResponse toResponse(Temporada temporada) {
        return new TemporadaResponse(temporada.getId(), temporada.getNombre(), temporada.getFechaInicio(),
                temporada.getFechaFin(), temporada.getEstado().name());
    }
}
