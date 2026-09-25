package auca.ac.rw.department.service;

import auca.ac.rw.department.domain.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentService {
    Department register(Department department);
    Department update(Department department);
    void delete(Department department);
    Department findById(Department department);
    List<Department> findAll();
//    Optional<Department> findByName(String name);
}
