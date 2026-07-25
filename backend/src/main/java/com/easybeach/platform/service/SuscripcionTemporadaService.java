package com.easybeach.platform.service;

import com.easybeach.platform.domain.EstadoSuscripcion;
import com.easybeach.platform.domain.Plan;
import com.easybeach.platform.domain.SuscripcionTemporada;
import com.easybeach.platform.domain.Temporada;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.platform.repository.PlanRepository;
import com.easybeach.platform.repository.SuscripcionTemporadaRepository;
import com.easybeach.platform.repository.TemporadaRepository;
import com.easybeach.platform.web.dto.SuscribirRequest;
import com.easybeach.platform.web.dto.SuscripcionResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-tenant intencional (Super Admin, sin {@code TenantFilterService}) -
 * es la única forma correcta de suscribir/administrar el balneario de
 * cualquier otro tenant (ADR-001 §"acceso cross-tenant solo Super Admin").
 */
@Service
public class SuscripcionTemporadaService {

    private static final Map<EstadoSuscripcion, Set<EstadoSuscripcion>> TRANSICIONES = Map.of(
            EstadoSuscripcion.PENDIENTE, Set.of(EstadoSuscripcion.ACTIVA),
            EstadoSuscripcion.ACTIVA, Set.of(EstadoSuscripcion.SUSPENDIDA, EstadoSuscripcion.FINALIZADA),
            EstadoSuscripcion.SUSPENDIDA, Set.of(EstadoSuscripcion.ACTIVA, EstadoSuscripcion.FINALIZADA),
            EstadoSuscripcion.FINALIZADA, Set.of()
    );

    private final SuscripcionTemporadaRepository repository;
    private final BalnearioRepository balnearioRepository;
    private final PlanRepository planRepository;
    private final TemporadaRepository temporadaRepository;
    private final AuditoriaPlataformaService auditoriaService;

    public SuscripcionTemporadaService(SuscripcionTemporadaRepository repository,
                                        BalnearioRepository balnearioRepository,
                                        PlanRepository planRepository,
                                        TemporadaRepository temporadaRepository,
                                        AuditoriaPlataformaService auditoriaService) {
        this.repository = repository;
        this.balnearioRepository = balnearioRepository;
        this.planRepository = planRepository;
        this.temporadaRepository = temporadaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public SuscripcionResponse suscribir(Long actorSuperAdminId, Long balnearioId, SuscribirRequest request) {
        if (!balnearioRepository.existsById(balnearioId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        if (repository.findByBalnearioIdAndTemporadaId(balnearioId, request.temporadaId()).isPresent()) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA, "El balneario ya tiene una suscripción para esa temporada");
        }
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        Temporada temporada = temporadaRepository.findById(request.temporadaId())
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));

        SuscripcionTemporada suscripcion = new SuscripcionTemporada();
        suscripcion.setBalnearioId(balnearioId);
        suscripcion.setPlan(plan);
        suscripcion.setTemporada(temporada);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion = repository.save(suscripcion);

        auditoriaService.registrar(actorSuperAdminId, "BALNEARIO_SUSCRITO", "suscripcion_temporada",
                suscripcion.getId(), balnearioId, Map.of("planId", plan.getId(), "temporadaId", temporada.getId()));

        return toResponse(suscripcion);
    }

    @Transactional
    public SuscripcionResponse cambiarEstado(Long actorSuperAdminId, Long balnearioId, Long suscripcionId,
                                              EstadoSuscripcion nuevoEstado, String motivo) {
        SuscripcionTemporada suscripcion = repository.findById(suscripcionId)
                .filter(s -> s.getBalnearioId().equals(balnearioId))
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!TRANSICIONES.get(suscripcion.getEstado()).contains(nuevoEstado)) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA,
                    "Transición inválida: " + suscripcion.getEstado() + " -> " + nuevoEstado);
        }
        suscripcion.setEstado(nuevoEstado);
        suscripcion = repository.save(suscripcion);
        auditoriaService.registrar(actorSuperAdminId, "SUSCRIPCION_" + nuevoEstado, "suscripcion_temporada",
                suscripcionId, balnearioId, Map.of("motivo", motivo == null ? "" : motivo));
        return toResponse(suscripcion);
    }

    @Transactional(readOnly = true)
    public List<SuscripcionResponse> listarPorBalneario(Long balnearioId) {
        return repository.findByBalnearioId(balnearioId).stream().map(this::toResponse).toList();
    }

    private SuscripcionResponse toResponse(SuscripcionTemporada s) {
        return new SuscripcionResponse(s.getId(), s.getBalnearioId(), s.getPlan().getId(), s.getTemporada().getId(),
                s.getEstado().name());
    }
}
