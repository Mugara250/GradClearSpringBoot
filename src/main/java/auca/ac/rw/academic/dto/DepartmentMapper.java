package auca.ac.rw.academic.dto;


import auca.ac.rw.department.domain.Department;
import auca.ac.rw.department.dto.DepartmentDTO;

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