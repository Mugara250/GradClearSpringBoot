package auca.ac.rw.user.repository;

import auca.ac.rw.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByStudent_StudentId(String studentStudentId);

    Optional<User> findByStaff_StaffId(String staffStaffId);

}
