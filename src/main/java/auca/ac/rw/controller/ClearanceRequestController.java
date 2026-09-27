package auca.ac.rw.controller;

import auca.ac.rw.clearance_request.domain.ClearanceRequest;
import auca.ac.rw.clearance_request.dto.ClearanceRequestDTO;
import auca.ac.rw.clearance_request.dto.ClearanceRequestMapper;
import auca.ac.rw.clearance_request.service.ClearanceRequestService;
import auca.ac.rw.response.ApiResponse;
import auca.ac.rw.student.domain.Student;
import auca.ac.rw.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/clearance-request")
public class ClearanceRequestController {

    private final ClearanceRequestService clearanceRequestService;
    private final StudentService studentService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<ClearanceRequestDTO>> findAll() {
        List<ClearanceRequestDTO> requests = clearanceRequestService.findAll().stream()
                .map(ClearanceRequestMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Clearance requests retrieved successfully",
                requests
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ClearanceRequestDTO> findById(@PathVariable UUID id) {
        ClearanceRequest request = new ClearanceRequest();
        request.setId(id);
        ClearanceRequest found = clearanceRequestService.findById(request);
        return ApiResponse.of(
                "Clearance request retrieved successfully",
                ClearanceRequestMapper.toDTO(found)
        );
    }

    @GetMapping("/student/{studentId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<ClearanceRequestDTO>> findByStudent(@PathVariable UUID studentId) {
        List<ClearanceRequestDTO> requests = clearanceRequestService.findByStudent(studentId).stream()
                .map(ClearanceRequestMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Clearance requests retrieved successfully",
                requests
        );
    }

    @GetMapping("/department/{departmentId}/pending")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<ClearanceRequestDTO>> findPendingForDepartment(@PathVariable UUID departmentId) {
        List<ClearanceRequestDTO> requests = clearanceRequestService.findPendingForDepartment(departmentId).stream()
                .map(ClearanceRequestMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Pending clearance requests retrieved successfully",
                requests
        );
    }

    @GetMapping("/student/{studentId}/fully-cleared")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Boolean> isStudentFullyCleared(@PathVariable UUID studentId) {
        boolean cleared = clearanceRequestService.isStudentFullyCleared(studentId);
        return ApiResponse.of("Clearance status retrieved successfully", cleared);
    }

    // runs the eligibility checks, then fans out one PENDING row per department
    @PostMapping("/initiate/{studentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<ClearanceRequestDTO>> initiateClearance(@PathVariable UUID studentId) {
        Student studentRef = new Student();
        studentRef.setId(studentId);
        Student student = studentService.findById(studentRef);

        List<ClearanceRequestDTO> requests = clearanceRequestService.initiateClearance(student).stream()
                .map(ClearanceRequestMapper::toDTO)
                .toList();
        return ApiResponse.of("Clearance initiated successfully", requests);
    }

    // a department officer approves/rejects their specific row
    @PutMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ClearanceRequestDTO> updateStatus(@PathVariable UUID id, @RequestBody ClearanceRequestDTO requestDTO) {
        ClearanceRequest request = new ClearanceRequest();
        request.setId(id);
        request.setStatus(requestDTO.getStatus());
        request.setRemarks(requestDTO.getRemarks());
        ClearanceRequest updated = clearanceRequestService.updateStatus(request);
        return ApiResponse.of(
                "Clearance request status updated successfully",
                ClearanceRequestMapper.toDTO(updated)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        ClearanceRequest request = new ClearanceRequest();
        request.setId(id);
        clearanceRequestService.delete(request);
        return ApiResponse.of("Clearance request deleted successfully", null);
    }
}
