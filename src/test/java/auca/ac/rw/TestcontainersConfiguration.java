package auca.ac.rw;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Throwaway infrastructure for @SpringBootTest classes (@Import this): a real
 * Postgres, RabbitMQ and MailHog (SMTP catcher), so tests never touch the
 * development database or send real email. Requires a running Docker daemon.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final String MAILHOG_API_PROPERTY = "test.mailhog.api-url";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16");
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer("rabbitmq:4.1-management");
    }

    @Bean
    GenericContainer<?> mailhogContainer() {
        return new GenericContainer<>("mailhog/mailhog:latest").withExposedPorts(1025, 8025);
    }

    // Spring Boot has no service connection for MailHog, so point spring.mail
    // at it by hand, and expose its HTTP API for assertions
    @Bean
    DynamicPropertyRegistrar mailhogProperties(GenericContainer<?> mailhogContainer) {
        return registry -> {
            registry.add("spring.mail.host", mailhogContainer::getHost);
            registry.add("spring.mail.port", () -> mailhogContainer.getMappedPort(1025));
            registry.add(MAILHOG_API_PROPERTY,
                    () -> "http://" + mailhogContainer.getHost() + ":" + mailhogContainer.getMappedPort(8025));
        };
    }
}
