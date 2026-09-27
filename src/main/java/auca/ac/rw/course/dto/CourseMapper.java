package auca.ac.rw.course.dto;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.course.domain.Course;

import java.util.stream.Collectors;

public class CourseMapper {

    private CourseMapper() {}

    public static CourseDTO toDTO(Course course) {
        return new CourseDTO(
                course.getId(),
                course.getCourseCode(),
                course.getCourseName(),
                course.getCreditHours(),
                course.getCourseScope(),
                course.getAcademics().stream().map(Academic::getId).collect(Collectors.toList())
        );
    }
}