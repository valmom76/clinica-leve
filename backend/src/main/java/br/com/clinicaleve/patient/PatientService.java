package br.com.clinicaleve.patient;

import br.com.clinicaleve.appointment.AppointmentRepository;
import br.com.clinicaleve.appointment.messaging.AppointmentMessagingService;
import br.com.clinicaleve.patient.PatientDtos.PatientRequest;
import br.com.clinicaleve.patient.PatientDtos.PatientResponse;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository repository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMessagingService messagingService;

    @Transactional(readOnly = true)
    public List<PatientResponse> list(String search) {
        var clinicId = TenantAccess.currentClinicId();
        var patients = search == null || search.isBlank()
                ? repository.findByClinicIdAndActiveTrueOrderByName(clinicId)
                : repository.findByClinicIdAndActiveTrueAndNameContainingIgnoreCaseOrderByName(
                        clinicId,
                        search.trim()
                );
        return patients.stream().map(PatientResponse::from).toList();
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        var patient = new Patient();
        patient.setClinicId(TenantAccess.currentClinicId());
        patient.setName(request.name().trim());
        patient.setCpf(blankToNull(request.cpf()));
        patient.setBirthDate(request.birthDate());
        patient.setEmail(blankToNull(request.email()));
        patient.setPhone(request.phone().trim());
        updateWhatsAppConsent(patient, request.whatsappOptIn());
        return PatientResponse.from(repository.save(patient));
    }

    @Transactional
    public PatientResponse update(String id, PatientRequest request) {
        var patient = repository.findByIdAndClinicId(id, TenantAccess.currentClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));
        var clinicId = patient.getClinicId();
        var previousOptIn = patient.isWhatsappOptIn();
        patient.setName(request.name().trim());
        patient.setCpf(blankToNull(request.cpf()));
        patient.setBirthDate(request.birthDate());
        patient.setEmail(blankToNull(request.email()));
        patient.setPhone(request.phone().trim());
        updateWhatsAppConsent(patient, request.whatsappOptIn());
        var saved = repository.save(patient);
        if (previousOptIn != saved.isWhatsappOptIn()) {
            appointmentRepository.findFutureOpen(clinicId, Instant.now()).stream()
                    .filter(appointment -> saved.getId().equals(appointment.getPatientId()))
                    .forEach(appointment -> {
                        if (saved.isWhatsappOptIn()) {
                            messagingService.rebuildQueue(appointment);
                        } else {
                            messagingService.cancelPendingForAppointment(appointment.getId(), clinicId);
                        }
                    });
        }
        return PatientResponse.from(saved);
    }

    private void updateWhatsAppConsent(Patient patient, boolean optedIn) {
        if (optedIn && !patient.isWhatsappOptIn()) {
            patient.setWhatsappOptInAt(Instant.now());
            patient.setWhatsappOptInRecordedBy(TenantAccess.currentUserId());
        } else if (!optedIn) {
            patient.setWhatsappOptInAt(null);
            patient.setWhatsappOptInRecordedBy(null);
        }
        patient.setWhatsappOptIn(optedIn);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
