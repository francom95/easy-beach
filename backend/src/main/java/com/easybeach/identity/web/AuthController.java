package com.easybeach.identity.web;

import com.easybeach.identity.service.AuthService;
import com.easybeach.identity.web.dto.CambiarPasswordRequest;
import com.easybeach.identity.web.dto.LoginRequest;
import com.easybeach.identity.web.dto.RefreshRequest;
import com.easybeach.identity.web.dto.RegistroClienteRequest;
import com.easybeach.identity.web.dto.TokenResponse;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registro(@Valid @RequestBody RegistroClienteRequest request) {
        return authService.registrarCliente(request);
    }

    @PostMapping("/login/cliente")
    public TokenResponse loginCliente(@Valid @RequestBody LoginRequest request) {
        return authService.loginCliente(request);
    }

    @PostMapping("/login/staff")
    public TokenResponse loginStaff(@Valid @RequestBody LoginRequest request) {
        return authService.loginStaff(request);
    }

    @PostMapping("/login/super-admin")
    public TokenResponse loginSuperAdmin(@Valid @RequestBody LoginRequest request) {
        return authService.loginSuperAdmin(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /** Requiere autenticación (a diferencia del resto de /auth/**) - ver SecurityConfig. */
    @PutMapping("/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request,
                                                 @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        authService.cambiarPassword(principal.usuarioPublicId(), request.passwordActual(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }
}
