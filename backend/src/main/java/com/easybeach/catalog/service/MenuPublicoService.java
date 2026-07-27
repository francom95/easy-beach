package com.easybeach.catalog.service;

import com.easybeach.catalog.domain.CategoriaMenu;
import com.easybeach.catalog.domain.Producto;
import com.easybeach.catalog.domain.ProductoVariante;
import com.easybeach.catalog.repository.CategoriaMenuRepository;
import com.easybeach.catalog.repository.ProductoRepository;
import com.easybeach.catalog.repository.ProductoVarianteRepository;
import com.easybeach.catalog.service.PromocionesPublicasProvider.ProductoParaPromo;
import com.easybeach.catalog.service.PromocionesPublicasProvider.PromocionResumen;
import com.easybeach.catalog.web.dto.MenuCategoriaResponse;
import com.easybeach.catalog.web.dto.MenuProductoResponse;
import com.easybeach.catalog.web.dto.MenuVarianteResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Etapa 11 criterio de aceptación: "el menú público responde en una sola
 * llamada todo lo que la pantalla de menú de la etapa 07 necesita". Público
 * - NO pasa por {@code TenantFilterService} (no hay tenant en el request;
 * el balneario ya viene resuelto por slug desde el controller, y estas
 * queries están explícitamente acotadas a {@code balnearioId} igual).
 *
 * <p>Promociones vigentes (etapa 14) se embeben vía
 * {@link PromocionesPublicasProvider} - inversión de dependencia (ADR-002):
 * este módulo no puede depender de {@code promotions}.
 */
@Service
public class MenuPublicoService {

    private final CategoriaMenuRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final PromocionesPublicasProvider promocionesPublicasProvider;

    public MenuPublicoService(CategoriaMenuRepository categoriaRepository, ProductoRepository productoRepository,
                               ProductoVarianteRepository varianteRepository,
                               PromocionesPublicasProvider promocionesPublicasProvider) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.promocionesPublicasProvider = promocionesPublicasProvider;
    }

    @Transactional(readOnly = true)
    public List<MenuCategoriaResponse> obtenerMenu(Long balnearioId) {
        List<CategoriaMenu> categorias = categoriaRepository.findByBalnearioIdAndActivaTrueOrderByOrdenAsc(balnearioId);
        List<Producto> productosDisponibles = productoRepository.findByBalnearioIdAndDisponibleTrueOrderByOrdenAsc(balnearioId);

        Map<Long, List<Producto>> productosPorCategoria = productosDisponibles.stream()
                .collect(Collectors.groupingBy(p -> p.getCategoria().getId()));

        List<Long> productoIds = productosDisponibles.stream().map(Producto::getId).toList();
        Map<Long, List<ProductoVariante>> variantesPorProducto = productoIds.isEmpty()
                ? Map.of()
                : varianteRepository.findByProductoIdInAndDisponibleTrue(productoIds).stream()
                        .collect(Collectors.groupingBy(v -> v.getProducto().getId()));

        List<ProductoParaPromo> productosParaPromo = productosDisponibles.stream()
                .map(p -> new ProductoParaPromo(p.getId(), p.getCategoria().getId()))
                .toList();
        Map<Long, List<PromocionResumen>> promosPorProducto =
                promocionesPublicasProvider.resolverPorProducto(balnearioId, productosParaPromo);

        return categorias.stream()
                .map(categoria -> new MenuCategoriaResponse(
                        categoria.getId(),
                        categoria.getNombre(),
                        productosPorCategoria.getOrDefault(categoria.getId(), List.of()).stream()
                                .map(producto -> toMenuProducto(producto, variantesPorProducto, promosPorProducto))
                                .toList()))
                .toList();
    }

    private MenuProductoResponse toMenuProducto(Producto producto, Map<Long, List<ProductoVariante>> variantesPorProducto,
                                                 Map<Long, List<PromocionResumen>> promosPorProducto) {
        List<MenuVarianteResponse> variantes = variantesPorProducto.getOrDefault(producto.getId(), List.of()).stream()
                .map(v -> new MenuVarianteResponse(v.getId(), v.getNombre(), v.getPrecio()))
                .toList();
        return new MenuProductoResponse(producto.getId(), producto.getNombre(), producto.getDescripcion(),
                producto.getPrecioBase(), producto.getFotoUrl(), variantes,
                promosPorProducto.getOrDefault(producto.getId(), List.of()));
    }
}
