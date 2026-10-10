package auca.ac.rw.security;

import auca.ac.rw.response.ApiResponse;
import auca.ac.rw.user.domain.User;
import auca.ac.rw.user.dto.UserMapper;
import auca.ac.rw.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

/**
 * The Class OAuth2LoginSuccessHandler.
 * Runs once Google has authenticated the browser. Turns the Google identity
 * into a GradClear JWT — the exact same kind UserController.login() issues —
 * so from here on the caller is indistinguishable from a local-login user:
 * JwtAuthenticationFilter and every @PreAuthorize rule treat both the same.
 *
 * Linking rule (pre-linked accounts only — no just-in-time provisioning):
 * the Google account's *verified* email must match either
 *   1. User.email, set by an ADMIN when creating/updating the user, or
 *   2. the email on the Student record a STUDENT user is linked to.
 * Anything else gets 403. A Google login never creates a User, because a
 * GradClear user must be tied to a real Student/Staff record and a role,
 * which nothing in a Google profile can tell us.
 *
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final SecurityErrorWriter securityErrorWriter;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // the short-lived session only existed to carry OAuth2 state through
        // the Google redirect; the API itself stays stateless
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        Boolean emailVerified = oauth2User.getAttribute("email_verified");

        if (email == null || !Boolean.TRUE.equals(emailVerified)) {
            securityErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "Unauthorized",
                    "The Google account has no verified email address");
            return;
        }

        Optional<User> linked = userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByStudent_EmailIgnoreCase(email));
        if (linked.isEmpty()) {
            securityErrorWriter.write(response, HttpStatus.FORBIDDEN, "Forbidden",
                    "No GradClear account is linked to " + email + ". Ask an administrator to link it.");
            return;
        }

        User user = linked.get();
        JwtService.IssuedToken issuedToken = jwtService.issue(user);

        // TODO(frontend): once a frontend exists, redirect to its callback
        // route instead (e.g. 302 to ${FRONTEND_URL}/auth/callback#token=...,
        // using the URL fragment so the token stays out of server logs).
        // Until then the token is returned directly as JSON, in the same
        // shape as POST /api/v1/user/login.
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.of("Login successful", UserMapper.toDTO(user, issuedToken)));
    }
}
