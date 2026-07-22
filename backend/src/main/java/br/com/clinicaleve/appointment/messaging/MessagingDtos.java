package br.com.clinicaleve.appointment.messaging;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class MessagingDtos {

    private MessagingDtos() {
    }

    public record SettingsRequest(
            boolean whatsappEnabled,
            @NotBlank @Pattern(regexp = "^[a-z0-9_]{1,160}$") String confirmationTemplateName,
            @NotBlank @Pattern(regexp = "^[a-z0-9_]{1,160}$") String reminderTemplateName,
            @NotBlank @Size(max = 20) String languageCode,
            @NotBlank @Size(max = 1000) String confirmationPreview,
            @NotBlank @Size(max = 1000) String reminderPreview,
            @Min(1) @Max(168) int firstReminderHours,
            @Min(1) @Max(168) Integer secondReminderHours,
            @Min(1) @Max(10) int maxAttempts,
            @Min(1) @Max(1440) int retryMinutes
    ) {
    }

    public record SettingsResponse(
            boolean platformConfigured,
            boolean whatsappEnabled,
            String confirmationTemplateName,
            String reminderTemplateName,
            String languageCode,
            String confirmationPreview,
            String reminderPreview,
            int firstReminderHours,
            Integer secondReminderHours,
            int maxAttempts,
            int retryMinutes,
            boolean smsPrepared
    ) {
    }

    public record MessageResponse(
            String id,
            MessageChannel channel,
            MessagePurpose purpose,
            MessageDirection direction,
            AppointmentMessageStatus status,
            String recipient,
            String templateName,
            Instant scheduledAt,
            int attemptCount,
            int maxAttempts,
            String providerMessageId,
            ResponseAction responseAction,
            String errorMessage,
            Instant sentAt,
            Instant deliveredAt,
            Instant readAt,
            Instant respondedAt,
            Instant createdAt
    ) {
    }
}
