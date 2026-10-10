package auca.ac.rw.user.dto;

import auca.ac.rw.user.domain.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
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
    private String email;

    // only filled in on login responses (local or OAuth2); left out of the
    // JSON everywhere else (/me, user listings) rather than sent as null
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant tokenExpiresAt;
}