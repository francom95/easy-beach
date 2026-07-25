package com.easybeach.branding.domain;

import com.easybeach.shared.audit.Auditable;
import com.easybeach.shared.tenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Theme white-label completo de un balneario (etapa 06 / contrato de tokens
 * en {@code docs/design/tokens.md}). {@code tokens} guarda el JSON resuelto
 * completo (personalizables + derivados) tal cual se sirve por API - la
 * lectura pública nunca recalcula nada en caliente.
 */
@Entity
@Table(name = "configuracion_visual")
@TenantScoped
@Filter(name = TenantScoped.FILTER_NAME, condition = "balneario_id = :" + TenantScoped.FILTER_PARAM)
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionVisual extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balneario_id", nullable = false)
    private Long balnearioId;

    @Column(name = "theme_version", nullable = false)
    private int themeVersion = 1;

    /**
     * JSON serializado a mano (Jackson, en el service) - se mapea como
     * String plano, sin tipo JDBC especial: MySQL acepta cualquier texto
     * JSON válido en una columna {@code JSON} vía bind estándar, y evita
     * depender de la conversión automática de Hibernate para un tipo (String
     * + columna JSON) cuyo comportamiento exacto no vale la pena arriesgar
     * sin poder verificarlo.
     */
    @Column(nullable = false, columnDefinition = "json")
    private String tokens;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "portada_url", length = 500)
    private String portadaUrl;

    @Column(name = "splash_url", length = 500)
    private String splashUrl;
}
