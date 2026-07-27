package com.easybeach.identity.service;

import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.repository.RolRepository;
import com.easybeach.identity.repository.UsuarioBalnearioRolRepository;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.identity.web.dto.InvitarStaffRequest;
import com.easybeach.identity.web.dto.InvitarStaffResponse;
import com.easybeach.identity.web.dto.MiembroResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.security.PasswordTemporalGenerator;
import com.easybeach.shared.security.RolCodigo;
import com.easybeach.shared.security.TipoUsuario;
import com.easybeach.shared.tenancy.TenantFilterService;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primer service tenant-scoped del proyecto: demuestra la convención
 * obligatoria de {@link TenantFilterService} (etapa 09, criterio de
 * aceptación "multitenancy operativo").
 *
 * <p>La invitación de staff (etapa 17: no existía ningún ABM real, solo el
 * demostrativo {@code /staff/miembros} de solo lectura de la etapa 09) sigue
 * el mismo patrón que el alta del admin de balneario (etapa 10): sin envío
 * de email en el MVP, la password temporal viaja en la respuesta.
 */
@Service
public class UsuarioBalnearioRolService {

    private static final Set<RolCodigo> ROLES_INVITABLES = Set.of(RolCodigo.CARPERO, RolCodigo.OPERADOR);

    private final TenantFilterService tenantFilterService;
    private final UsuarioBalnearioRolRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioBalnearioRolService(TenantFilterService tenantFilterService,
                                       UsuarioBalnearioRolRepository repository,
                                       UsuarioRepository usuarioRepository,
                                       RolRepository rolRepository,
                                       PasswordEncoder passwordEncoder) {
        this.tenantFilterService = tenantFilterService;
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@code findAll()} sin ningún {@code WHERE balneario_id} explícito en
     * el código: el aislamiento lo garantiza el filtro Hibernate habilitado
     * por {@link TenantFilterService#applyCurrentTenant()}, no la query.
     */
    @Transactional(readOnly = true)
    public List<UsuarioBalnearioRol> listarMiembrosDelBalnearioActual() {
        tenantFilterService.applyCurrentTenant();
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MiembroResponse> listarStaff(Long balnearioId) {
        tenantFilterService.applyCurrentTenant();
        return repository.findByBalnearioId(balnearioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InvitarStaffResponse invitar(Long balnearioId, InvitarStaffRequest request) {
        if (!ROLES_INVITABLES.contains(request.rol())) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA,
                    "Solo se puede invitar staff con rol CARPERO u OPERADOR");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.EMAIL_YA_REGISTRADO);
        }
        tenantFilterService.applyCurrentTenant();

        String passwordTemporal = PasswordTemporalGenerator.generar();
        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setNombre(request.nombre());
        usuario.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        usuario.setTipo(TipoUsuario.STAFF);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setDebeCambiarPassword(true);
        usuario = usuarioRepository.save(usuario);

        UsuarioBalnearioRol vinculo = new UsuarioBalnearioRol();
        vinculo.setUsuario(usuario);
        vinculo.setBalnearioId(balnearioId);
        vinculo.setRol(rolRepository.findByCodigo(request.rol()).orElseThrow());
        repository.save(vinculo);

        return new InvitarStaffResponse(usuario.getPublicId(), usuario.getEmail(), usuario.getNombre(),
                request.rol().name(), passwordTemporal);
    }

    /**
     * Revoca el acceso de un miembro a ESTE balneario (borra solo el vínculo
     * {@link UsuarioBalnearioRol}, nunca el {@link Usuario}: su historial en
     * {@code pedido_evento}/auditoría sigue siendo legible por
     * {@code usuario_id} - "no hard-delete" del mockup de etapa 08).
     */
    @Transactional
    public void revocar(Long balnearioId, String usuarioPublicId) {
        tenantFilterService.applyCurrentTenant();
        Usuario usuario = usuarioRepository.findByPublicId(usuarioPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        UsuarioBalnearioRol vinculo = repository.findByUsuarioIdAndBalnearioId(usuario.getId(), balnearioId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));

        if (vinculo.getRol().getCodigo() == RolCodigo.ADMIN_BALNEARIO) {
            long admins = repository.findByBalnearioId(balnearioId).stream()
                    .filter(m -> m.getRol().getCodigo() == RolCodigo.ADMIN_BALNEARIO)
                    .count();
            if (admins <= 1) {
                throw new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                        "No se puede quitar al único admin del balneario");
            }
        }
        repository.delete(vinculo);
    }

    private MiembroResponse toResponse(UsuarioBalnearioRol miembro) {
        return new MiembroResponse(miembro.getUsuario().getPublicId(), miembro.getUsuario().getNombre(),
                miembro.getUsuario().getEmail(), miembro.getRol().getCodigo().name(), miembro.getBalnearioId());
    }
}
