package com.easybeach.concierge.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/** Solicitud de un cliente con estadía activa, ligada a su ubicación actual. */
@Entity
@Table(name = "solicitud_servicio")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class SolicitudServicio extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "CHAR(26)")
    private String publicId;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(name = "estadia_id", nullable = false)
    private Long estadiaId;

    /** Clave del canal SSE del cliente (ver comentario en la migración V009). */
    @Column(name = "cliente_public_id", nullable = false, columnDefinition = "CHAR(26)")
    private String clientePublicId;

    @Column(name = "ubicacion_id", nullable = false)
    private Long ubicacionId;

    @Column(name = "tipo_servicio_id", nullable = false)
    private Long tipoServicioId;

    @Column(length = 300)
    private String nota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoSolicitudServicio estado = EstadoSolicitudServicio.PENDIENTE;

    @Column(name = "atendida_por_usuario_id")
    private Long atendidaPorUsuarioId;

    @PrePersist
    void prePersist() {
        if (publicId == null) {
            publicId = UlidGenerator.generate();
        }
    }

    public void transicionarA(EstadoSolicitudServicio destino) {
        if (!estado.puedeTransicionarA(destino)) {
            throw new IllegalStateException("Transición inválida: " + estado + " -> " + destino);
        }
        this.estado = destino;
    }
}
