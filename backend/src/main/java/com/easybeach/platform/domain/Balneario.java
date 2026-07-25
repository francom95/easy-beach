package com.easybeach.platform.domain;

import com.easybeach.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * El tenant. NO es {@code @TenantScoped} (etapa 03 §3.1: "no es
 * tenant-scoped") - es la entidad que el resto del sistema referencia por
 * {@code balneario_id}. Su ABM completo (branding, planes, temporadas)
 * pertenece a la etapa 10; acá solo lo necesario para resolver el tenant.
 */
@Entity
@Table(name = "balneario")
@Getter
@Setter
@NoArgsConstructor
public class Balneario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String slug;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "email_contacto", nullable = false, length = 160)
    private String emailContacto;

    @Column(length = 40)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoBalneario estado = EstadoBalneario.ACTIVO;
}
