package auca.ac.rw.clearance_request.dto;

import auca.ac.rw.clearance_request.domain.ClearanceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClearanceRequestDTO {

    private UUID id;
    private UUID studentId;
    private UUID departmentId;
    private ClearanceStatus status;
    private String remarks;
    private LocalDate requestDate;
}