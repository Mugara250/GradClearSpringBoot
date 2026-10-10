package auca.ac.rw.security;

import auca.ac.rw.user.domain.User;
import auca.ac.rw.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * The Class JwtAuthenticationFilter.
 * Reads `Authorization: Bearer <token>`, validates it through JwtService,
 * checks it hasn't been logged out, reloads the User it names and puts an
 * AuthenticatedUser into the SecurityContext for the rest of the request.
 *
 * No header -> passes through untouched (the authorization rules then decide
 * whether anonymous access is fine). A header that is present but bad
 * (malformed, wrong signature, expired, revoked, user gone) -> 401 right
 * here, rather than silently treating the caller as anonymous.
 *
 * Deliberately not a @Component: Spring Boot would otherwise also register
 * it as a plain servlet filter and run it twice. SecurityConfig adds it to
 * the security chain directly.
 *
 * @version 1.0
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlocklist tokenBlocklist;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthenticatedUser principal = authenticate(header.substring(BEARER_PREFIX.length()).trim());

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, ex);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private AuthenticatedUser authenticate(String token) {
        Jwt jwt;
        UUID userId;
        try {
            jwt = jwtService.decode(token);
            userId = UUID.fromString(jwt.getSubject());
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            throw new BadCredentialsException("Invalid or expired token", ex);
        }

        if (tokenBlocklist.isRevoked(jwt.getId())) {
            throw new BadCredentialsException("Token has been revoked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Account no longer exists"));
        return AuthenticatedUser.of(user, jwt.getId(), jwt.getExpiresAt());
    }
}
