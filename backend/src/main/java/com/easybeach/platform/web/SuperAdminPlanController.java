package com.easybeach.platform.web;

import com.easybeach.platform.service.PlanService;
import com.easybeach.platform.web.dto.PlanRequest;
import com.easybeach.platform.web.dto.PlanResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin/planes")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminPlanController {

    private final PlanService planService;

    public SuperAdminPlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse crear(@Valid @RequestBody PlanRequest request) {
        return planService.crear(request);
    }

    @PutMapping("/{id}")
    public PlanResponse actualizar(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        return planService.actualizar(id, request);
    }

    @GetMapping
    public List<PlanResponse> listar() {
        return planService.listar();
    }
}
