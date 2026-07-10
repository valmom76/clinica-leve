package br.com.clinicaleve.appointment;

import br.com.clinicaleve.appointment.AppointmentDtos.AppointmentRequest;
import br.com.clinicaleve.appointment.AppointmentDtos.AppointmentResponse;
import br.com.clinicaleve.patient.Patient;
import br.com.clinicaleve.patient.PatientRepository;
import br.com.clinicaleve.professional.Professional;
import br.com.clinicaleve.professional.ProfessionalRepository;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.specialty.Specialty;
import br.com.clinicaleve.specialty.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;
    private final SpecialtyRepository specialtyRepository;

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(Instant from, Instant to) {
        var clinicId = TenantAccess.currentClinicId();
        var rangeStart = from == null ? Instant.now().minus(7, ChronoUnit.DAYS) : from;
        var rangeEnd = to == null ? Instant.now().plus(30, ChronoUnit.DAYS) : to;
        if (!rangeEnd.isAfter(rangeStart)) {
            throw new IllegalArgumentException("Período da agenda inválido");
        }

        var appointments = repository
                .findByClinicIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
                        clinicId,
                        rangeStart,
                        rangeEnd
                );
        var patients = mapPatients(clinicId, appointments);
        var professionals = mapProfessionals(clinicId, appointments);
        var specialties = mapSpecialties(clinicId, appointments);
        return appointments.stream()
                .map(appointment -> response(
                        appointment,
                        patients.get(appointment.getPatientId()),
                        professionals.get(appointment.getProfessionalId()),
                        specialties.get(appointment.getSpecialtyId())
                ))
                .toList();
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("O fim deve ser posterior ao início");
        }

        var patient = patientRepository.findByIdAndClinicId(request.patientId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));
        var professional = professionalRepository
                .findByIdAndClinicId(request.professionalId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));
        var specialty = specialtyRepository.findByIdAndClinicId(request.specialtyId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Especialidade não encontrada"));

        if (!professional.getSpecialtyId().equals(specialty.getId())) {
            throw new IllegalArgumentException("Especialidade incompatível com o profissional");
        }
        if (repository.hasConflict(
                clinicId,
                professional.getId(),
                request.startAt(),
                request.endAt()
        )) {
            throw new IllegalArgumentException("O profissional já possui atendimento nesse horário");
        }

        var appointment = new Appointment();
        appointment.setClinicId(clinicId);
        appointment.setPatientId(patient.getId());
        appointment.setProfessionalId(professional.getId());
        appointment.setSpecialtyId(specialty.getId());
        appointment.setStartAt(request.startAt());
        appointment.setEndAt(request.endAt());
        appointment.setStatus(
                request.status() == null ? AppointmentStatus.SCHEDULED : request.status()
        );
        appointment.setNotes(blankToNull(request.notes()));
        return response(repository.save(appointment), patient, professional, specialty);
    }

    private Map<String, Patient> mapPatients(String clinicId, List<Appointment> appointments) {
        return patientRepository.findAllById(
                        appointments.stream().map(Appointment::getPatientId).toList())
                .stream()
                .filter(patient -> clinicId.equals(patient.getClinicId()))
                .collect(Collectors.toMap(Patient::getId, Function.identity()));
    }

    private Map<String, Professional> mapProfessionals(
            String clinicId,
            List<Appointment> appointments
    ) {
        return professionalRepository.findAllById(
                        appointments.stream().map(Appointment::getProfessionalId).toList())
                .stream()
                .filter(professional -> clinicId.equals(professional.getClinicId()))
                .collect(Collectors.toMap(Professional::getId, Function.identity()));
    }

    private Map<String, Specialty> mapSpecialties(
            String clinicId,
            List<Appointment> appointments
    ) {
        return specialtyRepository.findAllById(
                        appointments.stream().map(Appointment::getSpecialtyId).toList())
                .stream()
                .filter(specialty -> clinicId.equals(specialty.getClinicId()))
                .collect(Collectors.toMap(Specialty::getId, Function.identity()));
    }

    private AppointmentResponse response(
            Appointment appointment,
            Patient patient,
            Professional professional,
            Specialty specialty
    ) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                patient == null ? "Paciente indisponível" : patient.getName(),
                appointment.getProfessionalId(),
                professional == null ? "Profissional indisponível" : professional.getName(),
                appointment.getSpecialtyId(),
                specialty == null ? "Especialidade indisponível" : specialty.getName(),
                specialty == null ? "#8b9692" : specialty.getColor(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getStatus(),
                appointment.getNotes()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
