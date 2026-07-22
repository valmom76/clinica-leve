package br.com.clinicaleve.appointment.messaging;

import br.com.clinicaleve.appointment.Appointment;
import br.com.clinicaleve.appointment.AppointmentRepository;
import br.com.clinicaleve.appointment.AppointmentStatus;
import br.com.clinicaleve.appointment.messaging.MessagingDtos.MessageResponse;
import br.com.clinicaleve.appointment.messaging.MessagingDtos.SettingsRequest;
import br.com.clinicaleve.appointment.messaging.MessagingDtos.SettingsResponse;
import br.com.clinicaleve.patient.PatientRepository;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentMessagingService {

    private static final String CONFIRMATION_PREVIEW = "Olá {{paciente}}, sua consulta na {{clinica}} com {{profissional}} está marcada para {{data_hora}}. Confirme ou solicite reagendamento.";
    private static final String REMINDER_PREVIEW = "Lembrete: {{paciente}}, sua consulta na {{clinica}} com {{profissional}} será em {{data_hora}}.";

    private final AppointmentMessagingSettingsRepository settingsRepository;
    private final AppointmentMessageRepository messageRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final MessagingProperties properties;

    @Transactional(readOnly = true)
    public SettingsResponse settings() {
        return response(currentSettings(TenantAccess.currentClinicId()));
    }

    @Transactional
    public SettingsResponse saveSettings(SettingsRequest request) {
        if (request.secondReminderHours() != null
                && request.secondReminderHours() >= request.firstReminderHours()) {
            throw new IllegalArgumentException("O segundo lembrete deve ocorrer depois do primeiro");
        }
        if (request.whatsappEnabled() && !properties.metaConfigured()) {
            throw new IllegalStateException(
                    "Configure as credenciais da Meta no backend antes de ativar o WhatsApp"
            );
        }
        var clinicId = TenantAccess.currentClinicId();
        var settings = settingsRepository.findByClinicId(clinicId).orElseGet(() -> defaults(clinicId));
        var wasEnabled = settings.isWhatsappEnabled();
        settings.setWhatsappEnabled(request.whatsappEnabled());
        settings.setConfirmationTemplateName(request.confirmationTemplateName().trim());
        settings.setReminderTemplateName(request.reminderTemplateName().trim());
        settings.setLanguageCode(request.languageCode().trim());
        settings.setConfirmationPreview(request.confirmationPreview().trim());
        settings.setReminderPreview(request.reminderPreview().trim());
        settings.setFirstReminderHours(request.firstReminderHours());
        settings.setSecondReminderHours(request.secondReminderHours());
        settings.setMaxAttempts(request.maxAttempts());
        settings.setRetryMinutes(request.retryMinutes());
        var saved = settingsRepository.save(settings);
        if (wasEnabled != saved.isWhatsappEnabled()) {
            appointmentRepository.findFutureOpen(clinicId, Instant.now()).forEach(appointment -> {
                if (saved.isWhatsappEnabled()) {
                    rebuildQueue(appointment);
                } else {
                    cancelPending(appointment.getId(), clinicId);
                }
            });
        }
        return response(saved);
    }

    @Transactional
    public void rebuildQueue(Appointment appointment) {
        cancelPending(appointment.getId(), appointment.getClinicId());
        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW
                || appointment.getStatus() == AppointmentStatus.RESCHEDULE_REQUESTED
                || !appointment.getStartAt().isAfter(Instant.now())) {
            return;
        }
        var settings = currentSettings(appointment.getClinicId());
        if (!settings.isWhatsappEnabled()) return;
        var patient = patientRepository.findByIdAndClinicId(
                appointment.getPatientId(), appointment.getClinicId()
        ).orElse(null);
        if (patient == null || !patient.isWhatsappOptIn()) return;
        var phone = normalizedPhone(patient.getPhone());
        if (phone == null) return;
        createMessage(appointment, settings, phone, MessagePurpose.CONFIRMATION, Instant.now());
        scheduleReminder(appointment, settings, phone, settings.getFirstReminderHours());
        if (settings.getSecondReminderHours() != null) {
            scheduleReminder(appointment, settings, phone, settings.getSecondReminderHours());
        }
        appointment.setConfirmationRequestedAt(Instant.now());
        appointmentRepository.save(appointment);
    }

    @Transactional
    public MessageResponse sendConfirmationNow(String appointmentId) {
        var clinicId = TenantAccess.currentClinicId();
        var appointment = appointmentRepository.findByIdAndClinicId(appointmentId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
        if (!appointment.getStartAt().isAfter(Instant.now())) {
            throw new IllegalStateException("Não é possível confirmar uma consulta que já iniciou");
        }
        var settings = currentSettings(clinicId);
        if (!settings.isWhatsappEnabled()) {
            throw new IllegalStateException("Ative o WhatsApp nas configurações da agenda");
        }
        var patient = patientRepository.findByIdAndClinicId(appointment.getPatientId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));
        if (!patient.isWhatsappOptIn()) {
            throw new IllegalStateException("Registre a autorização do paciente para contato por WhatsApp");
        }
        var phone = normalizedPhone(patient.getPhone());
        if (phone == null) {
            throw new IllegalArgumentException("O paciente não possui um celular válido com DDD");
        }
        var message = createMessage(appointment, settings, phone, MessagePurpose.CONFIRMATION, Instant.now());
        appointment.setConfirmationRequestedAt(Instant.now());
        appointmentRepository.save(appointment);
        return response(message);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> history(String appointmentId) {
        var clinicId = TenantAccess.currentClinicId();
        appointmentRepository.findByIdAndClinicId(appointmentId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
        return messageRepository.findByAppointmentIdAndClinicIdOrderByCreatedAtDesc(appointmentId, clinicId)
                .stream().map(this::response).toList();
    }

    public AppointmentMessagingSettings currentSettings(String clinicId) {
        return settingsRepository.findByClinicId(clinicId).orElseGet(() -> defaults(clinicId));
    }

    private void scheduleReminder(
            Appointment appointment,
            AppointmentMessagingSettings settings,
            String phone,
            int hoursBefore
    ) {
        var at = appointment.getStartAt().minus(hoursBefore, ChronoUnit.HOURS);
        if (at.isAfter(Instant.now())) {
            createMessage(appointment, settings, phone, MessagePurpose.REMINDER, at);
        }
    }

    private AppointmentMessage createMessage(
            Appointment appointment,
            AppointmentMessagingSettings settings,
            String phone,
            MessagePurpose purpose,
            Instant scheduledAt
    ) {
        var message = new AppointmentMessage();
        message.setClinicId(appointment.getClinicId());
        message.setAppointmentId(appointment.getId());
        message.setChannel(MessageChannel.WHATSAPP);
        message.setPurpose(purpose);
        message.setDirection(MessageDirection.OUTBOUND);
        message.setStatus(AppointmentMessageStatus.PENDING);
        message.setRecipient(phone);
        message.setTemplateName(purpose == MessagePurpose.CONFIRMATION
                ? settings.getConfirmationTemplateName()
                : settings.getReminderTemplateName());
        message.setScheduledAt(scheduledAt);
        message.setNextAttemptAt(scheduledAt);
        message.setMaxAttempts(settings.getMaxAttempts());
        return messageRepository.save(message);
    }

    @Transactional
    public void cancelPendingForAppointment(String appointmentId, String clinicId) {
        cancelPending(appointmentId, clinicId);
    }

    @Transactional
    public void cancelPendingConfirmations(String appointmentId, String clinicId) {
        var messages = messageRepository.findCancellableConfirmations(appointmentId, clinicId);
        messages.forEach(message -> {
            message.setStatus(AppointmentMessageStatus.CANCELLED);
            message.setNextAttemptAt(null);
        });
        messageRepository.saveAll(messages);
    }

    private void cancelPending(String appointmentId, String clinicId) {
        var messages = messageRepository.findCancellable(appointmentId, clinicId);
        messages.forEach(message -> {
            message.setStatus(AppointmentMessageStatus.CANCELLED);
            message.setNextAttemptAt(null);
        });
        messageRepository.saveAll(messages);
    }

    private String normalizedPhone(String value) {
        if (value == null) return null;
        var digits = value.replaceAll("\\D", "");
        if (digits.length() == 10 || digits.length() == 11) digits = "55" + digits;
        return digits.length() >= 12 && digits.length() <= 15 ? digits : null;
    }

    private AppointmentMessagingSettings defaults(String clinicId) {
        var settings = new AppointmentMessagingSettings();
        settings.setClinicId(clinicId);
        settings.setConfirmationPreview(CONFIRMATION_PREVIEW);
        settings.setReminderPreview(REMINDER_PREVIEW);
        return settings;
    }

    private SettingsResponse response(AppointmentMessagingSettings settings) {
        return new SettingsResponse(
                properties.metaConfigured(),
                settings.isWhatsappEnabled(),
                settings.getConfirmationTemplateName(),
                settings.getReminderTemplateName(),
                settings.getLanguageCode(),
                settings.getConfirmationPreview(),
                settings.getReminderPreview(),
                settings.getFirstReminderHours(),
                settings.getSecondReminderHours(),
                settings.getMaxAttempts(),
                settings.getRetryMinutes(),
                true
        );
    }

    private MessageResponse response(AppointmentMessage message) {
        return new MessageResponse(
                message.getId(), message.getChannel(), message.getPurpose(), message.getDirection(),
                message.getStatus(), message.getRecipient(), message.getTemplateName(),
                message.getScheduledAt(), message.getAttemptCount(), message.getMaxAttempts(),
                message.getProviderMessageId(), message.getResponseAction(), message.getErrorMessage(),
                message.getSentAt(), message.getDeliveredAt(), message.getReadAt(),
                message.getRespondedAt(), message.getCreatedAt()
        );
    }
}
