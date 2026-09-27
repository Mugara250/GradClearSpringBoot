package auca.ac.rw.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The Class LoginRequestDTO.
 * loginId is the student's studentId (reg number) or the staff's staffId —
 * UserRepository resolves it through whichever relation matches.
 *
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
    private String loginId;
    private String password;
}
