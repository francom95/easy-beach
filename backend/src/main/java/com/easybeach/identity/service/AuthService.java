package com.easybeach.identity.service;

import com.easybeach.identity.domain.EstadoSesion;
import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.identity.domain.SesionRefresh;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.repository.SesionRefreshRepository;
import com.easybeach.identity.repository.UsuarioBalnearioRolRepository;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.identity.security.JwtService;
import com.easybeach.identity.web.dto.LoginRequest;
import com.easybeach.identity.web.dto.RefreshRequest;
import com.easybeach.identity.web.dto.RegistroClienteRequest;
import com.easybeach.identity.web.dto.TokenResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.id.UlidGenerator;
import com.easybeach.shared.security.RolCodigo;
import com.easybeach.shared.security.TipoUsuario;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro/login de cliente y staff, refresh rotativo con detección de
 * reuso, logout (etapa 05 §1). Autenticación manual (password contra hash)
 * en vez de {@code AuthenticationManager}: el matiz de "cliente vs. staff
 * vs. super admin" y la resolución de {@code balneario_id} no encajan bien
 * en el flujo estándar de Spring Security basado en {@code UserDetailsService}.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioBalnearioRolRepository usuarioBalnearioRolRepository;
    private final SesionRefreshRepository sesionRefreshRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SesionRefreshRevocationService revocationService;

    public AuthService(UsuarioRepository usuarioRepository,
                        UsuarioBalnearioRolRepository usuarioBalnearioRolRepository,
                        SesionRefreshRepository sesionRefreshRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        SesionRefreshRevocationService revocationService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioBalnearioRolRepository = usuarioBalnearioRolRepository;
        this.sesionRefreshRepository = sesionRefreshRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.revocationService = revocationService;
    }

    @Transactional
    public TokenResponse registrarCliente(RegistroClienteRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.EMAIL_YA_REGISTRADO);
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setNombre(request.nombre());
        usuario.setTipo(TipoUsuario.CLIENTE);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);
        return issueTokens(usuario, RolCodigo.CLIENTE, null);
    }

    @Transactional
    public TokenResponse loginCliente(LoginRequest request) {
        Usuario usuario = authenticateByTipo(request, TipoUsuario.CLIENTE);
        return issueTokens(usuario, RolCodigo.CLIENTE, null);
    }

    @Transactional
    public TokenResponse loginStaff(LoginRequest request) {
        Usuario usuario = authenticateByTipo(request, TipoUsuario.STAFF);
        // Único lookup intencionalmente sin TenantContext (ver Javadoc del repositorio):
        // acá se DESCUBRE a qué balneario pertenece el staff.
        List<UsuarioBalnearioRol> membresias = usuarioBalnearioRolRepository.findByUsuarioId(usuario.getId());
        if (membresias.isEmpty()) {
            throw new ApiException(ErrorCode.CREDENCIALES_INVALIDAS, "El usuario no tiene un balneario asignado");
        }
        UsuarioBalnearioRol membresia = membresias.get(0);
        return issueTokens(usuario, membresia.getRol().getCodigo(), membresia.getBalnearioId());
    }

    @Transactional
    public TokenResponse loginSuperAdmin(LoginRequest request) {
        Usuario usuario = authenticateByTipo(request, TipoUsuario.SUPER_ADMIN);
        return issueTokens(usuario, RolCodigo.SUPER_ADMIN, null);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String hash = jwtService.hashRefreshToken(request.refreshToken());
        SesionRefresh sesion = sesionRefreshRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALIDO));

        if (sesion.getEstado() == EstadoSesion.ROTADA) {
            // Reuso de un refresh ya rotado: posible robo -> revocar toda la familia.
            // En su propia transacción (REQUIRES_NEW): debe quedar commiteada aunque
            // esta transacción termine haciendo rollback por el throw de abajo.
            revocationService.revocarFamilia(sesion.getFamiliaId());
            throw new ApiException(ErrorCode.REFRESH_REUTILIZADO);
        }
        if (sesion.getEstado() == EstadoSesion.REVOCADA || sesion.getExpiraAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.TOKEN_INVALIDO);
        }

        Usuario usuario = sesion.getUsuario();
        if (usuario.getEstado() == EstadoUsuario.BAJA) {
            throw new ApiException(ErrorCode.TOKEN_INVALIDO);
        }

        sesion.setEstado(EstadoSesion.ROTADA);
        sesion.setUpdatedAt(Instant.now());
        sesionRefreshRepository.save(sesion);

        RolCodigo rol;
        Long balnearioId;
        if (usuario.getTipo() == TipoUsuario.STAFF) {
            List<UsuarioBalnearioRol> membresias = usuarioBalnearioRolRepository.findByUsuarioId(usuario.getId());
            if (membresias.isEmpty()) {
                throw new ApiException(ErrorCode.TOKEN_INVALIDO);
            }
            rol = membresias.get(0).getRol().getCodigo();
            balnearioId = membresias.get(0).getBalnearioId();
        } else if (usuario.getTipo() == TipoUsuario.SUPER_ADMIN) {
            rol = RolCodigo.SUPER_ADMIN;
            balnearioId = null;
        } else {
            rol = RolCodigo.CLIENTE;
            balnearioId = null;
        }

        return issueTokensRotating(usuario, rol, balnearioId, sesion.getFamiliaId());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        String hash = jwtService.hashRefreshToken(request.refreshToken());
        sesionRefreshRepository.findByTokenHash(hash).ifPresent(sesion -> {
            sesion.setEstado(EstadoSesion.REVOCADA);
            sesion.setUpdatedAt(Instant.now());
            sesionRefreshRepository.save(sesion);
        });
    }

    private Usuario authenticateByTipo(LoginRequest request, TipoUsuario tipoEsperado) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENCIALES_INVALIDAS));
        if (usuario.getTipo() != tipoEsperado || usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ApiException(ErrorCode.CREDENCIALES_INVALIDAS);
        }
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new ApiException(ErrorCode.CREDENCIALES_INVALIDAS);
        }
        return usuario;
    }

    private TokenResponse issueTokens(Usuario usuario, RolCodigo rol, Long balnearioId) {
        return issueTokensRotating(usuario, rol, balnearioId, UlidGenerator.generate());
    }

    private TokenResponse issueTokensRotating(Usuario usuario, RolCodigo rol, Long balnearioId, String familiaId) {
        JwtService.AccessToken accessToken =
                jwtService.generateAccessToken(usuario.getPublicId(), usuario.getId(), usuario.getTipo(),
                        rol, balnearioId);

        String rawRefreshToken = jwtService.generateOpaqueRefreshToken();
        SesionRefresh sesion = new SesionRefresh();
        sesion.setUsuario(usuario);
        sesion.setFamiliaId(familiaId);
        sesion.setTokenHash(jwtService.hashRefreshToken(rawRefreshToken));
        sesion.setEstado(EstadoSesion.ACTIVA);
        sesion.setExpiraAt(Instant.now().plus(jwtService.refreshTtlFor(usuario.getTipo())));
        sesionRefreshRepository.save(sesion);

        return new TokenResponse(
                accessToken.value(),
                accessToken.expiresAt(),
                rawRefreshToken,
                usuario.getTipo().name(),
                rol.name(),
                balnearioId,
                usuario.isDebeCambiarPassword()
        );
    }

    /** Etapa 05 §1.1/§1.3: cambio de password revoca TODAS las sesiones del usuario. */
    @Transactional
    public void cambiarPassword(String usuarioPublicId, String passwordActual, String passwordNueva) {
        Usuario usuario = usuarioRepository.findByPublicId(usuarioPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO));
        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new ApiException(ErrorCode.CREDENCIALES_INVALIDAS, "La contraseña actual no coincide");
        }
        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        Instant ahora = Instant.now();
        List<SesionRefresh> sesionesActivas = sesionRefreshRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoSesion.ACTIVA);
        for (SesionRefresh sesion : sesionesActivas) {
            sesion.setEstado(EstadoSesion.REVOCADA);
            sesion.setUpdatedAt(ahora);
        }
        sesionRefreshRepository.saveAll(sesionesActivas);
    }
}
