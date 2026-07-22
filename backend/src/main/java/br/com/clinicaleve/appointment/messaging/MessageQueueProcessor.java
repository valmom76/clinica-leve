package br.com.clinicaleve.appointment.messaging;

import br.com.clinicaleve.appointment.AppointmentRepository;
import br.com.clinicaleve.appointment.AppointmentStatus;
import br.com.clinicaleve.patient.PatientRepository;
import br.com.clinicaleve.professional.ProfessionalRepository;
import br.com.clinicaleve.tenant.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageQueueProcessor {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"));

    private final AppointmentMessageRepository messageRepository;
    private final AppointmentMessagingSettingsRepository settingsRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;
    private final ClinicRepository clinicRepository;
    private final List<AppointmentMessageSender> senders;
    private final MessagingProperties properties;

    public List<String> dueIds() {
        if (!properties.metaConfigured()) return List.of();
        return messageRepository.findDueIds(Instant.now()).stream().limit(50).toList();
    }

    @Transactional
    public void process(String id) {
        var message = messageRepository.findForProcessing(id).orElse(null);
        if (message == null || message.getNextAttemptAt() == null
                || message.getNextAttemptAt().isAfter(Instant.now())
                || (message.getStatus() != AppointmentMessageStatus.PENDING
                && message.getStatus() != AppointmentMessageStatus.FAILED)) return;
        message.setStatus(AppointmentMessageStatus.PROCESSING);
        message.setAttemptCount(message.getAttemptCount() + 1);
        try {
            var appointment = appointmentRepository.findByIdAndClinicId(
                    message.getAppointmentId(), message.getClinicId()
            ).orElseThrow(() -> new IllegalStateException("Agendamento não encontrado"));
            if (!appointment.getStartAt().isAfter(Instant.now())
                    || appointment.getStatus() == AppointmentStatus.CANCELLED
                    || appointment.getStatus() == AppointmentStatus.COMPLETED
                    || appointment.getStatus() == AppointmentStatus.NO_SHOW
                    || appointment.getStatus() == AppointmentStatus.RESCHEDULE_REQUESTED) {
                message.setStatus(AppointmentMessageStatus.CANCELLED);
                message.setNextAttemptAt(null);
                messageRepository.save(message);
                return;
            }
            var patient = patientRepository.findByIdAndClinicId(
                    appointment.getPatientId(), message.getClinicId()
            ).orElseThrow(() -> new IllegalStateException("Paciente não encontrado"));
            var professional = professionalRepository.findByIdAndClinicId(
                    appointment.getProfessionalId(), message.getClinicId()
            ).orElseThrow(() -> new IllegalStateException("Profissional não encontrado"));
            var clinic = clinicRepository.findById(message.getClinicId())
                    .orElseThrow(() -> new IllegalStateException("Clínica não encontrada"));
            var settings = settingsRepository.findByClinicId(message.getClinicId())
                    .orElseThrow(() -> new IllegalStateException("Mensagens da clínica não configuradas"));
            if (!settings.isWhatsappEnabled()) {
                message.setStatus(AppointmentMessageStatus.CANCELLED);
                message.setNextAttemptAt(null);
                messageRepository.save(message);
                return;
            }
            var providerId = sender(message.getChannel()).send(new AppointmentMessageSender.TemplateMessage(
                    message.getId(),
                    message.getRecipient(),
                    message.getTemplateName(),
                    settings.getLanguageCode(),
                    List.of(
                            patient.getName(),
                            clinic.getName(),
                            professional.getName(),
                            DATE_TIME.format(appointment.getStartAt().atZone(ZoneId.of(clinic.getTimezone())))
                    ),
                    message.getPurpose() == MessagePurpose.CONFIRMATION
            ));
            message.setProviderMessageId(providerId);
            message.setStatus(AppointmentMessageStatus.SENT);
            message.setSentAt(Instant.now());
            message.setNextAttemptAt(null);
            message.setErrorMessage(null);
        } catch (RuntimeException exception) {
            message.setStatus(AppointmentMessageStatus.FAILED);
            message.setErrorMessage(safeMessage(exception));
            if (message.getAttemptCount() < message.getMaxAttempts()) {
                var retryMinutes = settingsRepository.findByClinicId(message.getClinicId())
                        .map(AppointmentMessagingSettings::getRetryMinutes)
                        .orElse(15);
                message.setNextAttemptAt(Instant.now().plusSeconds(retryMinutes * 60L));
            } else {
                message.setNextAttemptAt(null);
            }
        }
        messageRepository.save(message);
    }

    private String safeMessage(RuntimeException exception) {
        var value = exception.getMessage();
        if (value == null || value.isBlank()) return "Falha ao enviar mensagem";
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private AppointmentMessageSender sender(MessageChannel channel) {
        return senders.stream()
                .filter(candidate -> candidate.channel() == channel)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhum provedor configurado para o canal " + channel
                ));
    }
}
