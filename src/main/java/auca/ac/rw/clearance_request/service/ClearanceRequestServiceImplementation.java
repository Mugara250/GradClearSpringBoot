package auca.ac.rw.clearance_request.service;

import auca.ac.rw.clearance_request.domain.ClearanceRequest;
import auca.ac.rw.clearance_request.domain.ClearanceStatus;
import auca.ac.rw.clearance_request.repository.ClearanceRequestRepository;
import auca.ac.rw.department.domain.Department;
import auca.ac.rw.department.repository.DepartmentRepository;
import auca.ac.rw.student.domain.Student;
import auca.ac.rw.transcript.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ClearanceRequestServiceImplementation implements ClearanceRequestService{

    private final ClearanceRequestRepository clearanceRequestRepository;
    private final DepartmentRepository departmentRepository;
    private final TranscriptService transcriptService;

    @Override
    public ClearanceRequest updateStatus(ClearanceRequest request) {
        ClearanceRequest found = findById(request);
        found.setStatus(request.getStatus());
        found.setRemarks(request.getRemarks());
        return clearanceRequestRepository.save(found);
    }

    @Override
    public void delete(ClearanceRequest request) {
        clearanceRequestRepository.delete(request);
    }

    @Override
    public ClearanceRequest findById(ClearanceRequest request) {
        return clearanceRequestRepository.findById(request.getId())
                .orElseThrow(()-> new RuntimeException("Clearance request with id " + request.getId() + " not found"));
    }

    @Override
    public List<ClearanceRequest> findAll() {
        return clearanceRequestRepository.findAll();
    }

    @Override
    public List<ClearanceRequest> initiateClearance(Student student) {
        boolean academicallyClear = transcriptService.isStudentAcademicallyClear(student.getId());
        if (!academicallyClear) {
            throw new IllegalStateException("Student has unresolved (failed/incomplete) courses");
        }
//        if (!Boolean.TRUE.equals(student.getCapstoneDefended())) {
//            throw new IllegalStateException("Student has not defended their capstone project");
//        }

        List<Department> allDepartments = departmentRepository.findAll();
        List<ClearanceRequest> requests = allDepartments.stream()
                .map(dept -> {
                    ClearanceRequest req = new ClearanceRequest();
                    req.setStudent(student);
                    req.setDepartment(dept);
                    return req;
                })
                .toList();

        return clearanceRequestRepository.saveAll(requests);
    }

    @Override
    public List<ClearanceRequest> findByStudent(UUID studentId) {
        return clearanceRequestRepository.findByStudentId(studentId);
    }

    @Override
    public List<ClearanceRequest> findPendingForDepartment(UUID departmentId) {
        return clearanceRequestRepository.findByDepartment_IdAndStatus(departmentId, ClearanceStatus.PENDING);
    }

    @Override
    public boolean isStudentFullyCleared(UUID studentId) {
        boolean hasUnapprovedRow = clearanceRequestRepository.existsByStudentIdAndStatusNot(studentId, ClearanceStatus.APPROVED);
        return !hasUnapprovedRow;
    }
}
