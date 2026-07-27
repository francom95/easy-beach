package com.easybeach.reporting.repository;

import com.easybeach.reporting.dto.AperturasPorDiaResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lecturas de {@code estadia} vía JDBC plano (ver
 * {@link PedidoReportingRepository} para el porqué: {@code reporting} no
 * puede depender de {@code stay}, ADR-002).
 */
@Repository
public class EstadiaReportingRepository {

    private final JdbcTemplate jdbc;

    public EstadiaReportingRepository(JdbcTemplate jdbc) {
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

    /** Estadías solicitadas cada día, sin importar el desenlace posterior. */
    public List<AperturasPorDiaResponse> aperturasPorDia(Long balnearioId, Instant desde, Instant hasta) {
        return jdbc.query("""
                select date(convert_tz(fecha_solicitud, '+00:00', '-03:00')) as dia, count(*) as cantidad
                from estadia
                where balneario_id = ? and fecha_solicitud >= ? and fecha_solicitud < ?
                group by dia
                order by dia
                """, (rs, rowNum) -> new AperturasPorDiaResponse(rs.getDate("dia").toLocalDate(), rs.getLong("cantidad")),
                balnearioId, ts(desde), ts(hasta));
    }

    /**
     * Horas promedio entre validación y cierre, solo estadías que
     * efectivamente cerraron ({@code CERRADA}/{@code CERRADA_POR_SISTEMA})
     * dentro del rango (por {@code fecha_cierre}) - una estadía todavía
     * abierta no tiene duración final que promediar.
     */
    public Double duracionPromedioHoras(Long balnearioId, Instant desde, Instant hasta) {
        return jdbc.queryForObject("""
                select avg(timestampdiff(minute, fecha_validacion, fecha_cierre)) / 60.0
                from estadia
                where balneario_id = ? and fecha_cierre >= ? and fecha_cierre < ?
                  and fecha_validacion is not null and fecha_cierre is not null
                """, Double.class, balnearioId, ts(desde), ts(hasta));
    }
}
