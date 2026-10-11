package auca.ac.rw.notification;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class NotificationMessaging.
 * RabbitMQ topology for clearance notifications:
 *
 *   ClearanceEventPublisher
 *        |  routing key clearance.decision.approved / clearance.decision.rejected
 *        v
 *   [gradclear.clearance]  (topic exchange)
 *        |-- clearance.decision.* --> gradclear.notifications.email --> EmailNotificationListener
 *        '-- clearance.decision.* --> gradclear.notifications.sms   --> SmsNotificationListener
 *
 * Each channel has its own queue, so a slow or broken mail server never
 * delays text messages and vice versa. A message that still fails after the
 * listener's retries (spring.rabbitmq.listener.simple.retry.*) is
 * dead-lettered to [gradclear.clearance.dlx] and parked in the matching
 * *.dlq queue for inspection in the RabbitMQ console, instead of being
 * redelivered forever.
 *
 * Queues and exchanges are declared by Spring on startup (RabbitAdmin), so
 * nothing needs creating by hand.
 *
 * @version 1.0
 */
@Configuration
public class NotificationMessaging {

    public static final String EXCHANGE = "gradclear.clearance";
    public static final String DEAD_LETTER_EXCHANGE = "gradclear.clearance.dlx";
    public static final String DECISION_ROUTING_PREFIX = "clearance.decision.";
    public static final String EMAIL_QUEUE = "gradclear.notifications.email";
    public static final String SMS_QUEUE = "gradclear.notifications.sms";

    @Bean
    public Declarables notificationTopology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        DirectExchange deadLetters = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);

        Queue email = workQueue(EMAIL_QUEUE);
        Queue emailDlq = QueueBuilder.durable(EMAIL_QUEUE + ".dlq").build();
        Queue sms = workQueue(SMS_QUEUE);
        Queue smsDlq = QueueBuilder.durable(SMS_QUEUE + ".dlq").build();

        Binding emailBinding = BindingBuilder.bind(email).to(exchange).with(DECISION_ROUTING_PREFIX + "*");
        Binding smsBinding = BindingBuilder.bind(sms).to(exchange).with(DECISION_ROUTING_PREFIX + "*");
        Binding emailDlqBinding = BindingBuilder.bind(emailDlq).to(deadLetters).with(EMAIL_QUEUE);
        Binding smsDlqBinding = BindingBuilder.bind(smsDlq).to(deadLetters).with(SMS_QUEUE);

        return new Declarables(exchange, deadLetters, email, emailDlq, sms, smsDlq,
                emailBinding, smsBinding, emailDlqBinding, smsDlqBinding);
    }

    // durable queue whose rejected messages go to the dead-letter exchange,
    // routed by the queue's own name to its .dlq
    private static Queue workQueue(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(name)
                .build();
    }

    // messages travel as JSON, readable in the RabbitMQ console; picked up by
    // both RabbitTemplate and the @RabbitListener containers
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
