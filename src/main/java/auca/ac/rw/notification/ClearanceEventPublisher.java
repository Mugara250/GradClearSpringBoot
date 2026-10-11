package auca.ac.rw.notification;

import auca.ac.rw.clearance_request.domain.ClearanceRequest;
import auca.ac.rw.clearance_request.domain.ClearanceStatus;
import auca.ac.rw.clearance_request.repository.ClearanceRequestRepository;
import auca.ac.rw.student.domain.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * The Class ClearanceEventPublisher.
 * Turns a saved clearance decision into a ClearanceDecisionEvent on the
 * gradclear.clearance exchange. Called by ClearanceRequestServiceImplementation
 * after the new status has been saved.
 *
 * Trade-off: if RabbitMQ is unreachable, the decision still stands and the
 * failure is logged; the student just isn't notified about that decision.
 * Guaranteeing delivery would need a transactional outbox (write the event
 * to a table in the same transaction, relay it to RabbitMQ afterwards).
 *
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ClearanceEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ClearanceRequestRepository clearanceRequestRepository;

    public void publishDecision(ClearanceRequest request) {
        ClearanceStatus status = request.getStatus();
        if (status != ClearanceStatus.APPROVED && status != ClearanceStatus.REJECTED) {
            return;
        }

        Student student = request.getStudent();
        List<ClearanceRequest> all = clearanceRequestRepository.findByStudentId(student.getId());
        int approved = (int) all.stream().filter(r -> r.getStatus() == ClearanceStatus.APPROVED).count();

        ClearanceDecisionEvent event = new ClearanceDecisionEvent(
                request.getId(),
                student.getId(),
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getEmail(),
                student.getPhoneNumber(),
                request.getDepartment().getName(),
                status,
                request.getRemarks(),
                Instant.now(),
                approved,
                all.size()
        );

        String routingKey = NotificationMessaging.DECISION_ROUTING_PREFIX + status.name().toLowerCase();
        try {
            rabbitTemplate.convertAndSend(NotificationMessaging.EXCHANGE, routingKey, event);
            log.info("Published {} for clearance request {} ({} / {})",
                    routingKey, request.getId(), event.registrationNumber(), event.departmentName());
        } catch (AmqpException ex) {
            log.error("Could not publish {} for clearance request {}; the student will not be notified of it",
                    routingKey, request.getId(), ex);
        }
    }
}
