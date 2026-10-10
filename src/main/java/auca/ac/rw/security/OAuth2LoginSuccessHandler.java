package auca.ac.rw.security;

import auca.ac.rw.user.domain.User;
import auca.ac.rw.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
 * Anything else is refused. A Google login never creates a User, because a
 * GradClear user must be tied to a real Student/Staff record and a role,
 * which nothing in a Google profile can tell us.
 *
 * Either way the browser is sent back to the frontend's callback route:
 *   ${gradclear.frontend-url}${gradclear.frontend-callback-path}#token=<jwt>&expiresAt=<ISO-8601>
 *   ${gradclear.frontend-url}${gradclear.frontend-callback-path}#error=<message>
 * The values ride in the URL fragment, which browsers never send to a
 * server, so the token stays out of access logs and proxies.
 *
 * @version 2.0
 */
@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${gradclear.frontend-url}")
    private String frontendUrl;

    @Value("${gradclear.frontend-callback-path}")
    private String callbackPath;

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
            redirect(response, "error=" + encode("The Google account has no verified email address."));
            return;
        }

        Optional<User> linked = userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByStudent_EmailIgnoreCase(email));
        if (linked.isEmpty()) {
            redirect(response, "error=" + encode(
                    "No GradClear account is linked to " + email + ". Ask an administrator to link it."));
            return;
        }

        JwtService.IssuedToken issuedToken = jwtService.issue(linked.get());
        redirect(response, "token=" + encode(issuedToken.token())
                + "&expiresAt=" + encode(issuedToken.expiresAt().toString()));
    }

    private void redirect(HttpServletResponse response, String fragment) throws IOException {
        response.sendRedirect(frontendUrl + callbackPath + "#" + fragment);
    }

    // form-encoding, which the frontend's URLSearchParams decodes
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
