package br.com.clinicaleve.billing;

import br.com.clinicaleve.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asaas_webhook_events")
public class AsaasWebhookEvent extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String asaasEventId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(length = 80)
    private String entityId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebhookProcessingStatus processingStatus;

    @Column(length = 1000)
    private String errorMessage;

    private Instant processedAt;
}
