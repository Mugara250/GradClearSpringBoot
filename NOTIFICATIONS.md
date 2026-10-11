# GradClear: Clearance notifications (RabbitMQ)

When a department **approves** or **rejects** a student's clearance request,
the student is notified by **email** and **SMS**. The notification goes
through RabbitMQ, so sending email or SMS never slows down or breaks the
officer's approve/reject action.

```
Officer approves / rejects (PUT /api/v1/clearance-request/{id}/status)
        │
        ▼
ClearanceRequestServiceImplementation.updateStatus()  ── saves the decision
        │  status actually changed to APPROVED / REJECTED
        ▼
ClearanceEventPublisher ── ClearanceDecisionEvent (JSON)
        │  routing key clearance.decision.approved | clearance.decision.rejected
        ▼
[gradclear.clearance]  topic exchange
        ├──► gradclear.notifications.email ──► EmailNotificationListener ──► SMTP
        └──► gradclear.notifications.sms   ──► SmsNotificationListener   ──► SmsSender
                     │ failed after retries
                     ▼
        [gradclear.clearance.dlx] ──► gradclear.notifications.email.dlq / .sms.dlq
```

## What the student receives

| Event | Email subject | SMS |
|---|---|---|
| Approved | `GradClear: <Department> approved your clearance` | `GradClear: <Department> APPROVED your clearance. 2/5 approved.` |
| Rejected | `GradClear: <Department> rejected your clearance` | `GradClear: <Department> REJECTED your clearance: <reason>` |
| Last approval | `GradClear: you are fully cleared for graduation` | `… You are fully cleared!` |

- The email body includes the officer's remarks (the **reason**, for a
  rejection), what to do next, and progress ("2 of 5 departments have
  approved").
- Contact details come from the **Student** record (`email`, `phoneNumber`).
  A student without an email or phone is skipped on that channel.
- Local phone numbers are converted to international format with
  `GRADCLEAR_DEFAULT_COUNTRY_CODE` (default `+250`), e.g. `0788123456` →
  `+250788123456`.
- No notification is sent when a request is reopened (back to PENDING), or
  when only the remarks change and the status stays the same.

## Running it

`compose.yaml` defines **RabbitMQ** and **MailHog** next to Postgres, and
Spring Boot starts them automatically when the app starts (you need Docker
running).

| What | Where |
|---|---|
| RabbitMQ management console | http://localhost:15672 (guest / guest) |
| MailHog: every email the app sends | http://localhost:8025 |
| Simulated SMS | the application log, lines starting with `[SMS simulated]` |

To try it: approve or reject a request in the Review queue, then open MailHog.
The email arrives within a second or two.

## Reliability

- **Retries:** a consumer that fails (e.g. the mail server is down) retries
  with back-off (`spring.rabbitmq.listener.simple.retry.*`).
- **Dead letters:** if it still fails, the message is moved to the queue's
  `.dlq` (e.g. `gradclear.notifications.email.dlq`), where you can inspect it
  in the RabbitMQ console. It is not lost and is not retried forever.
- **Separate queues per channel:** a mail outage doesn't hold up SMS, and the
  reverse is also true.
- **Broker down when an officer decides:** the decision is still saved, and
  the publish failure is logged. The student isn't notified about that one
  decision. Guaranteeing delivery even then would need a transactional
  outbox: store the event in Postgres in the same transaction, and relay it
  to RabbitMQ afterwards.

## Sending real emails

Point `spring.mail.*` at a real SMTP server using environment variables. For
example, Gmail with an App Password (Google account → Security → 2-Step
Verification → App passwords):

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=<16-character app password>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
GRADCLEAR_MAIL_FROM="GradClear <you@gmail.com>"
```

## Sending real SMS

`LoggingSmsSender` only logs. Real SMS needs a gateway account, for example
**Africa's Talking** (common in Rwanda) or Twilio. To use one:

1. Add a class implementing `auca.ac.rw.notification.SmsSender` that calls
   the provider's API.
2. Delete `LoggingSmsSender`, or keep it and mark your class `@Primary`.

Nothing else changes: the queue, retries and dead-lettering stay the same.

## Note on data

Each event carries the student's name, email and phone number, so consumers
don't need database access and could later run as a separate service. Treat
the RabbitMQ console as containing personal data. In production, change the
default `guest` credentials.
