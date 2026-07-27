package com.easybeach.shared.time;

import java.time.ZoneId;

/**
 * TZ de negocio (etapa 02/04): {@code America/Argentina/Buenos_Aires}. Todo
 * timestamp se almacena en UTC (etapa 03 §1); esta zona se aplica solo en
 * consulta/reporte.
 *
 * <p>{@code OFFSET_SQL} es el offset fijo {@code -03:00} para usar en
 * {@code CONVERT_TZ(col, '+00:00', '-03:00')}: Argentina no tiene horario de
 * verano desde 2009, así que el offset es constante todo el año. Se usa el
 * offset numérico y no el nombre de la zona porque {@code CONVERT_TZ} con
 * nombres depende de que la imagen de MySQL tenga cargadas las tablas
 * {@code mysql.time_zone_name} (via {@code mysql_tzinfo_to_sql}) - muchas
 * imágenes no las traen por defecto, y con el nombre {@code CONVERT_TZ}
 * devuelve {@code NULL} en silencio en vez de fallar. El offset numérico no
 * tiene esa dependencia.
 */
public final class ZonaNegocio {

    public static final ZoneId ZONE_ID = ZoneId.of("America/Argentina/Buenos_Aires");
    public static final String OFFSET_SQL = "-03:00";

    private ZonaNegocio() {
    }
}
