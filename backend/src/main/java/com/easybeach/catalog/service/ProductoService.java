package com.easybeach.catalog.service;

import com.easybeach.catalog.domain.CategoriaMenu;
import com.easybeach.catalog.domain.Producto;
import com.easybeach.catalog.repository.CategoriaMenuRepository;
import com.easybeach.catalog.repository.ProductoRepository;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.storage.AssetStorageService;
import com.easybeach.shared.tenancy.TenantFilterService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductoService {

    private final ProductoRepository repository;
    private final CategoriaMenuRepository categoriaRepository;
    private final TenantFilterService tenantFilterService;
    private final AssetStorageService assetStorageService;

    public ProductoService(ProductoRepository repository, CategoriaMenuRepository categoriaRepository,
                            TenantFilterService tenantFilterService, AssetStorageService assetStorageService) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.tenantFilterService = tenantFilterService;
        this.assetStorageService = assetStorageService;
    }

    @Transactional
    public ProductoResponse crear(Long balnearioId, ProductoRequest request) {
        tenantFilterService.applyCurrentTenant();
        CategoriaMenu categoria = obtenerCategoriaPropia(balnearioId, request.categoriaId());
        Producto producto = new Producto();
        producto.setBalnearioId(balnearioId);
        producto.setCategoria(categoria);
        aplicar(producto, request, categoria);
        return toResponse(repository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long balnearioId, Long id, ProductoRequest request) {
        tenantFilterService.applyCurrentTenant();
        Producto producto = obtenerPropio(balnearioId, id);
        CategoriaMenu categoria = obtenerCategoriaPropia(balnearioId, request.categoriaId());
        aplicar(producto, request, categoria);
        return toResponse(repository.save(producto));
    }

    /** "Disponibilidad on/off inmediata" (etapa 11): un PATCH liviano dedicado, sin tocar el resto de los campos. */
    @Transactional
    public ProductoResponse cambiarDisponibilidad(Long balnearioId, Long id, boolean disponible) {
        tenantFilterService.applyCurrentTenant();
        Producto producto = obtenerPropio(balnearioId, id);
        producto.setDisponible(disponible);
        return toResponse(repository.save(producto));
    }

    /** Foto de producto (etapa 17): {@code ProductoRequest} nunca tuvo forma de subirla - gap real encontrado. */
    @Transactional
    public ProductoResponse actualizarFoto(Long balnearioId, Long id, MultipartFile file) {
        tenantFilterService.applyCurrentTenant();
        Producto producto = obtenerPropio(balnearioId, id);
        AssetStorageService.StoredAsset stored = assetStorageService.storeProductoFoto(balnearioId, file);
        producto.setFotoUrl(stored.publicUrl());
        return toResponse(repository.save(producto));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return repository.findByBalnearioIdOrderByOrdenAsc(balnearioId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void eliminar(Long balnearioId, Long id) {
        tenantFilterService.applyCurrentTenant();
        Producto producto = obtenerPropio(balnearioId, id);
        producto.setDeletedAt(Instant.now());
        repository.save(producto);
    }

    private CategoriaMenu obtenerCategoriaPropia(Long balnearioId, Long categoriaId) {
        CategoriaMenu categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!categoria.getBalnearioId().equals(balnearioId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        return categoria;
    }

    private Producto obtenerPropio(Long balnearioId, Long id) {
        Producto producto = repository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!producto.getBalnearioId().equals(balnearioId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        return producto;
    }

    private void aplicar(Producto producto, ProductoRequest request, CategoriaMenu categoria) {
        producto.setCategoria(categoria);
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecioBase(request.precioBase());
        producto.setDisponible(request.disponible());
        producto.setOrden(request.orden());
    }

    private ProductoResponse toResponse(Producto producto) {
        return new ProductoResponse(producto.getId(), producto.getCategoria().getId(), producto.getNombre(),
                producto.getDescripcion(), producto.getPrecioBase(), producto.getFotoUrl(), producto.isDisponible(),
                producto.getOrden());
    }
}
