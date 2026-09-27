package auca.ac.rw.user.dto;

import auca.ac.rw.user.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private UUID id;
    private UserRole role;
    private UUID studentId;
    private UUID staffId;
}