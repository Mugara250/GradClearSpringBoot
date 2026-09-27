package auca.ac.rw.department.dto;

import auca.ac.rw.department.domain.Department;

public class DepartmentMapper {

    private DepartmentMapper() {}

    public static DepartmentDTO toDTO(Department department) {
        return new DepartmentDTO(
                department.getId(),
                department.getName(),
                department.getDescription()
        );
    }
}
