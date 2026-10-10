package auca.ac.rw.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The Class OAuth2UnavailableController.
 * Fallback for the "Sign in with Google" link when Google sign-in isn't set
 * up on this server (the "oauth2" profile / GOOGLE_CLIENT_ID aren't active).
 *
 * When it IS set up, Spring Security's OAuth2 redirect filter handles
 * /oauth2/authorization/{provider} before the request ever reaches a
 * controller, so this only runs in the not-configured case. Instead of an
 * unhandled 500, the browser goes back to the frontend's callback page with
 * an explanation, the same way other Google sign-in failures arrive there.
 *
 * @version 1.0
 */
@RestController
public class OAuth2UnavailableController {

    @Value("${gradclear.frontend-url}")
    private String frontendUrl;

    @Value("${gradclear.frontend-callback-path}")
    private String callbackPath;

    @GetMapping("/oauth2/authorization/{provider}")
    public void notConfigured(@PathVariable String provider, HttpServletResponse response) throws IOException {
        String message = "Sign-in with " + provider + " isn't enabled on this server yet. "
                + "Use your student or staff ID and password, or ask an administrator to set it up.";
        response.sendRedirect(frontendUrl + callbackPath + "#error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }
}
