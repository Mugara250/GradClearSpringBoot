package auca.ac.rw.staff.repository;

import auca.ac.rw.staff.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {
    Optional<Staff> findStaffByStaffId(String staffId);
    List<Staff> findByDepartmentsId(UUID departmentId);
}
