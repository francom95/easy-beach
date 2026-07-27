package com.easybeach.platform.service;

import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.repository.RolRepository;
import com.easybeach.identity.repository.UsuarioBalnearioRolRepository;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.platform.domain.Balneario;
import com.easybeach.platform.domain.EstadoBalneario;
import com.easybeach.platform.domain.EstadoSuscripcion;
import com.easybeach.platform.domain.EstadoTemporada;
import com.easybeach.platform.event.BalnearioCreado;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.platform.repository.SuscripcionTemporadaRepository;
import com.easybeach.platform.web.dto.ActualizarBalnearioRequest;
import com.easybeach.platform.web.dto.BalnearioResponse;
import com.easybeach.platform.web.dto.CrearBalnearioRequest;
import com.easybeach.platform.web.dto.CrearBalnearioResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.security.PasswordTemporalGenerator;
import com.easybeach.shared.security.RolCodigo;
import com.easybeach.shared.security.TipoUsuario;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ABM de balnearios (etapa 10). Cross-tenant por diseño - lo opera Super
 * Admin, sin {@code TenantFilterService}: acá no hay "un" tenant, se
 * administran todos.
 */
@Service
public class BalnearioService {

    private final BalnearioRepository balnearioRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioBalnearioRolRepository usuarioBalnearioRolRepository;
    private final RolRepository rolRepository;
    private final SuscripcionTemporadaRepository suscripcionRepository;
    private final AuditoriaPlataformaService auditoriaService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public BalnearioService(BalnearioRepository balnearioRepository,
                             UsuarioRepository usuarioRepository,
                             UsuarioBalnearioRolRepository usuarioBalnearioRolRepository,
                             RolRepository rolRepository,
                             SuscripcionTemporadaRepository suscripcionRepository,
                             AuditoriaPlataformaService auditoriaService,
                             PasswordEncoder passwordEncoder,
                             ApplicationEventPublisher eventPublisher) {
        this.balnearioRepository = balnearioRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioBalnearioRolRepository = usuarioBalnearioRolRepository;
        this.rolRepository = rolRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.auditoriaService = auditoriaService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CrearBalnearioResponse crear(Long actorSuperAdminId, CrearBalnearioRequest request) {
        if (balnearioRepository.findBySlug(request.slug()).isPresent()) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA, "Ya existe un balneario con ese slug");
        }
        if (usuarioRepository.existsByEmail(request.emailAdmin())) {
            throw new ApiException(ErrorCode.EMAIL_YA_REGISTRADO);
        }

        Balneario balneario = new Balneario();
        balneario.setSlug(request.slug());
        balneario.setNombre(request.nombre());
        balneario.setEmailContacto(request.emailContactoBalneario());
        balneario.setTelefono(request.telefono());
        balneario.setEstado(EstadoBalneario.ACTIVO);
        balneario = balnearioRepository.save(balneario);

        String passwordTemporal = PasswordTemporalGenerator.generar();
        Usuario admin = new Usuario();
        admin.setEmail(request.emailAdmin());
        admin.setNombre(request.nombreAdmin());
        admin.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        admin.setTipo(TipoUsuario.STAFF);
        admin.setEstado(EstadoUsuario.ACTIVO);
        admin.setDebeCambiarPassword(true);
        admin = usuarioRepository.save(admin);

        UsuarioBalnearioRol vinculo = new UsuarioBalnearioRol();
        vinculo.setUsuario(admin);
        vinculo.setBalnearioId(balneario.getId());
        vinculo.setRol(rolRepository.findByCodigo(RolCodigo.ADMIN_BALNEARIO).orElseThrow());
        usuarioBalnearioRolRepository.save(vinculo);

        eventPublisher.publishEvent(new BalnearioCreado(balneario.getId()));

        auditoriaService.registrar(actorSuperAdminId, "BALNEARIO_CREADO", "balneario", balneario.getId(),
                balneario.getId(), Map.of("slug", balneario.getSlug(), "emailAdmin", request.emailAdmin()));

        return new CrearBalnearioResponse(toResponse(balneario), request.emailAdmin(), passwordTemporal);
    }

    @Transactional
    public BalnearioResponse actualizar(Long balnearioId, ActualizarBalnearioRequest request) {
        Balneario balneario = obtenerOFallar(balnearioId);
        balneario.setNombre(request.nombre());
        balneario.setEmailContacto(request.emailContacto());
        balneario.setTelefono(request.telefono());
        return toResponse(balnearioRepository.save(balneario));
    }

    @Transactional(readOnly = true)
    public Page<BalnearioResponse> listar(Pageable pageable) {
        return balnearioRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BalnearioResponse obtener(Long balnearioId) {
        return toResponse(obtenerOFallar(balnearioId));
    }

    /** Público (sin auth): solo balnearios operativos - selector de balneario de la app cliente. */
    @Transactional(readOnly = true)
    public List<BalnearioResponse> listarPublico() {
        return suscripcionRepository.findBalnearioIdsConSuscripcionActivaEnTemporadaEnCurso().stream()
                .map(balnearioRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(b -> b.getEstado() == EstadoBalneario.ACTIVO)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BalnearioResponse obtenerPublicoPorSlug(String slug) {
        Balneario balneario = balnearioRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!esOperativo(balneario.getId())) {
            throw new ApiException(ErrorCode.BALNEARIO_NO_OPERATIVO);
        }
        return toResponse(balneario);
    }

    @Transactional
    public BalnearioResponse activar(Long actorSuperAdminId, Long balnearioId, String motivo) {
        Balneario balneario = obtenerOFallar(balnearioId);
        balneario.setEstado(EstadoBalneario.ACTIVO);
        balneario = balnearioRepository.save(balneario);
        auditoriaService.registrar(actorSuperAdminId, "BALNEARIO_ACTIVADO", "balneario", balnearioId,
                balnearioId, Map.of("motivo", motivo));
        return toResponse(balneario);
    }

    @Transactional
    public BalnearioResponse suspender(Long actorSuperAdminId, Long balnearioId, String motivo) {
        Balneario balneario = obtenerOFallar(balnearioId);
        balneario.setEstado(EstadoBalneario.SUSPENDIDO);
        balneario = balnearioRepository.save(balneario);
        auditoriaService.registrar(actorSuperAdminId, "BALNEARIO_SUSPENDIDO", "balneario", balnearioId,
                balnearioId, Map.of("motivo", motivo));
        // Nota (etapa 10): la política sobre estadías/pedidos EN CURSO al suspender
        // se implementa en la etapa 12 (stay) - la entidad Estadia no existe todavía.
        // Lo que sí es efectivo ya: esOperativo(balnearioId) pasa a false de inmediato,
        // así que ningún flujo nuevo (apertura de estadía, pedido) puede empezar.
        return toResponse(balneario);
    }

    /** "Un balneario suspendido o fuera de temporada no acepta estadías ni pedidos nuevos" (criterio de aceptación). */
    @Transactional(readOnly = true)
    public boolean esOperativo(Long balnearioId) {
        Balneario balneario = balnearioRepository.findById(balnearioId).orElse(null);
        if (balneario == null || balneario.getEstado() != EstadoBalneario.ACTIVO) {
            return false;
        }
        return suscripcionRepository.findByBalnearioId(balnearioId).stream()
                .anyMatch(s -> s.getEstado() == EstadoSuscripcion.ACTIVA
                        && s.getTemporada().getEstado() == EstadoTemporada.EN_CURSO);
    }

    private Balneario obtenerOFallar(Long balnearioId) {
        return balnearioRepository.findById(balnearioId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
    }

    private BalnearioResponse toResponse(Balneario balneario) {
        return new BalnearioResponse(balneario.getId(), balneario.getSlug(), balneario.getNombre(),
                balneario.getEmailContacto(), balneario.getTelefono(), balneario.getEstado().name(),
                esOperativo(balneario.getId()));
    }
}
