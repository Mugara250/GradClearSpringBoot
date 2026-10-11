package auca.ac.rw.notification;

import auca.ac.rw.clearance_request.domain.ClearanceStatus;

/**
 * The Class NotificationMessages.
 * The wording of clearance notifications, shared by the email and SMS
 * consumers so both channels always say the same thing.
 *
 * @version 1.0
 */
final class NotificationMessages {

    private NotificationMessages() {}

    static String emailSubject(ClearanceDecisionEvent e) {
        if (e.fullyCleared()) {
            return "GradClear: you are fully cleared for graduation";
        }
        return e.status() == ClearanceStatus.APPROVED
                ? "GradClear: " + e.departmentName() + " approved your clearance"
                : "GradClear: " + e.departmentName() + " rejected your clearance";
    }

    static String emailBody(ClearanceDecisionEvent e) {
        StringBuilder body = new StringBuilder()
                .append("Dear ").append(e.studentName()).append(",\n\n");

        if (e.status() == ClearanceStatus.APPROVED) {
            body.append(e.departmentName()).append(" has approved your graduation clearance.\n");
        } else {
            body.append(e.departmentName()).append(" has rejected your graduation clearance.\n");
        }
        if (e.remarks() != null && !e.remarks().isBlank()) {
            body.append(e.status() == ClearanceStatus.REJECTED ? "\nReason: " : "\nRemarks: ")
                    .append(e.remarks()).append('\n');
        }
        if (e.status() == ClearanceStatus.REJECTED) {
            body.append("\nPlease resolve this with ").append(e.departmentName())
                    .append(". Once it is resolved, they will reopen your request.\n");
        }

        body.append("\nProgress: ").append(e.approvedCount()).append(" of ").append(e.totalCount())
                .append(" departments have approved your clearance.\n");
        if (e.fullyCleared()) {
            body.append("\nCongratulations! Every department has approved. You are fully cleared for graduation.\n");
        }

        return body.append("\nRegistration number: ").append(e.registrationNumber())
                .append("\n\nGradClear graduation clearance\n")
                .toString();
    }

    // kept short: one SMS segment where possible
    static String sms(ClearanceDecisionEvent e) {
        String text = e.status() == ClearanceStatus.APPROVED
                ? "GradClear: " + e.departmentName() + " APPROVED your clearance. " + e.approvedCount() + "/" + e.totalCount() + " approved."
                : "GradClear: " + e.departmentName() + " REJECTED your clearance"
                        + (e.remarks() != null && !e.remarks().isBlank() ? ": " + e.remarks() : ".");
        return e.fullyCleared() ? text + " You are fully cleared!" : text;
    }
}
