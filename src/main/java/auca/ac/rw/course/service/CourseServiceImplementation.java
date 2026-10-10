package auca.ac.rw.course.service;

import auca.ac.rw.course.domain.Course;
import auca.ac.rw.course.domain.CourseScope;
import auca.ac.rw.course.repository.CourseRepository;
import auca.ac.rw.exception.DuplicateResourceException;
import auca.ac.rw.exception.ResourceNotFoundException;
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
        validateUniqueCode(course);
        validateScope(course);
        return courseRepository.save(course);
    }

    @Override
    public Course update(Course course) {
        Course found = findById(course);
        validateUniqueCode(course);
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
                .orElseThrow(()->new ResourceNotFoundException("Course with id " + course.getId() + " not found"));
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

    // course_code is unique in the database too, but checking first gives a
    // clear 409 instead of a constraint violation surfacing as a 500. Ignores
    // case, so "CS101" and "cs101" count as the same course.
    private void validateUniqueCode(Course course) {
        courseRepository.findByCourseCodeIgnoreCase(course.getCourseCode())
                .filter(existing -> !existing.getId().equals(course.getId()))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A course with code " + existing.getCourseCode() + " already exists (" + existing.getCourseName() + ")");
                });
    }

    private void validateScope(Course course) {
        if (course.getCourseScope() == CourseScope.GENERAL && !course.getAcademics().isEmpty()) {
            throw new IllegalStateException("GENERAL courses must not be linked to specific Academic entries");
        }
        if (course.getCourseScope() == CourseScope.MAJOR && course.getAcademics().isEmpty()) {
            throw new IllegalStateException("SPECIFIC courses must be linked to at least one Academic entry");
        }
    }
}
