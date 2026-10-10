package auca.ac.rw.security;

import auca.ac.rw.user.domain.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * The Class JwtService.
 * Issues and validates the HS256 access tokens used by every authenticated
 * request. Both entry points — local login (UserController.login) and Google
 * login (OAuth2LoginSuccessHandler) — call issue(), so the rest of the app
 * never needs to know which one a token came from.
 *
 * Claims: sub = User id, role = UserRole name, jti = random token id (used by
 * logout's blocklist), iss, iat, exp.
 *
 * @version 1.0
 */
@Service
public class JwtService {

    public static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtService(JwtProperties properties) {
        this.properties = properties;

        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("gradclear.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");

        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));

        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // default validators check exp/nbf (60s clock skew); also pin the issuer
        nimbusDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        this.decoder = nimbusDecoder;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    // throws JwtException (BadJwtException / JwtValidationException) when the
    // signature, issuer or expiry don't check out
    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
