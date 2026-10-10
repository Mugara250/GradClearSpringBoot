package auca.ac.rw.security;

import auca.ac.rw.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * The Class SecurityErrorWriter.
 * Writes 401/403 failures that happen inside the security filter chain
 * (before any controller, so GlobalExceptionHandler never sees them) in the
 * same ErrorResponse JSON shape the rest of the API uses, instead of Spring's
 * default empty body / HTML error page.
 *
 * Used as the chain's AuthenticationEntryPoint (no/invalid token -> 401) and
 * AccessDeniedHandler (URL rule refuses the role -> 403).
 *
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class SecurityErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "Unauthorized",
                authException.getMessage() != null ? authException.getMessage() : "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource");
    }

    public void write(HttpServletResponse response, HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(status.value(), error, message));
    }
}
