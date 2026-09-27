package auca.ac.rw.controller;

import auca.ac.rw.department.domain.Department;
import auca.ac.rw.department.dto.DepartmentDTO;
import auca.ac.rw.department.dto.DepartmentMapper;
import auca.ac.rw.department.service.DepartmentService;
import auca.ac.rw.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/department")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<DepartmentDTO>> findAll() {
        List<DepartmentDTO> departments = departmentService.findAll().stream()
                .map(DepartmentMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Departments retrieved successfully",
                departments
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<DepartmentDTO> findById(@PathVariable UUID id) {
        Department department = new Department();
        department.setId(id);
        Department found = departmentService.findById(department);
        return ApiResponse.of(
                "Department retrieved successfully",
                DepartmentMapper.toDTO(found)
        );
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DepartmentDTO> register(@RequestBody DepartmentDTO departmentDTO) {
        Department department = buildEntityFromDTO(departmentDTO);
        Department saved = departmentService.register(department);
        return ApiResponse.of(
                "Department created successfully",
                DepartmentMapper.toDTO(saved)
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<DepartmentDTO> update(@PathVariable UUID id, @RequestBody DepartmentDTO departmentDTO) {
        Department department = buildEntityFromDTO(departmentDTO);
        department.setId(id);
        Department updated = departmentService.update(department);
        return ApiResponse.of(
                "Department updated successfully",
                DepartmentMapper.toDTO(updated)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        Department department = new Department();
        department.setId(id);
        departmentService.delete(department);
        return ApiResponse.of("Department deleted successfully", null);
    }

    private Department buildEntityFromDTO(DepartmentDTO dto) {
        Department department = new Department();
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        return department;
    }
}
