package com.easybeach.platform.service;

import com.easybeach.platform.domain.Plan;
import com.easybeach.platform.repository.PlanRepository;
import com.easybeach.platform.web.dto.PlanRequest;
import com.easybeach.platform.web.dto.PlanResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {

    private final PlanRepository repository;

    public PlanService(PlanRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PlanResponse crear(PlanRequest request) {
        Plan plan = new Plan();
        aplicar(plan, request);
        return toResponse(repository.save(plan));
    }

    @Transactional
    public PlanResponse actualizar(Long id, PlanRequest request) {
        Plan plan = repository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        aplicar(plan, request);
        return toResponse(repository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    private void aplicar(Plan plan, PlanRequest request) {
        plan.setNombre(request.nombre());
        plan.setDescripcion(request.descripcion());
        plan.setPrecio(request.precio());
        plan.setActivo(request.activo());
    }

    private PlanResponse toResponse(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getNombre(), plan.getDescripcion(), plan.getPrecio(), plan.isActivo());
    }
}
