package com.easybeach.payments.service;

import com.easybeach.payments.MercadoPagoOAuthClient;
import com.easybeach.payments.TokenEncryptionService;
import com.easybeach.payments.domain.BalnearioMpCredencial;
import com.easybeach.payments.domain.EstadoCredencialMp;
import com.easybeach.payments.domain.MpOauthSolicitud;
import com.easybeach.payments.repository.BalnearioMpCredencialRepository;
import com.easybeach.payments.repository.MpOauthSolicitudRepository;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.tenancy.TenantFilterService;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vinculación OAuth de la cuenta de Mercado Pago del balneario (etapa 10 /
 * ADR-004). "Un balneario sin cuenta MP vinculada no puede recibir pedidos
 * pagos" (criterio de aceptación) - {@link #puedeRecibirPagos(Long)} es el
 * hook que la etapa 13 (creación de pago) va a exigir antes de cobrar.
 */
@Service
public class BalnearioMpCredencialService {

    private static final Duration TTL_SOLICITUD = Duration.ofMinutes(10);

    private final BalnearioMpCredencialRepository credencialRepository;
    private final MpOauthSolicitudRepository solicitudRepository;
    private final MercadoPagoOAuthClient oAuthClient;
    private final TokenEncryptionService tokenEncryptionService;
    private final TenantFilterService tenantFilterService;
    private final SecureRandom random = new SecureRandom();

    public BalnearioMpCredencialService(BalnearioMpCredencialRepository credencialRepository,
                                         MpOauthSolicitudRepository solicitudRepository,
                                         MercadoPagoOAuthClient oAuthClient,
                                         TokenEncryptionService tokenEncryptionService,
                                         TenantFilterService tenantFilterService) {
        this.credencialRepository = credencialRepository;
        this.solicitudRepository = solicitudRepository;
        this.oAuthClient = oAuthClient;
        this.tokenEncryptionService = tokenEncryptionService;
        this.tenantFilterService = tenantFilterService;
    }

    public record EstadoVinculacion(boolean vinculado, String mpUserId, EstadoCredencialMp estado) {
    }

    @Transactional
    public String iniciarVinculacion(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        String state = generarState();
        MpOauthSolicitud solicitud = new MpOauthSolicitud();
        solicitud.setBalnearioId(balnearioId);
        solicitud.setState(state);
        solicitud.setExpiraAt(Instant.now().plus(TTL_SOLICITUD));
        solicitudRepository.save(solicitud);
        return oAuthClient.buildAuthorizationUrl(state);
    }

    /**
     * Callback público de Mercado Pago: SIN TenantContext (no hay token de
     * ningún balneario en este request), el tenant se descubre a partir del
     * {@code state} - excepción documentada, igual que el login de staff.
     */
    @Transactional
    public void manejarCallback(String state, String authorizationCode) {
        MpOauthSolicitud solicitud = solicitudRepository.findByState(state)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDACION_FALLIDA, "Solicitud de vinculación inválida"));
        if (solicitud.isUsado() || solicitud.getExpiraAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA, "La solicitud de vinculación expiró o ya fue usada");
        }
        solicitud.setUsado(true);
        solicitudRepository.save(solicitud);

        MercadoPagoOAuthClient.TokenResponse tokens = oAuthClient.exchangeCode(authorizationCode);

        BalnearioMpCredencial credencial = credencialRepository.findByBalnearioId(solicitud.getBalnearioId())
                .orElseGet(BalnearioMpCredencial::new);
        credencial.setBalnearioId(solicitud.getBalnearioId());
        credencial.setMpUserId(tokens.mpUserId());
        credencial.setAccessTokenCifrado(tokenEncryptionService.encrypt(tokens.accessToken()));
        credencial.setRefreshTokenCifrado(tokenEncryptionService.encrypt(tokens.refreshToken()));
        credencial.setTokenExpiraAt(Instant.now().plusSeconds(tokens.expiresInSeconds()));
        credencial.setScope(tokens.scope());
        credencial.setEstado(EstadoCredencialMp.VINCULADA);
        credencialRepository.save(credencial);
    }

    @Transactional(readOnly = true)
    public EstadoVinculacion obtenerEstado(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return credencialRepository.findByBalnearioId(balnearioId)
                .map(c -> new EstadoVinculacion(c.getEstado() == EstadoCredencialMp.VINCULADA, c.getMpUserId(), c.getEstado()))
                .orElse(new EstadoVinculacion(false, null, EstadoCredencialMp.DESVINCULADA));
    }

    @Transactional
    public void desvincular(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        credencialRepository.findByBalnearioId(balnearioId).ifPresent(credencialRepository::delete);
    }

    /** Hook para la etapa 13: sin esto, el pedido no puede pasar a PAGO_PENDIENTE. */
    @Transactional(readOnly = true)
    public boolean puedeRecibirPagos(Long balnearioId) {
        return credencialRepository.existsByBalnearioIdAndEstado(balnearioId, EstadoCredencialMp.VINCULADA);
    }

    private String generarState() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
