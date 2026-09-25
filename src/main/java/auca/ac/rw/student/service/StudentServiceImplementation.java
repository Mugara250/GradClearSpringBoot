package auca.ac.rw.student.service;

import auca.ac.rw.student.domain.Student;
import auca.ac.rw.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class StudentServiceImplementation implements StudentService{

    private final StudentRepository studentRepository;

    @Override
    public Student register(Student student) {
        return studentRepository.save(student);
    }

    @Override
    public Student update(Student student) {
        Student found = findById(student);
        found.setStudentId(student.getStudentId());
        found.setFirstName(student.getFirstName());
        found.setLastName(student.getLastName());
        found.setEmail(student.getEmail());
        found.setPhoneNumber(student.getPhoneNumber());
        found.setAcademic(student.getAcademic());
        return studentRepository.save(student);
    }

    @Override
    public void delete(Student student) {
        studentRepository.delete(student);
    }

    @Override
    public Student findById(Student student) {
        return studentRepository.findById(student.getId())
                .orElseThrow(()->new RuntimeException("Student with id " + student.getId() + " not found"));
    }

    @Override
    public List<Student> findAll() {
        return studentRepository.findAll();
    }
}
