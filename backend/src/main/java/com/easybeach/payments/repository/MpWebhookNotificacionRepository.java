package com.easybeach.payments.repository;

import com.easybeach.payments.domain.MpWebhookNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MpWebhookNotificacionRepository extends JpaRepository<MpWebhookNotificacion, Long> {

    boolean existsByMpPaymentIdAndTipoAndPayloadHash(String mpPaymentId, String tipo, String payloadHash);
}
