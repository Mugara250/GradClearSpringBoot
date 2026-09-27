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
public class UserRegistrationDTO {

    private String password;
    private UserRole role;
    private UUID studentId;    // provide when role = STUDENT
    private UUID staffId;      // provide when role = DEPARTMENT_OFFICER or ADMIN
}