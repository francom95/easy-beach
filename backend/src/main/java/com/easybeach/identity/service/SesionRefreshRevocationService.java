package com.easybeach.identity.service;

import com.easybeach.identity.domain.EstadoSesion;
import com.easybeach.identity.domain.SesionRefresh;
import com.easybeach.identity.repository.SesionRefreshRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aislado en su propio bean para que {@code REQUIRES_NEW} realmente tome
 * efecto: si viviera como método privado de {@link AuthService}, la
 * autoinvocación (llamar {@code this.metodo()} dentro de la misma clase)
 * evita el proxy de Spring y la anotación se ignora silenciosamente.
 *
 * <p>La revocación de la familia debe quedar COMMITEADA aunque
 * {@link AuthService#refresh} termine lanzando {@code ApiException}
 * (reuso detectado) - con la transacción por defecto de {@code refresh()},
 * esa excepción hace rollback de todo, incluida la revocación que
 * justamente queremos preservar.
 */
@Service
public class SesionRefreshRevocationService {

    private final SesionRefreshRepository sesionRefreshRepository;

    public SesionRefreshRevocationService(SesionRefreshRepository sesionRefreshRepository) {
        this.sesionRefreshRepository = sesionRefreshRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revocarFamilia(String familiaId) {
        List<SesionRefresh> activas = sesionRefreshRepository.findByFamiliaIdAndEstado(familiaId, EstadoSesion.ACTIVA);
        Instant now = Instant.now();
        for (SesionRefresh sesion : activas) {
            sesion.setEstado(EstadoSesion.REVOCADA);
            sesion.setUpdatedAt(now);
        }
        sesionRefreshRepository.saveAll(activas);
    }
}
