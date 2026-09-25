package auca.ac.rw.course.service;

import auca.ac.rw.course.domain.Course;
import auca.ac.rw.course.domain.CourseScope;
import auca.ac.rw.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CourseServiceImplementation implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    public Course register(Course course) {
        validateScope(course);
        return courseRepository.save(course);
    }

    @Override
    public Course update(Course course) {
        Course found = findById(course);
        found.setCourseCode(course.getCourseCode());
        found.setCourseName(course.getCourseName());
        found.setCreditHours(course.getCreditHours());
        found.setCourseScope(course.getCourseScope());
        found.setAcademics(course.getAcademics());
        validateScope(found);
        return courseRepository.save(found);
    }

    @Override
    public void delete(Course course) {
        courseRepository.delete(course);
    }

    @Override
    public Course findById(Course course) {
        return courseRepository.findById(course.getId())
                .orElseThrow(()->new RuntimeException("Course with id " + course.getId() + " not found"));
    }

    @Override
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    @Override
    public List<Course> findByScope(CourseScope scope) {
        return courseRepository.findByCourseScope(scope);
    }

    @Override
    public List<Course> findByAcademic(UUID academicId) {
        return courseRepository.findByAcademicsId(academicId);
    }

    private void validateScope(Course course) {
        if (course.getCourseScope() == CourseScope.GENERAL && !course.getAcademics().isEmpty()) {
            throw new IllegalStateException("GENERAL courses must not be linked to specific Academic entries");
        }
        if (course.getCourseScope() == CourseScope.SPECIFIC && course.getAcademics().isEmpty()) {
            throw new IllegalStateException("SPECIFIC courses must be linked to at least one Academic entry");
        }
    }
}
