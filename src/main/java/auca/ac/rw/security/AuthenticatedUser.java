package auca.ac.rw.security;

import auca.ac.rw.user.domain.User;
import auca.ac.rw.user.domain.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The Class AuthenticatedUser.
 * The principal JwtAuthenticationFilter puts in the SecurityContext. Built
 * from the User row as it is *now* (not from the token's role claim), so a
 * role change or account deletion takes effect on the very next request.
 *
 * @param userId     User id (the token's sub)
 * @param role       the user's current role
 * @param studentId  linked Student's id, or null
 * @param staffId    linked Staff's id, or null
 * @param tokenId    the token's jti — what logout revokes
 * @param expiresAt  the token's exp — how long the revocation has to be remembered
 * @version 1.0
 */
public record AuthenticatedUser(UUID userId, UserRole role, UUID studentId, UUID staffId,
                                String tokenId, Instant expiresAt) {

    public static AuthenticatedUser of(User user, String tokenId, Instant expiresAt) {
        return new AuthenticatedUser(
                user.getId(),
                user.getRole(),
                user.getStudent() != null ? user.getStudent().getId() : null,
                user.getStaff() != null ? user.getStaff().getId() : null,
                tokenId,
                expiresAt
        );
    }

    // ROLE_ prefix so hasRole('ADMIN') etc. work
    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
