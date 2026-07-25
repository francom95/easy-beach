package com.easybeach.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Refresh token opaco (etapa 05 §1.1): se persiste el hash, no el valor.
 * Agrupado por {@code familiaId} para poder revocar toda la cadena de
 * rotación ante un reuso detectado (posible robo).
 */
@Entity
@Table(name = "sesion_refresh")
@Getter
@Setter
@NoArgsConstructor
public class SesionRefresh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "familia_id", nullable = false, columnDefinition = "CHAR(26)")
    private String familiaId;

    @Column(name = "token_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoSesion estado = EstadoSesion.ACTIVA;

    @Column(name = "expira_at", nullable = false)
    private Instant expiraAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
