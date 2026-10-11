package auca.ac.rw.user.dto;

import auca.ac.rw.security.JwtService;
import auca.ac.rw.user.domain.User;

public class UserMapper {

    private UserMapper() {}

    public static UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getRole(),
                user.getStudent() != null ? user.getStudent().getId() : null,
                user.getStaff() != null ? user.getStaff().getId() : null,
                user.getEmail(),
                null,
                null,
                null
        );
    }

    // login responses: same fields plus the freshly issued access token
    public static UserDTO toDTO(User user, JwtService.IssuedToken issuedToken) {
        UserDTO dto = toDTO(user);
        dto.setToken(issuedToken.token());
        dto.setTokenExpiresAt(issuedToken.expiresAt());
        return dto;
    }
}