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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BalnearioMpCredencialService.class);

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

    /**
     * Credenciales vinculadas cuyo access token vence dentro del margen.
     * Cross-tenant a propósito (lo corre {@code MpTokenRefreshJob}), por eso no
     * aplica el filtro de tenant. Devuelve ids: cada refresh va en su propia
     * transacción para que la falla de un balneario no arrastre al resto.
     */
    @Transactional(readOnly = true)
    public List<Long> idsDeCredencialesPorVencer(Instant limite) {
        return credencialRepository
                .findByEstadoAndTokenExpiraAtBefore(EstadoCredencialMp.VINCULADA, limite)
                .stream()
                .map(BalnearioMpCredencial::getId)
                .toList();
    }

    /**
     * Renueva el access token de MP con el refresh token guardado (ADR-004:
     * "refresh anticipado por job"). Sin esto, el token vence y el balneario
     * deja de poder cobrar en silencio.
     *
     * <p>Si el refresh falla y el token <b>ya venció</b>, la credencial se marca
     * {@link EstadoCredencialMp#EXPIRADA}: es un estado terminal que el panel de
     * cobros muestra, y le corta el paso a nuevos pedidos
     * ({@link #puedeRecibirPagos}) en vez de dejarlos fallar en el cobro. Si el
     * token todavía es válido, la falla se considera transitoria y se reintenta
     * en el próximo ciclo.
     *
     * <p>No propaga la excepción de MP a propósito: marcar EXPIRADA y después
     * relanzar haría rollback de ese mismo cambio (la RuntimeException marca la
     * transacción para revertir). El desenlace se devuelve como valor y el job
     * lo loguea.
     */
    @Transactional
    public ResultadoRefresh refrescarCredencial(Long credencialId) {
        BalnearioMpCredencial credencial = credencialRepository.findById(credencialId).orElse(null);
        if (credencial == null || credencial.getEstado() != EstadoCredencialMp.VINCULADA) {
            return ResultadoRefresh.OMITIDA;
        }

        String refreshToken = tokenEncryptionService.decrypt(credencial.getRefreshTokenCifrado());
        try {
            MercadoPagoOAuthClient.TokenResponse tokens = oAuthClient.refreshToken(refreshToken);
            credencial.setAccessTokenCifrado(tokenEncryptionService.encrypt(tokens.accessToken()));
            credencial.setRefreshTokenCifrado(tokenEncryptionService.encrypt(tokens.refreshToken()));
            credencial.setTokenExpiraAt(Instant.now().plusSeconds(tokens.expiresInSeconds()));
            if (tokens.scope() != null) {
                credencial.setScope(tokens.scope());
            }
            credencialRepository.save(credencial);
            return ResultadoRefresh.RENOVADA;
        } catch (RuntimeException e) {
            log.error("Falló el refresh del token de Mercado Pago del balneario {}",
                    credencial.getBalnearioId(), e);
            boolean yaVencio = credencial.getTokenExpiraAt() != null
                    && credencial.getTokenExpiraAt().isBefore(Instant.now());
            if (!yaVencio) {
                // Todavía hay margen: se asume transitorio y se reintenta.
                return ResultadoRefresh.FALLO_TRANSITORIO;
            }
            credencial.setEstado(EstadoCredencialMp.EXPIRADA);
            credencialRepository.save(credencial);
            return ResultadoRefresh.EXPIRADA;
        }
    }

    /** Desenlace de intentar renovar el token de MP de un balneario. */
    public enum ResultadoRefresh {
        /** La credencial ya no estaba vinculada al momento de procesarla. */
        OMITIDA,
        /** Token renovado. */
        RENOVADA,
        /** MP falló pero el token sigue vigente: se reintenta el próximo ciclo. */
        FALLO_TRANSITORIO,
        /** MP falló y el token ya venció: el balneario deja de poder cobrar. */
        EXPIRADA
    }

    private String generarState() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
