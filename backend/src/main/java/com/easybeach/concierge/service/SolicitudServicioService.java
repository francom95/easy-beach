package com.easybeach.concierge.service;

import com.easybeach.concierge.domain.EstadoSolicitudServicio;
import com.easybeach.concierge.domain.SolicitudServicio;
import com.easybeach.concierge.domain.TipoServicio;
import com.easybeach.concierge.repository.SolicitudServicioRepository;
import com.easybeach.concierge.repository.TipoServicioRepository;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.realtime.TiempoRealService;
import com.easybeach.shared.tenancy.TenantContext;
import com.easybeach.shared.tenancy.TenantFilterService;
import com.easybeach.stay.domain.Estadia;
import com.easybeach.stay.domain.EstadoEstadia;
import com.easybeach.stay.repository.EstadiaRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo simple de solicitud de servicio (etapa 14): sin dinero de por medio,
 * misma mecánica de tiempo real que los pedidos (etapa 13).
 */
@Service
public class SolicitudServicioService {

    private final SolicitudServicioRepository repository;
    private final TipoServicioRepository tipoServicioRepository;
    private final EstadiaRepository estadiaRepository;
    private final TiempoRealService tiempoRealService;
    private final TenantFilterService tenantFilterService;

    public SolicitudServicioService(SolicitudServicioRepository repository,
                                     TipoServicioRepository tipoServicioRepository,
                                     EstadiaRepository estadiaRepository,
                                     TiempoRealService tiempoRealService,
                                     TenantFilterService tenantFilterService) {
        this.repository = repository;
        this.tipoServicioRepository = tipoServicioRepository;
        this.estadiaRepository = estadiaRepository;
        this.tiempoRealService = tiempoRealService;
        this.tenantFilterService = tenantFilterService;
    }

    @Transactional
    public SolicitudServicio solicitar(Long clienteId, String clientePublicId, String estadiaPublicId,
                                        Long tipoServicioId, String nota) {
        Estadia estadia = estadiaRepository.findByPublicId(estadiaPublicId)
                .filter(e -> e.getClienteId().equals(clienteId))
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (estadia.getEstado() != EstadoEstadia.ACTIVA) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "La estadía debe estar ACTIVA para solicitar un servicio");
        }

        TenantContext.set(estadia.getBalnearioId());
        tenantFilterService.applyCurrentTenant();

        TipoServicio tipo = tipoServicioRepository.findById(tipoServicioId)
                .filter(t -> t.getBalnearioId().equals(estadia.getBalnearioId()) && t.isActivo())
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO,
                        "Tipo de servicio inexistente o inactivo"));

        SolicitudServicio solicitud = new SolicitudServicio();
        solicitud.setBalnearioId(estadia.getBalnearioId());
        solicitud.setEstadiaId(estadia.getId());
        solicitud.setClientePublicId(clientePublicId);
        solicitud.setUbicacionId(estadia.getUbicacionId());
        solicitud.setTipoServicioId(tipo.getId());
        solicitud.setNota(nota);
        solicitud.setEstado(EstadoSolicitudServicio.PENDIENTE);
        solicitud = repository.save(solicitud);

        tiempoRealService.emitirAOperativo(estadia.getBalnearioId(), "servicio.nuevo",
                Map.of("solicitudPublicId", solicitud.getPublicId(), "tipoServicio", tipo.getNombre()));
        return solicitud;
    }

    /** Cola del carpero: activas, por antigüedad. */
    @Transactional(readOnly = true)
    public List<SolicitudServicio> colaOperativa(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return repository.findByBalnearioIdAndEstadoInOrderByCreatedAtAsc(balnearioId,
                List.of(EstadoSolicitudServicio.PENDIENTE, EstadoSolicitudServicio.EN_CURSO));
    }

    @Transactional
    public SolicitudServicio transicionar(Long balnearioId, Long carperoUsuarioId, String publicId,
                                           EstadoSolicitudServicio destino) {
        SolicitudServicio solicitud = repository.findByPublicId(publicId)
                .filter(s -> s.getBalnearioId().equals(balnearioId))
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!solicitud.getEstado().puedeTransicionarA(destino)) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Transición inválida: " + solicitud.getEstado() + " -> " + destino);
        }
        solicitud.transicionarA(destino);
        solicitud.setAtendidaPorUsuarioId(carperoUsuarioId);
        solicitud = repository.save(solicitud);
        notificarCambioDeEstado(solicitud);
        return solicitud;
    }

    @Transactional
    public SolicitudServicio cancelarPorCliente(Long clienteId, String publicId) {
        SolicitudServicio solicitud = repository.findByPublicId(publicId).orElseThrow(
                () -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        Estadia estadia = estadiaRepository.findById(solicitud.getEstadiaId()).orElseThrow();
        if (!estadia.getClienteId().equals(clienteId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        if (!solicitud.getEstado().puedeTransicionarA(EstadoSolicitudServicio.CANCELADA)) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "Ya no podés cancelar esta solicitud (estado: " + solicitud.getEstado() + ")");
        }
        solicitud.transicionarA(EstadoSolicitudServicio.CANCELADA);
        solicitud = repository.save(solicitud);
        notificarCambioDeEstado(solicitud);
        return solicitud;
    }

    private void notificarCambioDeEstado(SolicitudServicio solicitud) {
        Map<String, Object> payload = Map.of("solicitudPublicId", solicitud.getPublicId(),
                "estado", solicitud.getEstado().name());
        tiempoRealService.emitirAOperativo(solicitud.getBalnearioId(), "servicio.estado", payload);
        tiempoRealService.emitirACliente(solicitud.getClientePublicId(), "servicio.estado", payload);
    }

    @Transactional(readOnly = true)
    public List<SolicitudServicio> deEstadia(Long clienteId, String estadiaPublicId) {
        Estadia estadia = estadiaRepository.findByPublicId(estadiaPublicId)
                .filter(e -> e.getClienteId().equals(clienteId))
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        return repository.findByEstadiaIdOrderByCreatedAtDesc(estadia.getId());
    }
}
