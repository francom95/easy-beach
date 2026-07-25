package com.easybeach.identity.domain;

import com.easybeach.shared.audit.Auditable;
import com.easybeach.shared.id.UlidGenerator;
import com.easybeach.shared.security.TipoUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario global de la plataforma: puede ser cliente (registro libre),
 * staff (creado por el admin del balneario) o super admin. NO es
 * tenant-scoped - el vínculo con un balneario específico (para staff) vive
 * en {@link UsuarioBalnearioRol}.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "CHAR(26)")
    private String publicId;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoUsuario tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    /** Etapa 05 §1.1: obligatorio tras alta con password temporal (etapa 10). */
    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = false;

    @PrePersist
    void prePersist() {
        if (publicId == null) {
            publicId = UlidGenerator.generate();
        }
    }
}
