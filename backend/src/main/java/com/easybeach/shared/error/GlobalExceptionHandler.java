package com.easybeach.shared.error;

import com.easybeach.shared.tenancy.TenantContextMissingException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Formato de error único (etapa 04 §1.3, RFC 7807 /
 * {@code application/problem+json}): type/title/status/detail/instance +
 * {@code code} de negocio estable + {@code errors[]} para validaciones.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI TYPE_BASE = URI.create("https://easybeach.dev/errors/");

    public record FieldErrorDetail(String field, String message) {
    }

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.errorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDACION_FALLIDA, ErrorCode.VALIDACION_FALLIDA.defaultDetail(), request, errors);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(ErrorCode.TOKEN_INVALIDO, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.ACCESO_DENEGADO, ErrorCode.ACCESO_DENEGADO.defaultDetail(), request, null);
    }

    @ExceptionHandler(TenantContextMissingException.class)
    public ProblemDetail handleTenantMissing(TenantContextMissingException ex, HttpServletRequest request) {
        log.error("TenantContext ausente en operación tenant-scoped", ex);
        return build(ErrorCode.ERROR_INESPERADO, "Error interno de resolución de tenant", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado", ex);
        return build(ErrorCode.ERROR_INESPERADO, ErrorCode.ERROR_INESPERADO.defaultDetail(), request, null);
    }

    private ProblemDetail build(ErrorCode code, String detail, HttpServletRequest request,
                                 List<FieldErrorDetail> errors) {
        HttpStatus status = code.status();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(TYPE_BASE.resolve(code.name().toLowerCase().replace('_', '-')));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code.name());
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }
}
