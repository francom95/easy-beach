package com.easybeach.reporting.web;

import com.easybeach.reporting.dto.DashboardResumenResponse;
import com.easybeach.reporting.dto.EstadiasReporteResponse;
import com.easybeach.reporting.dto.ProductoVendidoResponse;
import com.easybeach.reporting.dto.PromocionRendimientoResponse;
import com.easybeach.reporting.dto.ServiciosReporteResponse;
import com.easybeach.reporting.dto.VentasReporteResponse;
import com.easybeach.reporting.service.ReporteBalnearioService;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes del balneario (etapa 15). Todos con filtro {@code desde}/{@code
 * hasta} (fechas de negocio, TZ Argentina - ver {@code RangoFechasUtil}),
 * salvo el dashboard, que siempre es "hoy". Cada reporte tabular tiene su
 * variante {@code /csv}.
 */
@RestController
@RequestMapping("/api/v1/admin/reportes")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminReporteController {

    private final ReporteBalnearioService service;

    public AdminReporteController(ReporteBalnearioService service) {
        this.service = service;
    }

    @GetMapping("/ventas")
    public VentasReporteResponse ventas(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                         @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.ventas(principal.balnearioId(), desde, hasta);
    }

    @GetMapping("/ventas/csv")
    public ResponseEntity<String> ventasCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        var reporte = service.ventas(principal.balnearioId(), desde, hasta);
        String csv = CsvWriter.build(List.of("dia", "cantidadPedidos", "facturacion"), reporte.porDia(),
                d -> List.of(d.dia(), d.cantidadPedidos(), d.facturacion()));
        return csvResponse(csv, "ventas.csv");
    }

    @GetMapping("/productos-mas-vendidos")
    public List<ProductoVendidoResponse> productosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite,
            @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.productosMasVendidos(principal.balnearioId(), desde, hasta, limite);
    }

    @GetMapping("/productos-mas-vendidos/csv")
    public ResponseEntity<String> productosMasVendidosCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite,
            @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        var productos = service.productosMasVendidos(principal.balnearioId(), desde, hasta, limite);
        String csv = CsvWriter.build(List.of("productoId", "nombreProducto", "unidadesVendidas", "facturacion"),
                productos, p -> List.of(p.productoId(), p.nombreProducto(), p.unidadesVendidas(), p.facturacion()));
        return csvResponse(csv, "productos-mas-vendidos.csv");
    }

    @GetMapping("/promociones")
    public List<PromocionRendimientoResponse> promociones(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.rendimientoPromociones(principal.balnearioId(), desde, hasta);
    }

    @GetMapping("/promociones/csv")
    public ResponseEntity<String> promocionesCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        var promociones = service.rendimientoPromociones(principal.balnearioId(), desde, hasta);
        String csv = CsvWriter.build(List.of("promocionId", "nombrePromocion", "usos", "montoDescontado"),
                promociones, p -> List.of(p.promocionId(), p.nombrePromocion(), p.usos(), p.montoDescontado()));
        return csvResponse(csv, "promociones.csv");
    }

    @GetMapping("/estadias")
    public EstadiasReporteResponse estadias(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.estadias(principal.balnearioId(), desde, hasta);
    }

    /** Exporta solo el desglose por día; duración/consumo promedio son KPIs escalares (solo JSON). */
    @GetMapping("/estadias/csv")
    public ResponseEntity<String> estadiasCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                               @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        var reporte = service.estadias(principal.balnearioId(), desde, hasta);
        String csv = CsvWriter.build(List.of("dia", "cantidad"), reporte.aperturasPorDia(),
                a -> List.of(a.dia(), a.cantidad()));
        return csvResponse(csv, "estadias.csv");
    }

    @GetMapping("/servicios")
    public ServiciosReporteResponse servicios(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                               @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.servicios(principal.balnearioId(), desde, hasta);
    }

    @GetMapping("/servicios/csv")
    public ResponseEntity<String> serviciosCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        var reporte = service.servicios(principal.balnearioId(), desde, hasta);
        String csv = CsvWriter.build(List.of("tipoServicio", "cantidad"), reporte.porTipo(),
                s -> List.of(s.tipoServicio(), s.cantidad()));
        return csvResponse(csv, "servicios.csv");
    }

    @GetMapping("/dashboard")
    public DashboardResumenResponse dashboard(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.dashboard(principal.balnearioId());
    }

    private ResponseEntity<String> csvResponse(String csv, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(csv);
    }
}
