package com.easybeach.reporting.repository;

import com.easybeach.reporting.dto.SolicitudesPorTipoResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lecturas de {@code solicitud_servicio}/{@code tipo_servicio} vía JDBC
 * plano ({@code reporting} no puede depender de {@code concierge}, ADR-002).
 */
@Repository
public class ServicioReportingRepository {

    private final JdbcTemplate jdbc;

    public ServicioReportingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Literal UTC, no {@code Timestamp} - ver el Javadoc extenso en
     * {@code PedidoReportingRepository.ts}: un {@code Timestamp} bindeado
     * por JDBC plano usa la zona horaria de la JVM, no la UTC que Hibernate
     * fuerza vía {@code hibernate.jdbc.time_zone}.
     */
    private String ts(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC).format(instant);
    }

    public List<SolicitudesPorTipoResponse> solicitudesPorTipo(Long balnearioId, Instant desde, Instant hasta) {
        return jdbc.query("""
                select ts.nombre as nombre, count(*) as cantidad
                from solicitud_servicio ss
                join tipo_servicio ts on ts.id = ss.tipo_servicio_id
                where ss.balneario_id = ? and ss.created_at >= ? and ss.created_at < ?
                group by ts.id, ts.nombre
                order by cantidad desc
                """, (rs, rowNum) -> new SolicitudesPorTipoResponse(rs.getString("nombre"), rs.getLong("cantidad")),
                balnearioId, ts(desde), ts(hasta));
    }

    /**
     * Minutos promedio entre creación y última actualización de solicitudes
     * {@code RESUELTA} - no hay una columna {@code fecha_resuelta} propia,
     * así que {@code updated_at} es el proxy correcto: una vez resuelta, la
     * solicitud no vuelve a cambiar (etapa 03 §4.3, estado terminal).
     */
    public Double tiempoResolucionPromedioMinutos(Long balnearioId, Instant desde, Instant hasta) {
        return jdbc.queryForObject("""
                select avg(timestampdiff(minute, created_at, updated_at))
                from solicitud_servicio
                where balneario_id = ? and estado = 'RESUELTA' and created_at >= ? and created_at < ?
                """, Double.class, balnearioId, ts(desde), ts(hasta));
    }
}
