package auca.ac.rw.student.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {

    private UUID id;
    private String studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UUID academicId;
    private Boolean capstoneDefended;
}