package com.easybeach.catalog.service;

import com.easybeach.catalog.domain.Producto;
import com.easybeach.catalog.domain.ProductoVariante;
import com.easybeach.catalog.repository.ProductoRepository;
import com.easybeach.catalog.repository.ProductoVarianteRepository;
import com.easybeach.catalog.web.dto.ProductoVarianteRequest;
import com.easybeach.catalog.web.dto.ProductoVarianteResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.tenancy.TenantFilterService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoVarianteService {

    private final ProductoVarianteRepository repository;
    private final ProductoRepository productoRepository;
    private final TenantFilterService tenantFilterService;

    public ProductoVarianteService(ProductoVarianteRepository repository, ProductoRepository productoRepository,
                                    TenantFilterService tenantFilterService) {
        this.repository = repository;
        this.productoRepository = productoRepository;
        this.tenantFilterService = tenantFilterService;
    }

    @Transactional
    public ProductoVarianteResponse crear(Long balnearioId, Long productoId, ProductoVarianteRequest request) {
        tenantFilterService.applyCurrentTenant();
        Producto producto = obtenerProductoPropio(balnearioId, productoId);
        ProductoVariante variante = new ProductoVariante();
        variante.setBalnearioId(balnearioId);
        variante.setProducto(producto);
        aplicar(variante, request);
        return toResponse(repository.save(variante));
    }

    @Transactional
    public ProductoVarianteResponse actualizar(Long balnearioId, Long productoId, Long id, ProductoVarianteRequest request) {
        tenantFilterService.applyCurrentTenant();
        obtenerProductoPropio(balnearioId, productoId);
        ProductoVariante variante = obtenerPropia(balnearioId, productoId, id);
        aplicar(variante, request);
        return toResponse(repository.save(variante));
    }

    @Transactional(readOnly = true)
    public List<ProductoVarianteResponse> listar(Long balnearioId, Long productoId) {
        tenantFilterService.applyCurrentTenant();
        obtenerProductoPropio(balnearioId, productoId);
        return repository.findByProductoIdOrderByOrdenAsc(productoId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void eliminar(Long balnearioId, Long productoId, Long id) {
        tenantFilterService.applyCurrentTenant();
        obtenerProductoPropio(balnearioId, productoId);
        ProductoVariante variante = obtenerPropia(balnearioId, productoId, id);
        variante.setDeletedAt(Instant.now());
        repository.save(variante);
    }

    private Producto obtenerProductoPropio(Long balnearioId, Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!producto.getBalnearioId().equals(balnearioId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        return producto;
    }

    private ProductoVariante obtenerPropia(Long balnearioId, Long productoId, Long id) {
        ProductoVariante variante = repository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!variante.getBalnearioId().equals(balnearioId) || !variante.getProducto().getId().equals(productoId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        return variante;
    }

    private void aplicar(ProductoVariante variante, ProductoVarianteRequest request) {
        variante.setNombre(request.nombre());
        variante.setPrecio(request.precio());
        variante.setDisponible(request.disponible());
        variante.setOrden(request.orden());
    }

    private ProductoVarianteResponse toResponse(ProductoVariante variante) {
        return new ProductoVarianteResponse(variante.getId(), variante.getNombre(), variante.getPrecio(),
                variante.isDisponible(), variante.getOrden());
    }
}
