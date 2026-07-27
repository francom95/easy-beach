package com.easybeach.concierge.web;

import com.easybeach.concierge.domain.SolicitudServicio;
import com.easybeach.concierge.repository.TipoServicioRepository;
import com.easybeach.concierge.web.dto.SolicitudServicioResponse;
import com.easybeach.stay.repository.UbicacionRepository;
import org.springframework.stereotype.Component;

@Component
public class SolicitudServicioMapper {

    private final TipoServicioRepository tipoServicioRepository;
    private final UbicacionRepository ubicacionRepository;

    public SolicitudServicioMapper(TipoServicioRepository tipoServicioRepository,
                                    UbicacionRepository ubicacionRepository) {
        this.tipoServicioRepository = tipoServicioRepository;
        this.ubicacionRepository = ubicacionRepository;
    }

    public SolicitudServicioResponse toResponse(SolicitudServicio solicitud) {
        String tipoNombre = tipoServicioRepository.findById(solicitud.getTipoServicioId())
                .map(t -> t.getNombre()).orElse(null);
        String ubicacion = ubicacionRepository.findById(solicitud.getUbicacionId())
                .map(u -> u.getIdentificador()).orElse(null);
        return new SolicitudServicioResponse(solicitud.getPublicId(), tipoNombre, ubicacion,
                solicitud.getNota(), solicitud.getEstado().name(), solicitud.getCreatedAt());
    }
}
