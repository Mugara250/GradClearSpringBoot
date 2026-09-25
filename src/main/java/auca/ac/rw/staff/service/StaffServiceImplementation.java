package auca.ac.rw.staff.service;

import auca.ac.rw.staff.domain.Staff;
import auca.ac.rw.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class StaffServiceImplementation implements StaffService{

    private final StaffRepository staffRepository;

    @Override
    public Staff register(Staff staff) {
        return staffRepository.save(staff);
    }

    @Override
    public Staff update(Staff staff) {
        Staff found = findById(staff);
        found.setStaffId(staff.getStaffId());
        found.setFirstName(staff.getFirstName());
        found.setLastName(staff.getLastName());
        found.setPosition(staff.getPosition());
        found.setDepartments(staff.getDepartments());

        return staffRepository.save(found);
    }

    @Override
    public void delete(Staff staff) {
        staffRepository.delete(staff);
    }

    @Override
    public Staff findById(Staff staff) {
        return staffRepository.findById(staff.getId())
                .orElseThrow(()->new RuntimeException("Staff with id" + staff.getId() + " not found"));
    }

    @Override
    public List<Staff> findAll() {
        return staffRepository.findAll();
    }

    @Override
    public Staff findByStaffId(String staffId) {
        return staffRepository.findStaffByStaffId(staffId)
                .orElseThrow(()-> new RuntimeException("Staff with staffId " + staffId + " not found"));
    }

    @Override
    public List<Staff> findByDepartment(UUID departmentId) {
        return staffRepository.findByDepartmentsId(departmentId);
    }
}
