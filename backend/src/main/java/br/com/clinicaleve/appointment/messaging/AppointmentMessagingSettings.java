package br.com.clinicaleve.appointment.messaging;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointment_messaging_settings")
public class AppointmentMessagingSettings extends TenantEntity {

    @Column(nullable = false)
    private boolean whatsappEnabled;

    @Column(nullable = false, length = 160)
    private String confirmationTemplateName = "consulta_confirmacao";

    @Column(nullable = false, length = 160)
    private String reminderTemplateName = "consulta_lembrete";

    @Column(nullable = false, length = 20)
    private String languageCode = "pt_BR";

    @Column(nullable = false, length = 1000)
    private String confirmationPreview;

    @Column(nullable = false, length = 1000)
    private String reminderPreview;

    @Column(nullable = false)
    private int firstReminderHours = 24;

    private Integer secondReminderHours = 2;

    @Column(nullable = false)
    private int maxAttempts = 3;

    @Column(nullable = false)
    private int retryMinutes = 15;
}
