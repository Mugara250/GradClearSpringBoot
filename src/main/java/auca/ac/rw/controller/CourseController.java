package auca.ac.rw.controller;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.service.AcademicService;
import auca.ac.rw.course.domain.Course;
import auca.ac.rw.course.domain.CourseScope;
import auca.ac.rw.course.dto.CourseDTO;
import auca.ac.rw.course.dto.CourseMapper;
import auca.ac.rw.course.service.CourseService;
import auca.ac.rw.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/course")
public class CourseController {

    private final CourseService courseService;
    private final AcademicService academicService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<CourseDTO>> findAll() {
        List<CourseDTO> courses = courseService.findAll().stream()
                .map(CourseMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Courses retrieved successfully",
                courses
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CourseDTO> findById(@PathVariable UUID id) {
        Course course = new Course();
        course.setId(id);
        Course found = courseService.findById(course);
        return ApiResponse.of(
                "Course retrieved successfully",
                CourseMapper.toDTO(found)
        );
    }

    @GetMapping("/scope/{scope}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<CourseDTO>> findByScope(@PathVariable CourseScope scope) {
        List<CourseDTO> courses = courseService.findByScope(scope).stream()
                .map(CourseMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Courses retrieved successfully",
                courses
        );
    }

    @GetMapping("/academic/{academicId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<CourseDTO>> findByAcademic(@PathVariable UUID academicId) {
        List<CourseDTO> courses = courseService.findByAcademic(academicId).stream()
                .map(CourseMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Courses retrieved successfully",
                courses
        );
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseDTO> register(@RequestBody CourseDTO courseDTO) {
        Course course = buildEntityFromDTO(courseDTO);
        Course saved = courseService.register(course);
        return ApiResponse.of(
                "Course created successfully",
                CourseMapper.toDTO(saved)
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CourseDTO> update(@PathVariable UUID id, @RequestBody CourseDTO courseDTO) {
        Course course = buildEntityFromDTO(courseDTO);
        course.setId(id);
        Course updated = courseService.update(course);
        return ApiResponse.of(
                "Course updated successfully",
                CourseMapper.toDTO(updated)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        Course course = new Course();
        course.setId(id);
        courseService.delete(course);
        return ApiResponse.of("Course deleted successfully", null);
    }

    // resolves each academicId -> an actual, validated Academic (throws 404 if missing)
    private Course buildEntityFromDTO(CourseDTO dto) {
        Course course = new Course();
        course.setCourseCode(dto.getCourseCode());
        course.setCourseName(dto.getCourseName());
        course.setCreditHours(dto.getCreditHours());
        course.setCourseScope(dto.getScope());

        Set<Academic> academics = new HashSet<>();
        if (dto.getAcademicIds() != null) {
            for (UUID academicId : dto.getAcademicIds()) {
                Academic academicRef = new Academic();
                academicRef.setId(academicId);
                academics.add(academicService.findById(academicRef));
            }
        }
        course.setAcademics(academics);
        return course;
    }
}
