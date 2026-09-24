package auca.ac.rw.clearance_request.repository;

import auca.ac.rw.clearance_request.domain.ClearanceRequest;
import auca.ac.rw.clearance_request.domain.ClearanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClearanceRequestRepository extends JpaRepository<ClearanceRequest, UUID> {
    List<ClearanceRequest> findByStudentId(UUID studentId);

    List<ClearanceRequest> findByDepartment_IdAndStatus(UUID departmentId, ClearanceStatus status);

    boolean existsByStudentIdAndStatusNot(UUID studentId, ClearanceStatus status);
}
