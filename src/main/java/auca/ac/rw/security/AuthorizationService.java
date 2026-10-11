package auca.ac.rw.security;

import auca.ac.rw.clearance_request.repository.ClearanceRequestRepository;
import auca.ac.rw.staff.repository.StaffRepository;
import auca.ac.rw.transcript.repository.TranscriptRepository;
import auca.ac.rw.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * The Class AuthorizationService.
 * Resource-owner checks that URL patterns can't express, called from
 * @PreAuthorize as @authz.xxx(#pathVariable). Every method answers "is the
 * *current* caller allowed?" and returns false (-> 403) when the target
 * doesn't exist, so a student can't probe which ids exist.
 *
 * Role-only rules (hasRole / hasAnyRole) stay in the annotations themselves;
 * these methods only add the ownership part.
 *
 * @version 1.0
 */
@RequiredArgsConstructor
@Component("authz")
@Transactional(readOnly = true)
public class AuthorizationService {

    private final TranscriptRepository transcriptRepository;
    private final ClearanceRequestRepository clearanceRequestRepository;
    private final StaffRepository staffRepository;

    // the clearance Department whose officers act as the academic registrar
    @Value("${gradclear.registrar-department}")
    private String registrarDepartment;

    // STUDENT whose own Student record has this id
    public boolean isSelfStudent(UUID studentId) {
        return currentUser()
                .filter(u -> u.role() == UserRole.STUDENT)
                .map(u -> studentId != null && studentId.equals(u.studentId()))
                .orElse(false);
    }

    // the User row with this id is the caller's own account
    public boolean isSelfUser(UUID userId) {
        return currentUser().map(u -> u.userId().equals(userId)).orElse(false);
    }

    // STUDENT reading one of their own transcript lines
    public boolean ownsTranscript(UUID transcriptId) {
        return transcriptRepository.findById(transcriptId)
                .map(t -> isSelfStudent(t.getStudent().getId()))
                .orElse(false);
    }

    // STUDENT reading one of their own clearance rows
    public boolean ownsClearanceRequest(UUID clearanceRequestId) {
        return clearanceRequestRepository.findById(clearanceRequestId)
                .map(r -> isSelfStudent(r.getStudent().getId()))
                .orElse(false);
    }

    // DEPARTMENT_OFFICER whose Staff record is attached to this (clearance) Department
    public boolean isOfficerOf(UUID departmentId) {
        return currentUser()
                .filter(u -> u.role() == UserRole.DEPARTMENT_OFFICER && u.staffId() != null)
                .map(u -> departmentId != null && staffRepository.existsByIdAndDepartments_Id(u.staffId(), departmentId))
                .orElse(false);
    }

    // DEPARTMENT_OFFICER of the department this clearance row is addressed to
    public boolean canReviewClearanceRequest(UUID clearanceRequestId) {
        return clearanceRequestRepository.findById(clearanceRequestId)
                .map(r -> isOfficerOf(r.getDepartment().getId()))
                .orElse(false);
    }

    // DEPARTMENT_OFFICER attached to the registrar's department: the office
    // that records results on transcripts (standing in for the student
    // portal / registration system GradClear would normally get them from)
    public boolean isRegistrar() {
        return currentUser().map(u -> isRegistrar(u.role(), u.staffId())).orElse(false);
    }

    public boolean isRegistrar(UserRole role, UUID staffId) {
        return role == UserRole.DEPARTMENT_OFFICER && staffId != null
                && staffRepository.existsByIdAndDepartments_NameIgnoreCase(staffId, registrarDepartment);
    }

    private Optional<AuthenticatedUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
