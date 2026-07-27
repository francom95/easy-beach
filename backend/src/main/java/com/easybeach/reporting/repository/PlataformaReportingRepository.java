package com.easybeach.reporting.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lecturas de {@code balneario}/{@code temporada}/{@code suscripcion_temporada}
 * vía JDBC plano, para el reporte de Super Admin - el único que cruza datos
 * entre balnearios por diseño (etapa 15 criterio de aceptación).
 */
@Repository
public class PlataformaReportingRepository {

    private final JdbcTemplate jdbc;

    public PlataformaReportingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** ACTIVO en la plataforma Y con suscripción ACTIVA vigente (etapa 03 §3.1: operativo efectivo). */
    public long balneariosActivos() {
        Long cantidad = jdbc.queryForObject("""
                select count(distinct b.id)
                from balneario b
                join suscripcion_temporada st on st.balneario_id = b.id
                join temporada t on t.id = st.temporada_id
                where b.estado = 'ACTIVO' and st.estado = 'ACTIVA' and t.estado = 'EN_CURSO'
                """, Long.class);
        return cantidad == null ? 0 : cantidad;
    }

    /** Rango de fechas de la temporada EN_CURSO, si existe. */
    public Optional<RangoTemporada> temporadaEnCurso() {
        return jdbc.query("select fecha_inicio, fecha_fin from temporada where estado = 'EN_CURSO' limit 1",
                (rs, rowNum) -> new RangoTemporada(rs.getDate("fecha_inicio").toLocalDate(),
                        rs.getDate("fecha_fin").toLocalDate()))
                .stream().findFirst();
    }

    public record RangoTemporada(LocalDate desde, LocalDate hasta) {
    }
}
