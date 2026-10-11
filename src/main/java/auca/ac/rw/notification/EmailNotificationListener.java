package auca.ac.rw.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * The Class EmailNotificationListener.
 * Consumes clearance decisions from gradclear.notifications.email and emails
 * the student through the configured SMTP server (spring.mail.*). In
 * development that's MailHog from compose.yaml: open http://localhost:8025
 * to see every email sent.
 *
 * Throwing from here (e.g. SMTP down) makes the listener retry with
 * back-off; after the last attempt the message goes to
 * gradclear.notifications.email.dlq instead of being lost or looping.
 *
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class EmailNotificationListener {

    private final JavaMailSender mailSender;

    @Value("${gradclear.notifications.from}")
    private String from;

    @RabbitListener(queues = NotificationMessaging.EMAIL_QUEUE)
    public void onDecision(ClearanceDecisionEvent event) {
        if (event.email() == null || event.email().isBlank()) {
            log.info("No email on file for student {}; skipping email for request {}",
                    event.registrationNumber(), event.requestId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.email());
        message.setSubject(NotificationMessages.emailSubject(event));
        message.setText(NotificationMessages.emailBody(event));
        mailSender.send(message);

        log.info("Emailed {} about {} decision from {}", event.email(), event.status(), event.departmentName());
    }
}
