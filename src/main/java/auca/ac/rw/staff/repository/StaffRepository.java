package auca.ac.rw.staff.repository;

import auca.ac.rw.staff.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {
    Optional<Staff> findStaffByStaffId(String staffId);
    List<Staff> findByDepartmentsId(UUID departmentId);

    // pulls the next value from staff_id_seq (see schema.sql) — atomic,
    // no risk of two concurrent registrations colliding on the same number.
    @Query(value = "SELECT nextval('staff_id_seq')", nativeQuery = true)
    Long getNextStaffIdSequence();
}
