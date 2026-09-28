package auca.ac.rw.student.repository;

import auca.ac.rw.student.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByStudentId(String studentId);
    // pulls the next value from student_id_seq (see schema.sql) — the
    // database hands this out atomically, so two concurrent registrations
    // can never receive the same number.
    @Query(value = "SELECT nextval('student_id_seq')", nativeQuery = true)
    Long getNextStudentIdSequence();
}
