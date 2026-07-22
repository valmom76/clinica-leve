package br.com.clinicaleve.appointment.messaging;

import br.com.clinicaleve.appointment.AppointmentRepository;
import br.com.clinicaleve.appointment.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WhatsAppWebhookService {

    private static final String CONFIRM = "CL_CONFIRM:";
    private static final String RESCHEDULE = "CL_RESCHEDULE:";

    private final ObjectMapper objectMapper;
    private final AppointmentMessageRepository messageRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMessagingSettingsRepository settingsRepository;

    @Transactional
    public void handle(byte[] payload) {
        try {
            var root = objectMapper.readTree(payload);
            root.path("entry").forEach(entry -> entry.path("changes").forEach(change -> {
                var value = change.path("value");
                value.path("statuses").forEach(this::applyStatus);
                value.path("messages").forEach(this::applyResponse);
            }));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Webhook do WhatsApp inválido", exception);
        }
    }

    private void applyStatus(JsonNode statusNode) {
        var providerId = statusNode.path("id").asText("");
        var status = statusNode.path("status").asText("");
        if (providerId.isBlank() || status.isBlank()) return;
        var message = messageRepository.findByProviderMessageId(providerId).orElse(null);
        if (message == null || message.getStatus() == AppointmentMessageStatus.RESPONDED) return;
        var at = instant(statusNode.path("timestamp"));
        switch (status) {
            case "sent" -> {
                if (deliveryRank(message.getStatus()) > deliveryRank(AppointmentMessageStatus.SENT)) return;
                message.setStatus(AppointmentMessageStatus.SENT);
                message.setSentAt(at);
            }
            case "delivered" -> {
                if (deliveryRank(message.getStatus()) > deliveryRank(AppointmentMessageStatus.DELIVERED)) return;
                message.setStatus(AppointmentMessageStatus.DELIVERED);
                message.setDeliveredAt(at);
            }
            case "read" -> {
                message.setStatus(AppointmentMessageStatus.READ);
                message.setReadAt(at);
            }
            case "failed" -> {
                if (deliveryRank(message.getStatus()) >= deliveryRank(AppointmentMessageStatus.DELIVERED)) return;
                message.setStatus(AppointmentMessageStatus.FAILED);
                message.setErrorMessage(errorMessage(statusNode));
                if (message.getAttemptCount() < message.getMaxAttempts()) {
                    var retryMinutes = settingsRepository.findByClinicId(message.getClinicId())
                            .map(AppointmentMessagingSettings::getRetryMinutes)
                            .orElse(15);
                    message.setNextAttemptAt(Instant.now().plusSeconds(retryMinutes * 60L));
                } else {
                    message.setNextAttemptAt(null);
                }
            }
            default -> {
                return;
            }
        }
        messageRepository.save(message);
    }

    private void applyResponse(JsonNode messageNode) {
        var responseId = messageNode.path("id").asText("");
        if (responseId.isBlank() || messageRepository.existsByResponseProviderMessageId(responseId)) return;
        var payload = responsePayload(messageNode);
        var action = action(payload);
        var internalId = internalMessageId(payload);
        if (action == null || internalId == null) return;
        var message = messageRepository.findById(internalId).orElse(null);
        if (message == null || !contextMatches(messageNode, message)) return;

        var at = instant(messageNode.path("timestamp"));
        message.setStatus(AppointmentMessageStatus.RESPONDED);
        message.setResponseAction(action);
        message.setResponseProviderMessageId(responseId);
        message.setRespondedAt(at);
        message.setNextAttemptAt(null);
        messageRepository.save(message);

        var appointment = appointmentRepository.findByIdAndClinicId(
                message.getAppointmentId(), message.getClinicId()
        ).orElse(null);
        if (appointment == null || appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) return;
        if (action == ResponseAction.CONFIRMED) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointment.setConfirmedAt(at);
            appointment.setRescheduleRequestedAt(null);
            cancelPendingConfirmations(appointment.getId(), appointment.getClinicId());
        } else {
            appointment.setStatus(AppointmentStatus.RESCHEDULE_REQUESTED);
            appointment.setRescheduleRequestedAt(at);
            cancelPendingMessages(appointment.getId(), appointment.getClinicId());
        }
        appointmentRepository.save(appointment);
    }

    private void cancelPendingMessages(String appointmentId, String clinicId) {
        var messages = messageRepository.findCancellable(appointmentId, clinicId);
        messages.forEach(message -> {
            message.setStatus(AppointmentMessageStatus.CANCELLED);
            message.setNextAttemptAt(null);
        });
        messageRepository.saveAll(messages);
    }

    private void cancelPendingConfirmations(String appointmentId, String clinicId) {
        var messages = messageRepository.findCancellableConfirmations(appointmentId, clinicId);
        messages.forEach(message -> {
            message.setStatus(AppointmentMessageStatus.CANCELLED);
            message.setNextAttemptAt(null);
        });
        messageRepository.saveAll(messages);
    }

    private String responsePayload(JsonNode node) {
        var button = node.path("button").path("payload").asText("");
        if (!button.isBlank()) return button;
        return node.path("interactive").path("button_reply").path("id").asText("");
    }

    private ResponseAction action(String payload) {
        if (payload.startsWith(CONFIRM)) return ResponseAction.CONFIRMED;
        if (payload.startsWith(RESCHEDULE)) return ResponseAction.RESCHEDULE_REQUESTED;
        return null;
    }

    private String internalMessageId(String payload) {
        var separator = payload.indexOf(':');
        if (separator < 0 || separator == payload.length() - 1) return null;
        return payload.substring(separator + 1);
    }

    private boolean contextMatches(JsonNode node, AppointmentMessage message) {
        var contextId = node.path("context").path("id").asText("");
        return contextId.isBlank() || contextId.equals(message.getProviderMessageId());
    }

    private Instant instant(JsonNode node) {
        var seconds = node.asLong(0);
        return seconds > 0 ? Instant.ofEpochSecond(seconds) : Instant.now();
    }

    private String errorMessage(JsonNode node) {
        var title = node.path("errors").path(0).path("title").asText("");
        return title.isBlank() ? "A Meta informou falha na entrega" : title;
    }

    private int deliveryRank(AppointmentMessageStatus status) {
        return switch (status) {
            case SENT -> 1;
            case DELIVERED -> 2;
            case READ -> 3;
            case RESPONDED -> 4;
            default -> 0;
        };
    }
}
