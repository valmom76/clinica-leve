package br.com.clinicaleve.appointment.messaging;

import java.util.List;

public interface AppointmentMessageSender {

    MessageChannel channel();

    String send(TemplateMessage message);

    record TemplateMessage(
            String messageId,
            String recipient,
            String templateName,
            String languageCode,
            List<String> bodyParameters,
            boolean confirmationButtons
    ) {
    }
}
