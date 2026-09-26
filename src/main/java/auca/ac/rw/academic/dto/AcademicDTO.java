package auca.ac.rw.academic.dto;

import auca.ac.rw.academic.domain.AcademicLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicDTO {

    private UUID id;
    private String name;
    private AcademicLevel level;
    private UUID parentId;
    private Integer requiredCredits;
}
