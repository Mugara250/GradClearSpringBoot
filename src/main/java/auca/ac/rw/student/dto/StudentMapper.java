package auca.ac.rw.student.dto;


import auca.ac.rw.student.domain.Student;

public class StudentMapper {

    private StudentMapper() {}

    public static StudentDTO toDTO(Student student) {
        return new StudentDTO(
                student.getId(),
                student.getStudentId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getPhoneNumber(),
                student.getAcademic() != null ? student.getAcademic().getId() : null
        );
    }
}