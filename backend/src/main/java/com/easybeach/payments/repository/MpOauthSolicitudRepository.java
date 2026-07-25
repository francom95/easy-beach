package com.easybeach.payments.repository;

import com.easybeach.payments.domain.MpOauthSolicitud;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MpOauthSolicitudRepository extends JpaRepository<MpOauthSolicitud, Long> {

    /** ÚNICA excepción documentada: el callback público de MP no trae tenant, se busca solo por state. */
    Optional<MpOauthSolicitud> findByState(String state);
}
