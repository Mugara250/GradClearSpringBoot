package auca.ac.rw.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The Class SmsNotificationListener.
 * Consumes clearance decisions from gradclear.notifications.sms and texts
 * the student through SmsSender. Same retry / dead-letter behaviour as the
 * email listener, on its own queue.
 *
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SmsNotificationListener {

    private final SmsSender smsSender;

    // used to turn a local number like 0788123456 into +250788123456
    @Value("${gradclear.notifications.default-country-code}")
    private String defaultCountryCode;

    @RabbitListener(queues = NotificationMessaging.SMS_QUEUE)
    public void onDecision(ClearanceDecisionEvent event) {
        String phone = toInternational(event.phoneNumber());
        if (phone == null) {
            log.info("No phone number on file for student {}; skipping SMS for request {}",
                    event.registrationNumber(), event.requestId());
            return;
        }
        smsSender.send(phone, NotificationMessages.sms(event));
    }

    String toInternational(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("[\\s()-]", "");
        if (digits.startsWith("+")) {
            return digits;
        }
        if (digits.startsWith("00")) {
            return "+" + digits.substring(2);
        }
        String countryDigits = defaultCountryCode.replace("+", "");
        if (digits.startsWith("0")) {
            return "+" + countryDigits + digits.substring(1);       // 0788123456 -> +250788123456
        }
        return "+" + (digits.startsWith(countryDigits) ? digits : countryDigits + digits); // 250788... / 788...
    }
}
