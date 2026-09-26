package auca.ac.rw.clearance_request.service;

import auca.ac.rw.clearance_request.domain.ClearanceRequest;
import auca.ac.rw.student.domain.Student;

import java.util.List;
import java.util.UUID;

public interface ClearanceRequestService {
    ClearanceRequest updateStatus(ClearanceRequest request);
    void delete(ClearanceRequest request);
    ClearanceRequest findById(ClearanceRequest request);
    List<ClearanceRequest> findAll();

    List<ClearanceRequest> initiateClearance(Student student);
    List<ClearanceRequest> findByStudent(UUID studentId);
    List<ClearanceRequest> findPendingForDepartment(UUID departmentId);
    boolean isStudentFullyCleared(UUID studentId);

}
