package auca.ac.rw.department.repository;

import auca.ac.rw.department.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findDepartmentByName(String name);

}
