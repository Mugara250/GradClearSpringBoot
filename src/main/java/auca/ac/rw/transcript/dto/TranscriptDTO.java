package auca.ac.rw.transcript.dto;

import auca.ac.rw.transcript.domain.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptDTO {

    private UUID id;
    private Double grade;
    private CourseStatus status;
    private String academicYear;
    private UUID studentId;
    private UUID courseId;
}