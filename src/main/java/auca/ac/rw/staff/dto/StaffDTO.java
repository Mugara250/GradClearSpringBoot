package auca.ac.rw.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffDTO {
    private UUID id;
    private String staffId;
    private String firstName;
    private String lastName;
    private String position;
    private Set<UUID> departmentIds;
}
