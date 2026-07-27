package com.easybeach.stay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Libera el cupo de las solicitudes de estadía que ningún carpero validó
 * dentro del TTL ({@link EstadiaService#TTL_VALIDACION}, 60 min). Sin esto,
 * una solicitud abandonada bloquearía al cliente para siempre en ese
 * balneario (por el UK de unicidad).
 *
 * <p>Corre cada 5 minutos: granularidad más que suficiente para un TTL de 60
 * minutos, y barato (una query indexada por estado+fecha).
 */
@Component
public class EstadiaExpiracionJob {

    private static final Logger log = LoggerFactory.getLogger(EstadiaExpiracionJob.class);

    private final EstadiaService estadiaService;

    public EstadiaExpiracionJob(EstadiaService estadiaService) {
        this.estadiaService = estadiaService;
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void expirarPendientes() {
        int expiradas = estadiaService.expirarPendientesVencidas();
        if (expiradas > 0) {
            log.info("Estadías PENDIENTE_VALIDACION expiradas por TTL: {}", expiradas);
        }
    }
}
