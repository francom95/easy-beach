package com.easybeach.platform.service;

import com.easybeach.platform.domain.AuditoriaPlataforma;
import com.easybeach.platform.repository.AuditoriaPlataformaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaPlataformaService {

    private final AuditoriaPlataformaRepository repository;
    private final ObjectMapper objectMapper;

    public AuditoriaPlataformaService(AuditoriaPlataformaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void registrar(Long actorUsuarioId, String accion, String entidadTipo, Long entidadId,
                           Long balnearioId, Map<String, Object> detalle) {
        AuditoriaPlataforma auditoria = new AuditoriaPlataforma();
        auditoria.setActorUsuarioId(actorUsuarioId);
        auditoria.setAccion(accion);
        auditoria.setEntidadTipo(entidadTipo);
        auditoria.setEntidadId(entidadId);
        auditoria.setBalnearioId(balnearioId);
        if (detalle != null && !detalle.isEmpty()) {
            try {
                auditoria.setDetalle(objectMapper.writeValueAsString(detalle));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("No se pudo serializar el detalle de auditoría", e);
            }
        }
        repository.save(auditoria);
    }

    /** Cross-tenant intencional: Super Admin ve auditoría de cualquier balneario o de la plataforma. */
    @Transactional(readOnly = true)
    public Page<AuditoriaPlataforma> listar(Long balnearioIdOpcional, Pageable pageable) {
        if (balnearioIdOpcional == null) {
            return repository.findAll(pageable);
        }
        return repository.findByBalnearioId(balnearioIdOpcional, pageable);
    }
}
