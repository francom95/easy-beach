package com.easybeach.stay.domain;

import com.easybeach.shared.audit.Auditable;
import com.easybeach.shared.id.UlidGenerator;
import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * El concepto de dominio distintivo de EasyBeach: el vínculo persistente
 * cliente-balneario-ubicación sobre el que cuelga todo el consumo. Sobrevive
 * días o toda la temporada hasta el cierre explícito del cliente.
 *
 * <p><b>{@code activaUk} no se toca a mano desde afuera</b>: lo mantiene
 * {@link #transicionarA} en sincronía con el estado. Es la columna sobre la
 * que descansa el UK {@code (balneario_id, activa_uk)} que hace la unicidad
 * "una estadía activa por cliente y balneario" imposible de violar por
 * concurrencia (etapa 03 §3.5).
 */
@Entity
@Table(name = "estadia")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class Estadia extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "CHAR(26)")
    private String publicId;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "ubicacion_id", nullable = false)
    private Long ubicacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EstadoEstadia estado = EstadoEstadia.PENDIENTE_VALIDACION;

    /** Ver Javadoc de la clase: gestionado por {@link #transicionarA}, no por setters externos. */
    @Column(name = "activa_uk")
    private Long activaUk;

    @Column(name = "fecha_solicitud", nullable = false)
    private Instant fechaSolicitud = Instant.now();

    @Column(name = "validada_por_usuario_id")
    private Long validadaPorUsuarioId;

    @Column(name = "fecha_validacion")
    private Instant fechaValidacion;

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    @PrePersist
    void prePersist() {
        if (publicId == null) {
            publicId = UlidGenerator.generate();
        }
        // Al nacer (PENDIENTE_VALIDACION) la estadía ya ocupa cupo.
        if (estado.ocupaCupo()) {
            activaUk = clienteId;
        }
    }

    /**
     * Única vía para cambiar de estado. Mantiene {@code activaUk} coherente:
     * lo libera ({@code null}) al entrar a un estado terminal, de modo que el
     * cliente pueda volver a solicitar una estadía en ese balneario.
     *
     * @throws IllegalStateException si la transición no es válida - el service
     *         valida antes con {@link EstadoEstadia#puedeTransicionarA} y
     *         traduce a un error de API; esto es la red de seguridad.
     */
    public void transicionarA(EstadoEstadia nuevoEstado) {
        if (!estado.puedeTransicionarA(nuevoEstado)) {
            throw new IllegalStateException("Transición inválida: " + estado + " -> " + nuevoEstado);
        }
        this.estado = nuevoEstado;
        this.activaUk = nuevoEstado.ocupaCupo() ? clienteId : null;
    }
}
