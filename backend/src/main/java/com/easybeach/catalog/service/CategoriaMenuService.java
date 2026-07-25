package com.easybeach.catalog.service;

import com.easybeach.catalog.domain.CategoriaMenu;
import com.easybeach.catalog.repository.CategoriaMenuRepository;
import com.easybeach.catalog.repository.ProductoRepository;
import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.tenancy.TenantFilterService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaMenuService {

    private final CategoriaMenuRepository repository;
    private final ProductoRepository productoRepository;
    private final TenantFilterService tenantFilterService;

    public CategoriaMenuService(CategoriaMenuRepository repository, ProductoRepository productoRepository,
                                 TenantFilterService tenantFilterService) {
        this.repository = repository;
        this.productoRepository = productoRepository;
        this.tenantFilterService = tenantFilterService;
    }

    @Transactional
    public CategoriaMenuResponse crear(Long balnearioId, CategoriaMenuRequest request) {
        tenantFilterService.applyCurrentTenant();
        CategoriaMenu categoria = new CategoriaMenu();
        categoria.setBalnearioId(balnearioId);
        aplicar(categoria, request);
        return toResponse(repository.save(categoria));
    }

    @Transactional
    public CategoriaMenuResponse actualizar(Long balnearioId, Long id, CategoriaMenuRequest request) {
        tenantFilterService.applyCurrentTenant();
        CategoriaMenu categoria = obtenerPropia(balnearioId, id);
        aplicar(categoria, request);
        return toResponse(repository.save(categoria));
    }

    @Transactional(readOnly = true)
    public List<CategoriaMenuResponse> listar(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return repository.findByBalnearioIdOrderByOrdenAsc(balnearioId).stream().map(this::toResponse).toList();
    }

    /** Etapa 11 §5: "no borrar categoría con productos" - regla real, ya verificable (Producto existe). */
    @Transactional
    public void eliminar(Long balnearioId, Long id) {
        tenantFilterService.applyCurrentTenant();
        CategoriaMenu categoria = obtenerPropia(balnearioId, id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                    "No se puede borrar una categoría que tiene productos");
        }
        categoria.setDeletedAt(Instant.now());
        repository.save(categoria);
    }

    private CategoriaMenu obtenerPropia(Long balnearioId, Long id) {
        CategoriaMenu categoria = repository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!categoria.getBalnearioId().equals(balnearioId)) {
            throw new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO);
        }
        return categoria;
    }

    private void aplicar(CategoriaMenu categoria, CategoriaMenuRequest request) {
        categoria.setNombre(request.nombre());
        categoria.setOrden(request.orden());
        categoria.setActiva(request.activa());
    }

    private CategoriaMenuResponse toResponse(CategoriaMenu categoria) {
        return new CategoriaMenuResponse(categoria.getId(), categoria.getNombre(), categoria.getOrden(), categoria.isActiva());
    }
}
