package com.easybeach.promotions.service;

import com.easybeach.catalog.repository.CategoriaMenuRepository;
import com.easybeach.catalog.repository.ProductoRepository;
import com.easybeach.promotions.domain.EstadoPromocion;
import com.easybeach.promotions.domain.Promocion;
import com.easybeach.promotions.domain.PromocionAlcance;
import com.easybeach.promotions.domain.PromocionComboItem;
import com.easybeach.promotions.domain.TipoAlcance;
import com.easybeach.promotions.domain.TipoPromocion;
import com.easybeach.promotions.repository.PromocionAlcanceRepository;
import com.easybeach.promotions.repository.PromocionComboItemRepository;
import com.easybeach.promotions.repository.PromocionRepository;
import com.easybeach.promotions.web.dto.AlcancePromocionRequest;
import com.easybeach.promotions.web.dto.ComboItemRequest;
import com.easybeach.promotions.web.dto.PromocionRequest;
import com.easybeach.promotions.web.dto.PromocionResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.tenancy.TenantFilterService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ABM de promociones por el admin de balneario (etapa 14), con activación on/off. */
@Service
public class PromocionService {

    private final PromocionRepository promocionRepository;
    private final PromocionAlcanceRepository alcanceRepository;
    private final PromocionComboItemRepository comboItemRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaMenuRepository categoriaRepository;
    private final TenantFilterService tenantFilterService;

    public PromocionService(PromocionRepository promocionRepository, PromocionAlcanceRepository alcanceRepository,
                             PromocionComboItemRepository comboItemRepository, ProductoRepository productoRepository,
                             CategoriaMenuRepository categoriaRepository, TenantFilterService tenantFilterService) {
        this.promocionRepository = promocionRepository;
        this.alcanceRepository = alcanceRepository;
        this.comboItemRepository = comboItemRepository;
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.tenantFilterService = tenantFilterService;
    }

    @Transactional
    public PromocionResponse crear(Long balnearioId, PromocionRequest request) {
        tenantFilterService.applyCurrentTenant();
        validar(balnearioId, request);

        Promocion promocion = new Promocion();
        promocion.setBalnearioId(balnearioId);
        aplicarCampos(promocion, request);
        promocion = promocionRepository.save(promocion);

        guardarAlcanceYCombo(balnearioId, promocion, request);
        return toResponse(promocion);
    }

    @Transactional
    public PromocionResponse actualizar(Long balnearioId, Long id, PromocionRequest request) {
        tenantFilterService.applyCurrentTenant();
        validar(balnearioId, request);
        Promocion promocion = obtenerPropia(balnearioId, id);
        aplicarCampos(promocion, request);
        promocion = promocionRepository.save(promocion);

        // Reemplazo completo de alcance/combo: más simple y correcto que un
        // diff parcial, y esta ABM no es de alta frecuencia.
        alcanceRepository.deleteAll(alcanceRepository.findByPromocionId(id));
        comboItemRepository.deleteAll(comboItemRepository.findByPromocionId(id));
        guardarAlcanceYCombo(balnearioId, promocion, request);
        return toResponse(promocion);
    }

    @Transactional
    public PromocionResponse cambiarEstado(Long balnearioId, Long id, EstadoPromocion nuevoEstado) {
        tenantFilterService.applyCurrentTenant();
        Promocion promocion = obtenerPropia(balnearioId, id);
        promocion.setEstado(nuevoEstado);
        return toResponse(promocionRepository.save(promocion));
    }

    @Transactional
    public void eliminar(Long balnearioId, Long id) {
        tenantFilterService.applyCurrentTenant();
        Promocion promocion = obtenerPropia(balnearioId, id);
        promocion.setDeletedAt(Instant.now());
        promocionRepository.save(promocion);
    }

    @Transactional(readOnly = true)
    public List<PromocionResponse> listar(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return promocionRepository.findByBalnearioIdOrderByIdDesc(balnearioId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validar(Long balnearioId, PromocionRequest request) {
        if (request.vigenciaDesde() != null && request.vigenciaHasta() != null
                && request.vigenciaDesde().isAfter(request.vigenciaHasta())) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA, "vigenciaDesde no puede ser posterior a vigenciaHasta");
        }
        if ((request.franjaHoraDesde() == null) != (request.franjaHoraHasta() == null)) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA,
                    "franjaHoraDesde y franjaHoraHasta deben venir juntos");
        }
        boolean esPorcentualOHappyHour = request.tipo() == TipoPromocion.DESCUENTO_PORCENTUAL
                || request.tipo() == TipoPromocion.HAPPY_HOUR;
        if (esPorcentualOHappyHour) {
            if (request.alcances() == null || request.alcances().isEmpty()) {
                throw new ApiException(ErrorCode.VALIDACION_FALLIDA,
                        "Este tipo de promoción requiere al menos un alcance (producto o categoría)");
            }
            for (AlcancePromocionRequest alcance : request.alcances()) {
                validarReferenciaDeAlcance(balnearioId, alcance);
            }
        } else {
            if (request.comboItems() == null || request.comboItems().size() < 2) {
                throw new ApiException(ErrorCode.VALIDACION_FALLIDA,
                        "Un combo requiere al menos dos productos");
            }
            for (ComboItemRequest item : request.comboItems()) {
                productoRepository.findById(item.productoId())
                        .filter(p -> p.getBalnearioId().equals(balnearioId))
                        .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO,
                                "Producto de combo inexistente"));
            }
        }
    }

    private void validarReferenciaDeAlcance(Long balnearioId, AlcancePromocionRequest alcance) {
        if (alcance.tipoAlcance() == TipoAlcance.PRODUCTO) {
            productoRepository.findById(alcance.referenciaId())
                    .filter(p -> p.getBalnearioId().equals(balnearioId))
                    .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO,
                            "Producto de alcance inexistente"));
        } else {
            categoriaRepository.findById(alcance.referenciaId())
                    .filter(c -> c.getBalnearioId().equals(balnearioId))
                    .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO,
                            "Categoría de alcance inexistente"));
        }
    }

    private void guardarAlcanceYCombo(Long balnearioId, Promocion promocion, PromocionRequest request) {
        if (request.alcances() != null) {
            for (AlcancePromocionRequest a : request.alcances()) {
                PromocionAlcance alcance = new PromocionAlcance();
                alcance.setPromocionId(promocion.getId());
                alcance.setBalnearioId(balnearioId);
                alcance.setTipoAlcance(a.tipoAlcance());
                alcance.setReferenciaId(a.referenciaId());
                alcanceRepository.save(alcance);
            }
        }
        if (request.comboItems() != null) {
            for (ComboItemRequest item : request.comboItems()) {
                PromocionComboItem comboItem = new PromocionComboItem();
                comboItem.setPromocionId(promocion.getId());
                comboItem.setBalnearioId(balnearioId);
                comboItem.setProductoId(item.productoId());
                comboItem.setCantidad(item.cantidad());
                comboItemRepository.save(comboItem);
            }
        }
    }

    private void aplicarCampos(Promocion promocion, PromocionRequest request) {
        promocion.setNombre(request.nombre());
        promocion.setTipo(request.tipo());
        promocion.setEstado(request.activa() ? EstadoPromocion.ACTIVA : EstadoPromocion.INACTIVA);
        promocion.setValor(request.valor());
        promocion.setVigenciaDesde(request.vigenciaDesde());
        promocion.setVigenciaHasta(request.vigenciaHasta());
        promocion.setFranjaHoraDesde(request.franjaHoraDesde());
        promocion.setFranjaHoraHasta(request.franjaHoraHasta());
        promocion.setDiasSemana(request.diasSemana());
    }

    private Promocion obtenerPropia(Long balnearioId, Long id) {
        return promocionRepository.findByIdAndBalnearioId(id, balnearioId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
    }

    private PromocionResponse toResponse(Promocion promocion) {
        List<AlcancePromocionRequest> alcances = alcanceRepository.findByPromocionId(promocion.getId()).stream()
                .map(a -> new AlcancePromocionRequest(a.getTipoAlcance(), a.getReferenciaId()))
                .toList();
        List<ComboItemRequest> comboItems = comboItemRepository.findByPromocionId(promocion.getId()).stream()
                .map(i -> new ComboItemRequest(i.getProductoId(), i.getCantidad()))
                .toList();
        return new PromocionResponse(promocion.getId(), promocion.getNombre(), promocion.getTipo().name(),
                promocion.getEstado().name(), promocion.getValor(), promocion.getVigenciaDesde(),
                promocion.getVigenciaHasta(), promocion.getFranjaHoraDesde(), promocion.getFranjaHoraHasta(),
                promocion.getDiasSemana(), alcances, comboItems);
    }
}
