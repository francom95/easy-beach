package com.easybeach.reporting.service;

import com.easybeach.reporting.dto.DashboardResumenResponse;
import com.easybeach.reporting.dto.EstadiasReporteResponse;
import com.easybeach.reporting.dto.ProductoVendidoResponse;
import com.easybeach.reporting.dto.PromocionRendimientoResponse;
import com.easybeach.reporting.dto.ServiciosReporteResponse;
import com.easybeach.reporting.dto.VentasReporteResponse;
import com.easybeach.reporting.repository.EstadiaReportingRepository;
import com.easybeach.reporting.repository.PedidoReportingRepository;
import com.easybeach.reporting.repository.ServicioReportingRepository;
import com.easybeach.shared.time.ZonaNegocio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Reportes de un balneario (etapa 15): consultas de solo lectura sobre
 * datos que ya existen, sin tablas de agregación ni jobs - si una query no
 * rinde en producción, se escala la decisión, no se inventa infraestructura
 * (alcance explícito del plan).
 */
@Service
public class ReporteBalnearioService {

    private static final int TOP_PRODUCTOS_DEFAULT = 20;

    private final PedidoReportingRepository pedidoRepo;
    private final EstadiaReportingRepository estadiaRepo;
    private final ServicioReportingRepository servicioRepo;

    public ReporteBalnearioService(PedidoReportingRepository pedidoRepo, EstadiaReportingRepository estadiaRepo,
                                    ServicioReportingRepository servicioRepo) {
        this.pedidoRepo = pedidoRepo;
        this.estadiaRepo = estadiaRepo;
        this.servicioRepo = servicioRepo;
    }

    public VentasReporteResponse ventas(Long balnearioId, LocalDate desde, LocalDate hasta) {
        var rango = RangoFechasUtil.resolver(desde, hasta);
        BigDecimal facturacion = pedidoRepo.facturacionTotal(balnearioId, rango.desde(), rango.hasta());
        long cantidad = pedidoRepo.cantidadPedidosEntregados(balnearioId, rango.desde(), rango.hasta());
        return new VentasReporteResponse(facturacion, cantidad, ticketPromedio(facturacion, cantidad),
                pedidoRepo.ventasPorDia(balnearioId, rango.desde(), rango.hasta()));
    }

    public List<ProductoVendidoResponse> productosMasVendidos(
            Long balnearioId, LocalDate desde, LocalDate hasta, Integer limite) {
        var rango = RangoFechasUtil.resolver(desde, hasta);
        int top = limite == null || limite <= 0 ? TOP_PRODUCTOS_DEFAULT : limite;
        return pedidoRepo.productosMasVendidos(balnearioId, rango.desde(), rango.hasta(), top);
    }

    public List<PromocionRendimientoResponse> rendimientoPromociones(
            Long balnearioId, LocalDate desde, LocalDate hasta) {
        var rango = RangoFechasUtil.resolver(desde, hasta);
        return pedidoRepo.rendimientoPromociones(balnearioId, rango.desde(), rango.hasta());
    }

    public EstadiasReporteResponse estadias(Long balnearioId, LocalDate desde, LocalDate hasta) {
        var rango = RangoFechasUtil.resolver(desde, hasta);
        var aperturas = estadiaRepo.aperturasPorDia(balnearioId, rango.desde(), rango.hasta());
        Double duracionHoras = estadiaRepo.duracionPromedioHoras(balnearioId, rango.desde(), rango.hasta());
        BigDecimal consumoPromedio = pedidoRepo.consumoPromedioPorEstadia(balnearioId, rango.desde(), rango.hasta());
        return new EstadiasReporteResponse(aperturas, duracionHoras, consumoPromedio);
    }

    public ServiciosReporteResponse servicios(Long balnearioId, LocalDate desde, LocalDate hasta) {
        var rango = RangoFechasUtil.resolver(desde, hasta);
        var porTipo = servicioRepo.solicitudesPorTipo(balnearioId, rango.desde(), rango.hasta());
        Double tiempoResolucion = servicioRepo.tiempoResolucionPromedioMinutos(balnearioId, rango.desde(), rango.hasta());
        return new ServiciosReporteResponse(porTipo, tiempoResolucion);
    }

    /** "Hoy" en TZ de negocio, no en UTC (etapa 04): si son las 23:50 en la playa, es hoy aunque en UTC ya sea mañana. */
    public DashboardResumenResponse dashboard(Long balnearioId) {
        LocalDate hoy = LocalDate.now(ZonaNegocio.ZONE_ID);
        Instant inicio = hoy.atStartOfDay(ZonaNegocio.ZONE_ID).toInstant();
        Instant fin = hoy.plusDays(1).atStartOfDay(ZonaNegocio.ZONE_ID).toInstant();

        BigDecimal facturacionHoy = pedidoRepo.facturacionTotal(balnearioId, inicio, fin);
        long pedidosEntregadosHoy = pedidoRepo.cantidadPedidosEntregados(balnearioId, inicio, fin);
        long pedidosEnCurso = pedidoRepo.pedidosEnCurso(balnearioId);
        return new DashboardResumenResponse(facturacionHoy, pedidosEntregadosHoy,
                ticketPromedio(facturacionHoy, pedidosEntregadosHoy), pedidosEnCurso);
    }

    private BigDecimal ticketPromedio(BigDecimal facturacion, long cantidadPedidos) {
        if (cantidadPedidos == 0) {
            return BigDecimal.ZERO;
        }
        return facturacion.divide(BigDecimal.valueOf(cantidadPedidos), 2, RoundingMode.HALF_UP);
    }
}
