package auca.ac.rw.staff.dto;

import auca.ac.rw.department.domain.Department;
import auca.ac.rw.staff.domain.Staff;

import java.util.stream.Collectors;

public class StaffMapper {

    private StaffMapper() {}

    public static StaffDTO toDTO(Staff staff) {
        return new StaffDTO(
                staff.getId(),
                staff.getStaffId(),
                staff.getFirstName(),
                staff.getLastName(),
                staff.getPosition(),
                staff.getDepartments().stream().map(Department::getId).collect(Collectors.toSet())
        );
    }
}