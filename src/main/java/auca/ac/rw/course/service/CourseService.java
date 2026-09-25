package auca.ac.rw.course.service;

import auca.ac.rw.course.domain.Course;
import auca.ac.rw.course.domain.CourseScope;

import java.util.List;
import java.util.UUID;

public interface CourseService {
    Course register(Course course);
    Course update(Course course);
    void delete(Course course);
    Course findById(Course course);
    List<Course> findAll();

    List<Course> findByScope(CourseScope scope);
    List<Course> findByAcademic(UUID academicId);
}
