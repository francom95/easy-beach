package com.easybeach.reporting.repository;

import com.easybeach.reporting.dto.PromocionRendimientoResponse;
import com.easybeach.reporting.dto.ProductoVendidoResponse;
import com.easybeach.reporting.dto.VentasPorDiaResponse;
import com.easybeach.reporting.dto.VolumenPorBalnearioResponse;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lecturas de {@code pedido}/{@code pedido_item}/{@code pedido_promocion}
 * vía JDBC plano - <b>no</b> reutiliza los repositorios JPA de
 * {@code ordering} (ADR-002: {@code reporting} no depende de ningún módulo
 * de negocio, solo de {@code shared}). Es el mismo diseño de "read model
 * propio" que anticipaba el {@code package-info} del módulo.
 *
 * <p>Cada query filtra explícitamente por {@code balneario_id} - acá no hay
 * {@code @Filter} de Hibernate que lo haga por vos. Esa es la razón de ser
 * de {@code ReportingCrossTenantIntegrationTest}.
 *
 * <p>Todas las consultas de facturación cuentan <b>solo</b> pedidos
 * {@code ENTREGADO} (etapa 15 criterio de aceptación: "los montos cuadran
 * con los pedidos entregados, excluyen cancelados").
 */
@Repository
public class PedidoReportingRepository {

    private final JdbcTemplate jdbc;

    public PedidoReportingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * {@code BIGINT UNSIGNED} vía JDBC puede volver como {@code BigInteger}
     * con {@code getObject()} según el driver - se lee siempre con
     * {@code getLong()} (soporta el rango real de ids) y se chequea
     * {@code wasNull()} para las columnas nullable (referencias blandas).
     */
    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long valor = rs.getLong(column);
        return rs.wasNull() ? null : valor;
    }

    /**
     * Formatea el instante como literal UTC ("yyyy-MM-dd HH:mm:ss.SSS"), NO
     * como {@code java.sql.Timestamp}. Bug real encontrado al verificar esta
     * etapa: la app fija {@code hibernate.jdbc.time_zone: UTC}
     * (application.yml) para que Hibernate escriba {@code Instant} como UTC
     * verdadero, pero esa property es exclusiva de Hibernate - un
     * {@code JdbcTemplate} crudo, sin pasar por el ORM, convierte un
     * {@code Timestamp} usando la zona horaria <b>por defecto de la JVM</b>
     * (acá, {@code America/Buenos_Aires}, UTC-3), no UTC. El resultado: cada
     * comparación de fecha en los reportes quedaba corrida 3 horas contra lo
     * que Hibernate realmente escribió, y las queries devolvían listas
     * vacías sin ningún error. Un literal de texto en UTC no pasa por
     * ninguna conversión de zona horaria del driver - se manda tal cual.
     */
    private String ts(Instant instant) {
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(java.time.ZoneOffset.UTC)
                .format(instant);
    }

    public BigDecimal facturacionTotal(Long balnearioId, Instant desde, Instant hasta) {
        BigDecimal total = jdbc.queryForObject("""
                select coalesce(sum(total), 0) from pedido
                where balneario_id = ? and estado = 'ENTREGADO' and created_at >= ? and created_at < ?
                """, BigDecimal.class, balnearioId, ts(desde), ts(hasta));
        return total == null ? BigDecimal.ZERO : total;
    }

    public long cantidadPedidosEntregados(Long balnearioId, Instant desde, Instant hasta) {
        Long cantidad = jdbc.queryForObject("""
                select count(*) from pedido
                where balneario_id = ? and estado = 'ENTREGADO' and created_at >= ? and created_at < ?
                """, Long.class, balnearioId, ts(desde), ts(hasta));
        return cantidad == null ? 0 : cantidad;
    }

    /** {@code dia} ya en TZ de negocio (offset fijo -03:00, ver {@code ZonaNegocio}). */
    public List<VentasPorDiaResponse> ventasPorDia(Long balnearioId, Instant desde, Instant hasta) {
        return jdbc.query("""
                select date(convert_tz(created_at, '+00:00', '-03:00')) as dia,
                       count(*) as cantidad, sum(total) as facturacion
                from pedido
                where balneario_id = ? and estado = 'ENTREGADO' and created_at >= ? and created_at < ?
                group by dia
                order by dia
                """, (rs, rowNum) -> new VentasPorDiaResponse(
                        rs.getDate("dia").toLocalDate(), rs.getLong("cantidad"), rs.getBigDecimal("facturacion")),
                balnearioId, ts(desde), ts(hasta));
    }

    public List<ProductoVendidoResponse> productosMasVendidos(Long balnearioId, Instant desde, Instant hasta, int limite) {
        return jdbc.query("""
                select pi.producto_id as producto_id, pi.nombre_producto as nombre_producto,
                       sum(pi.cantidad) as unidades, sum(pi.subtotal_linea) as facturacion
                from pedido_item pi
                join pedido p on p.id = pi.pedido_id
                where pi.balneario_id = ? and p.estado = 'ENTREGADO' and p.created_at >= ? and p.created_at < ?
                group by pi.producto_id, pi.nombre_producto
                order by unidades desc
                limit ?
                """, (rs, rowNum) -> new ProductoVendidoResponse(
                        nullableLong(rs, "producto_id"), rs.getString("nombre_producto"),
                        rs.getLong("unidades"), rs.getBigDecimal("facturacion")),
                balnearioId, ts(desde), ts(hasta), limite);
    }

    public List<PromocionRendimientoResponse> rendimientoPromociones(Long balnearioId, Instant desde, Instant hasta) {
        return jdbc.query("""
                select pp.promocion_id as promocion_id, pp.nombre_promocion as nombre_promocion,
                       count(*) as usos, sum(pp.monto_descuento) as descuento
                from pedido_promocion pp
                join pedido p on p.id = pp.pedido_id
                where pp.balneario_id = ? and p.estado = 'ENTREGADO' and p.created_at >= ? and p.created_at < ?
                group by pp.promocion_id, pp.nombre_promocion
                order by descuento desc
                """, (rs, rowNum) -> new PromocionRendimientoResponse(
                        nullableLong(rs, "promocion_id"), rs.getString("nombre_promocion"),
                        rs.getLong("usos"), rs.getBigDecimal("descuento")),
                balnearioId, ts(desde), ts(hasta));
    }

    /** Foto del presente (dashboard): pedidos que YA entraron a la cola operativa y no terminaron. */
    public long pedidosEnCurso(Long balnearioId) {
        Long cantidad = jdbc.queryForObject("""
                select count(*) from pedido
                where balneario_id = ? and estado in ('CONFIRMADO', 'EN_PREPARACION', 'EN_CAMINO')
                """, Long.class, balnearioId);
        return cantidad == null ? 0 : cantidad;
    }

    /** Consumo entregado de una lista de estadías (para el promedio del reporte de estadías). */
    public BigDecimal consumoPromedioPorEstadia(Long balnearioId, Instant estadiaDesde, Instant estadiaHasta) {
        BigDecimal promedio = jdbc.queryForObject("""
                select avg(consumo) from (
                    select e.id, coalesce(sum(p.total), 0) as consumo
                    from estadia e
                    left join pedido p on p.estadia_id = e.id and p.estado = 'ENTREGADO'
                    where e.balneario_id = ? and e.fecha_cierre >= ? and e.fecha_cierre < ?
                    group by e.id
                ) por_estadia
                """, BigDecimal.class, balnearioId, ts(estadiaDesde), ts(estadiaHasta));
        return promedio == null ? BigDecimal.ZERO : promedio;
    }

    /** Cantidad de pedidos + facturación (ENTREGADO) por balneario, cruzando tenants - Super Admin. */
    public List<VolumenPorBalnearioResponse> volumenPorBalneario(Instant desde, Instant hasta) {
        return jdbc.query("""
                select b.id as balneario_id, b.nombre as balneario_nombre,
                       count(p.id) as cantidad, coalesce(sum(p.total), 0) as facturacion
                from balneario b
                left join pedido p on p.balneario_id = b.id and p.estado = 'ENTREGADO'
                                    and p.created_at >= ? and p.created_at < ?
                group by b.id, b.nombre
                order by facturacion desc
                """, (rs, rowNum) -> new VolumenPorBalnearioResponse(
                        rs.getLong("balneario_id"), rs.getString("balneario_nombre"),
                        rs.getLong("cantidad"), rs.getBigDecimal("facturacion")),
                ts(desde), ts(hasta));
    }
}
