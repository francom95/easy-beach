package com.easybeach.reporting.web;

import com.easybeach.reporting.dto.PlataformaReporteResponse;
import com.easybeach.reporting.service.ReportePlataformaService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reporte de plataforma (etapa 15): el único que cruza datos entre
 * balnearios, y solo para Super Admin.
 */
@RestController
@RequestMapping("/api/v1/super-admin/reportes")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminReporteController {

    private final ReportePlataformaService service;

    public SuperAdminReporteController(ReportePlataformaService service) {
        this.service = service;
    }

    @GetMapping("/plataforma")
    public PlataformaReporteResponse plataforma() {
        return service.reporte();
    }

    @GetMapping("/plataforma/csv")
    public ResponseEntity<String> plataformaCsv() {
        PlataformaReporteResponse reporte = service.reporte();
        String csv = CsvWriter.build(List.of("balnearioId", "balnearioNombre", "cantidadPedidos", "facturacion"),
                reporte.volumenPorBalneario(),
                v -> List.of(v.balnearioId(), v.balnearioNombre(), v.cantidadPedidos(), v.facturacion()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plataforma.csv\"")
                .body(csv);
    }
}
