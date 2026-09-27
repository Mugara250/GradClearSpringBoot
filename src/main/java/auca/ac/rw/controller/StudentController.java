package auca.ac.rw.controller;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.service.AcademicService;
import auca.ac.rw.response.ApiResponse;
import auca.ac.rw.student.domain.Student;
import auca.ac.rw.student.dto.StudentDTO;
import auca.ac.rw.student.dto.StudentMapper;
import auca.ac.rw.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/student")
public class StudentController {

    private final StudentService studentService;
    private final AcademicService academicService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<StudentDTO>> findAll() {
        List<StudentDTO> students = studentService.findAll().stream()
                .map(StudentMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Students retrieved successfully",
                students
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StudentDTO> findById(@PathVariable UUID id) {
        Student student = new Student();
        student.setId(id);
        Student found = studentService.findById(student);
        return ApiResponse.of(
                "Student retrieved successfully",
                StudentMapper.toDTO(found)
        );
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentDTO> register(@RequestBody StudentDTO studentDTO) {
        Student student = buildEntityFromDTO(studentDTO);
        Student saved = studentService.register(student);
        return ApiResponse.of(
                "Student created successfully",
                StudentMapper.toDTO(saved)
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StudentDTO> update(@PathVariable UUID id, @RequestBody StudentDTO studentDTO) {
        Student student = buildEntityFromDTO(studentDTO);
        student.setId(id);
        Student updated = studentService.update(student);
        return ApiResponse.of(
                "Student updated successfully",
                StudentMapper.toDTO(updated)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        Student student = new Student();
        student.setId(id);
        studentService.delete(student);
        return ApiResponse.of("Student deleted successfully", null);
    }

    // resolves academicId -> an actual, validated Academic (throws 404 if missing)
    private Student buildEntityFromDTO(StudentDTO dto) {
        Student student = new Student();
        student.setStudentId(dto.getStudentId());
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setEmail(dto.getEmail());
        student.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getAcademicId() != null) {
            Academic academicRef = new Academic();
            academicRef.setId(dto.getAcademicId());
            student.setAcademic(academicService.findById(academicRef));
        }
        return student;
    }
}
