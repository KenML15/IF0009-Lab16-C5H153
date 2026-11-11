package ucr.ac.cr.medivault.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ucr.ac.cr.medivault.business.SecurityAuditorService;
import ucr.ac.cr.medivault.security.RateLimitingFilter;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalSecurityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalSecurityExceptionHandler.class);

    private final SecurityAuditorService auditor;

    public GlobalSecurityExceptionHandler(SecurityAuditorService auditor) {
        this.auditor = auditor;
    }

    @ExceptionHandler(AccessDeniedSecurityException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedSecurityException ex,
                                            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null) ? auth.getName() : "anonimo";
        String uri = request.getRequestURI();
        // Solo la URL del recurso; nunca datos clinicos
        auditor.registrar(user, RateLimitingFilter.extractClientIp(request),
                "IDOR_ATTEMPT_BLOCKED", uri, 403, "Acceso denegado a " + uri);
        return build(HttpStatus.FORBIDDEN, "Acceso Prohibido (IDOR / BOLA)",
                "forbidden", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleSpringAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Acceso Prohibido",
                "forbidden", "No tiene permisos para esta operacion.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Recurso No Encontrado",
                "not-found", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex,
                                              HttpServletRequest request) {
        auditor.registrar("desconocido", RateLimitingFilter.extractClientIp(request),
                "LOGIN_FAILED", request.getRequestURI(), 401, "Credenciales invalidas");
        return build(HttpStatus.UNAUTHORIZED, "Fallo de Autenticacion",
                "unauthorized", "Credenciales invalidas.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Solicitud Mal Formada",
                "bad-request", "Cuerpo de la peticion invalido.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail p = build(HttpStatus.BAD_REQUEST, "Error de Validacion",
                "bad-request", "Validacion de datos fallida.");
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        p.setProperty("invalid_fields", errors);
        return p;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Error interno no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error Interno",
                "internal", "Error interno del servidor.");
    }

    private ProblemDetail build(HttpStatus status, String title,
                                String typeSlug, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        p.setType(URI.create("https://medivault.ucr.ac.cr/" + typeSlug));
        return p;
    }
}