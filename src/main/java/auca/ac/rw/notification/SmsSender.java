package auca.ac.rw.notification;

/**
 * The Interface SmsSender.
 * Sends one text message. The default implementation (LoggingSmsSender)
 * only logs, since sending real SMS needs a paid gateway account. To go
 * live, add a bean implementing this with your provider's API (e.g. Africa's
 * Talking, widely used in Rwanda, or Twilio) and remove LoggingSmsSender.
 *
 * @version 1.0
 */
public interface SmsSender {

    /**
     * @param phoneNumber recipient in international format, e.g. +250788123456
     * @param text        message body
     */
    void send(String phoneNumber, String text);
}
