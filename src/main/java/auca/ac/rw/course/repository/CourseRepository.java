package auca.ac.rw.course.repository;

import auca.ac.rw.course.domain.Course;
import auca.ac.rw.course.domain.CourseScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    Optional<Course> findByCourseCode(String courseCode);

    List<Course> findByCourseScope(CourseScope courseScope);

    List<Course> findByAcademicsId(UUID academicsId);
}
