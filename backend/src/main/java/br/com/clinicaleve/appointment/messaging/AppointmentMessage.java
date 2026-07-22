package br.com.clinicaleve.appointment.messaging;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointment_messages")
public class AppointmentMessage extends TenantEntity {

    @Column(nullable = false, length = 36, updatable = false)
    private String appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageChannel channel = MessageChannel.WHATSAPP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MessagePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageDirection direction = MessageDirection.OUTBOUND;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentMessageStatus status = AppointmentMessageStatus.PENDING;

    @Column(nullable = false, length = 30)
    private String recipient;

    @Column(length = 160)
    private String templateName;

    @Column(nullable = false)
    private Instant scheduledAt;

    private Instant nextAttemptAt;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts = 3;

    @Column(length = 200)
    private String providerMessageId;

    @Column(length = 200)
    private String responseProviderMessageId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResponseAction responseAction;

    @Column(length = 1000)
    private String errorMessage;

    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant respondedAt;
}
