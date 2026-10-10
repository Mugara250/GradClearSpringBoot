package auca.ac.rw.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * The Class TokenBlocklist.
 * Remembers the jti of tokens revoked through POST /api/v1/user/logout until
 * they would have expired anyway, so the blocklist never grows beyond the
 * set of still-live revoked tokens.
 *
 * Trade-off: this lives in memory. It is cleared on restart (a token logged
 * out before a restart becomes usable again until its exp) and is not shared
 * between instances. That is acceptable for a single-instance deployment with
 * 24h tokens; running several instances, or needing revocation to survive
 * restarts, would mean moving this to a Postgres table (jti, expires_at) or
 * Redis with the same TTL rule.
 *
 * @version 1.0
 */
@Component
public class TokenBlocklist {

    private final Cache<String, Instant> revoked = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Instant>() {
                @Override
                public long expireAfterCreate(String tokenId, Instant expiresAt, long currentTime) {
                    return remainingNanos(expiresAt);
                }

                @Override
                public long expireAfterUpdate(String tokenId, Instant expiresAt, long currentTime, long currentDuration) {
                    return remainingNanos(expiresAt);
                }

                @Override
                public long expireAfterRead(String tokenId, Instant expiresAt, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId != null && expiresAt != null && expiresAt.isAfter(Instant.now())) {
            revoked.put(tokenId, expiresAt);
        }
    }

    public boolean isRevoked(String tokenId) {
        return tokenId != null && revoked.getIfPresent(tokenId) != null;
    }

    private static long remainingNanos(Instant expiresAt) {
        return Math.max(0, Duration.between(Instant.now(), expiresAt).toNanos());
    }
}
