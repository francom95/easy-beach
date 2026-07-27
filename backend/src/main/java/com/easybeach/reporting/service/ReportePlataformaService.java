package com.easybeach.reporting.service;

import com.easybeach.reporting.dto.PlataformaReporteResponse;
import com.easybeach.reporting.dto.VolumenPorBalnearioResponse;
import com.easybeach.reporting.repository.PedidoReportingRepository;
import com.easybeach.reporting.repository.PlataformaReportingRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Reporte de Super Admin (etapa 15): el único que cruza datos entre
 * balnearios, por diseño explícito del criterio de aceptación.
 */
@Service
public class ReportePlataformaService {

    private final PlataformaReportingRepository plataformaRepo;
    private final PedidoReportingRepository pedidoRepo;

    public ReportePlataformaService(PlataformaReportingRepository plataformaRepo, PedidoReportingRepository pedidoRepo) {
        this.plataformaRepo = plataformaRepo;
        this.pedidoRepo = pedidoRepo;
    }

    public PlataformaReporteResponse reporte() {
        long balneariosActivos = plataformaRepo.balneariosActivos();
        var temporada = plataformaRepo.temporadaEnCurso();
        if (temporada.isEmpty()) {
            // Sin temporada EN_CURSO no hay ventana de volumen que reportar;
            // no es un error, es un estado de calendario válido (entretemporada).
            return new PlataformaReporteResponse(balneariosActivos, List.of());
        }
        var rango = RangoFechasUtil.resolver(temporada.get().desde(), temporada.get().hasta());
        List<VolumenPorBalnearioResponse> volumen = pedidoRepo.volumenPorBalneario(rango.desde(), rango.hasta());
        return new PlataformaReporteResponse(balneariosActivos, volumen);
    }
}
