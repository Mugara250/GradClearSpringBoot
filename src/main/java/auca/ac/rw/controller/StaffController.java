package auca.ac.rw.controller;

import auca.ac.rw.department.domain.Department;
import auca.ac.rw.department.service.DepartmentService;
import auca.ac.rw.response.ApiResponse;
import auca.ac.rw.staff.domain.Staff;
import auca.ac.rw.staff.dto.StaffDTO;
import auca.ac.rw.staff.dto.StaffMapper;
import auca.ac.rw.staff.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffService staffService;
    private final DepartmentService departmentService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<StaffDTO>> findAll() {
        List<StaffDTO> staffList = staffService.findAll().stream()
                .map(StaffMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Staff retrieved successfully",
                staffList
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StaffDTO> findById(@PathVariable UUID id) {
        Staff staff = new Staff();
        staff.setId(id);
        Staff found = staffService.findById(staff);
        return ApiResponse.of(
                "Staff retrieved successfully",
                StaffMapper.toDTO(found)
        );
    }

    @GetMapping("/by-staff-id/{staffId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StaffDTO> findByStaffId(@PathVariable String staffId) {
        Staff found = staffService.findByStaffId(staffId);
        return ApiResponse.of(
                "Staff retrieved successfully",
                StaffMapper.toDTO(found)
        );
    }

    @GetMapping("/department/{departmentId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<StaffDTO>> findByDepartment(@PathVariable UUID departmentId) {
        List<StaffDTO> staffList = staffService.findByDepartment(departmentId).stream()
                .map(StaffMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Staff retrieved successfully",
                staffList
        );
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffDTO> register(@RequestBody StaffDTO staffDTO) {
        Staff staff = buildEntityFromDTO(staffDTO);
        Staff saved = staffService.register(staff);
        return ApiResponse.of(
                "Staff created successfully",
                StaffMapper.toDTO(saved)
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StaffDTO> update(@PathVariable UUID id, @RequestBody StaffDTO staffDTO) {
        Staff staff = buildEntityFromDTO(staffDTO);
        staff.setId(id);
        Staff updated = staffService.update(staff);
        return ApiResponse.of(
                "Staff updated successfully",
                StaffMapper.toDTO(updated)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        Staff staff = new Staff();
        staff.setId(id);
        staffService.delete(staff);
        return ApiResponse.of("Staff deleted successfully", null);
    }

    // resolves each departmentId -> an actual, validated Department (throws 404 if missing)
    private Staff buildEntityFromDTO(StaffDTO dto) {
        Staff staff = new Staff();
        staff.setStaffId(dto.getStaffId());
        staff.setFirstName(dto.getFirstName());
        staff.setLastName(dto.getLastName());
        staff.setPosition(dto.getPosition());

        Set<Department> departments = new HashSet<>();
        if (dto.getDepartmentIds() != null) {
            for (UUID departmentId : dto.getDepartmentIds()) {
                Department departmentRef = new Department();
                departmentRef.setId(departmentId);
                departments.add(departmentService.findById(departmentRef));
            }
        }
        staff.setDepartments(departments);
        return staff;
    }
}
