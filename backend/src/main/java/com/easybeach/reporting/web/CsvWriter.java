package com.easybeach.reporting.web;

import java.util.List;
import java.util.function.Function;

/**
 * CSV mínimo y correcto (etapa 15, criterio "export CSV de cada reporte"):
 * sin librería externa, escapando comillas y envolviendo en comillas
 * cualquier campo con coma, comilla o salto de línea (RFC 4180).
 */
final class CsvWriter {

    private CsvWriter() {
    }

    static <T> String build(List<String> encabezados, List<T> filas, Function<T, List<Object>> fila) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", encabezados.stream().map(CsvWriter::escapar).toList())).append("\r\n");
        for (T item : filas) {
            List<Object> valores = fila.apply(item);
            sb.append(String.join(",", valores.stream().map(v -> escapar(v == null ? "" : v.toString())).toList()))
                    .append("\r\n");
        }
        return sb.toString();
    }

    private static String escapar(String valor) {
        boolean necesitaComillas = valor.contains(",") || valor.contains("\"") || valor.contains("\n")
                || valor.contains("\r");
        String escapado = valor.replace("\"", "\"\"");
        return necesitaComillas ? "\"" + escapado + "\"" : escapado;
    }
}
