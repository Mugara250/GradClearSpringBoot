package auca.ac.rw.course.dto;

import auca.ac.rw.course.domain.CourseScope;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {

    private UUID id;
    private String courseCode;
    private String courseName;
    private Integer creditHours;
    private CourseScope scope;
    private List<UUID> academicIds;
}