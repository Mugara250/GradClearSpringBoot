package auca.ac.rw.student.service;

import auca.ac.rw.student.domain.Student;
import org.springframework.stereotype.Service;

import java.util.List;

public interface StudentService {
    Student register(Student student);
    Student update(Student student);
    void delete(Student student);
    Student findById(Student student);
    List<Student> findAll();
}
