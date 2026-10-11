package auca.ac.rw.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The Class LoggingSmsSender.
 * Stand-in SMS gateway: writes the text it would have sent to the
 * application log ("[SMS simulated]"), so the whole flow can be
 * demonstrated without a paid SMS account. See SmsSender to plug in a real one.
 *
 * @version 1.0
 */
@Slf4j
@Component
public class LoggingSmsSender implements SmsSender {

    @Override
    public void send(String phoneNumber, String text) {
        log.info("[SMS simulated] to {}: {}", phoneNumber, text);
    }
}
