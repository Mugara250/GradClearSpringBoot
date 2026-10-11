package auca.ac.rw.notification;

import auca.ac.rw.clearance_request.domain.ClearanceStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * The Class ClearanceDecisionEvent.
 * Message published to RabbitMQ when a department approves or rejects a
 * student's clearance request. It carries everything the notification
 * consumers need (who to contact, what was decided, overall progress), so
 * they never have to query the database and could run as a separate service.
 *
 * @param requestId       the clearance request that was decided
 * @param studentId       Student row id
 * @param registrationNumber the student's human-readable studentId
 * @param studentName     "First Last"
 * @param email           where to email the student, or null
 * @param phoneNumber     where to text the student, or null
 * @param departmentName  the office that made the decision
 * @param status          APPROVED or REJECTED
 * @param remarks         the officer's remarks (the reason, for a rejection)
 * @param decidedAt       when the decision was saved
 * @param approvedCount   how many of the student's departments have approved so far
 * @param totalCount      how many departments the student needs in total
 * @version 1.0
 */
public record ClearanceDecisionEvent(
        UUID requestId,
        UUID studentId,
        String registrationNumber,
        String studentName,
        String email,
        String phoneNumber,
        String departmentName,
        ClearanceStatus status,
        String remarks,
        Instant decidedAt,
        int approvedCount,
        int totalCount
) {
    public boolean fullyCleared() {
        return totalCount > 0 && approvedCount == totalCount;
    }
}
