package br.com.clinicaleve.billing;

import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AsaasWebhookService {
    private final AsaasWebhookEventRepository eventRepository;
    private final ClinicSubscriptionRepository subscriptionRepository;
    private final ClinicBillingProfileRepository profileRepository;
    private final SubscriptionPaymentService paymentService;

    public WebhookOutcome handle(JsonNode payload) {
        var eventId = text(payload, "id");
        var eventType = text(payload, "event");
        if (eventId == null || eventType == null) {
            return new WebhookOutcome(false, false, "Evento sem identificador ou tipo");
        }

        var existing = eventRepository.findByAsaasEventId(eventId);
        if (existing.isPresent()
                && (existing.get().getProcessingStatus() == WebhookProcessingStatus.PROCESSED
                || existing.get().getProcessingStatus() == WebhookProcessingStatus.IGNORED)) {
            return new WebhookOutcome(true, true, null);
        }

        var event = existing.orElseGet(AsaasWebhookEvent::new);
        event.setAsaasEventId(eventId);
        event.setEventType(eventType);
        event.setPayload(payload.toString());
        event.setProcessingStatus(WebhookProcessingStatus.PENDING);
        event.setErrorMessage(null);
        eventRepository.save(event);

        try {
            var payment = payload.path("payment");
            if (payment.isMissingNode() || payment.isNull() || text(payment, "id") == null) {
                event.setProcessingStatus(WebhookProcessingStatus.IGNORED);
                event.setProcessedAt(Instant.now());
                eventRepository.save(event);
                return new WebhookOutcome(true, false, null);
            }

            event.setEntityId(text(payment, "id"));
            var subscription = resolveSubscription(payment);
            if (subscription == null) {
                event.setProcessingStatus(WebhookProcessingStatus.IGNORED);
                event.setProcessedAt(Instant.now());
                eventRepository.save(event);
                return new WebhookOutcome(true, false, null);
            }

            var remoteSubscriptionId = text(payment, "subscription");
            if (remoteSubscriptionId != null && subscription.getAsaasSubscriptionId() == null) {
                subscription.setAsaasSubscriptionId(remoteSubscriptionId);
                subscriptionRepository.save(subscription);
            }

            paymentService.apply(subscription, eventType, paymentData(payment));
            event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            eventRepository.save(event);
            return new WebhookOutcome(true, false, null);
        } catch (RuntimeException exception) {
            event.setProcessingStatus(WebhookProcessingStatus.FAILED);
            event.setErrorMessage(limit(exception.getMessage(), 1000));
            eventRepository.save(event);
            return new WebhookOutcome(false, false, "Falha ao processar o evento");
        }
    }

    private ClinicSubscription resolveSubscription(JsonNode payment) {
        var remoteSubscriptionId = text(payment, "subscription");
        if (remoteSubscriptionId != null) {
            var byRemote = subscriptionRepository.findByAsaasSubscriptionId(remoteSubscriptionId);
            if (byRemote.isPresent()) return byRemote.get();
        }

        var externalReference = text(payment, "externalReference");
        if (externalReference != null) {
            var byReference = subscriptionRepository.findById(externalReference);
            if (byReference.isPresent()) return byReference.get();
        }

        var customerId = text(payment, "customer");
        if (customerId == null) return null;
        return profileRepository.findByAsaasCustomerId(customerId)
                .flatMap(profile -> subscriptionRepository.findByClinicId(profile.getClinicId()))
                .orElse(null);
    }

    private SubscriptionPaymentService.PaymentData paymentData(JsonNode payment) {
        return new SubscriptionPaymentService.PaymentData(
                text(payment, "id"),
                text(payment, "status"),
                text(payment, "billingType"),
                decimal(payment, "value"),
                date(payment, "dueDate"),
                text(payment, "paymentDate"),
                text(payment, "clientPaymentDate"),
                text(payment, "invoiceUrl"),
                text(payment, "bankSlipUrl"),
                text(payment, "description")
        );
    }

    private String text(JsonNode node, String field) {
        var value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        var value = node.path(field);
        return value.isNumber() ? value.decimalValue() : BigDecimal.ZERO;
    }

    private LocalDate date(JsonNode node, String field) {
        var value = text(node, field);
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String limit(String value, int max) {
        if (value == null) return "Erro não informado";
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record WebhookOutcome(boolean success, boolean duplicate, String error) {
    }
}
