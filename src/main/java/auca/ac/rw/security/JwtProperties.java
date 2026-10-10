package auca.ac.rw.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The Class JwtProperties.
 * Bound from gradclear.jwt.* in application.properties. The secret is read
 * from the JWT_SECRET environment variable when set; the value committed in
 * application.properties is a development-only fallback.
 *
 * @param secret         HS256 signing key, at least 32 bytes (256 bits)
 * @param accessTokenTtl how long an issued access token stays valid
 * @param issuer         value written to / required in the `iss` claim
 * @version 1.0
 */
@ConfigurationProperties(prefix = "gradclear.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, String issuer) {
}
