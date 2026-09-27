package auca.ac.rw.user.dto;

import auca.ac.rw.user.domain.User;

public class UserMapper {

    private UserMapper() {}

    public static UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getRole(),
                user.getStudent() != null ? user.getStudent().getId() : null,
                user.getStaff() != null ? user.getStaff().getId() : null
        );
    }
}