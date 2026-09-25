package auca.ac.rw.staff.service;

import auca.ac.rw.staff.domain.Staff;

import java.util.List;
import java.util.UUID;

public interface StaffService {
    Staff register(Staff staff);
    Staff update(Staff staff);
    void delete(Staff staff);
    Staff findById(Staff staff);
    List<Staff> findAll();
    Staff findByStaffId(String staffId);
    List<Staff> findByDepartment(UUID departmentId);
}
