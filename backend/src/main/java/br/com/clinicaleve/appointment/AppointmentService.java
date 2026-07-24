package br.com.clinicaleve.appointment;

import br.com.clinicaleve.appointment.AppointmentDtos.AppointmentRequest;
import br.com.clinicaleve.appointment.AppointmentDtos.AppointmentReportResponse;
import br.com.clinicaleve.appointment.AppointmentDtos.AppointmentResponse;
import br.com.clinicaleve.appointment.messaging.AppointmentMessagingService;
import br.com.clinicaleve.patient.Patient;
import br.com.clinicaleve.patient.PatientRepository;
import br.com.clinicaleve.professional.Professional;
import br.com.clinicaleve.professional.ProfessionalRepository;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.specialty.Specialty;
import br.com.clinicaleve.specialty.SpecialtyRepository;
import br.com.clinicaleve.tenant.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ClinicRepository clinicRepository;
    private final AppointmentMessagingService messagingService;

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
        return responses(clinicId, appointments);
    }

    @Transactional(readOnly = true)
    public AppointmentReportResponse report(
            LocalDate from,
            LocalDate to,
            String professionalId,
            String specialtyId,
            AppointmentStatus status
    ) {
        validateReportPeriod(from, to);
        var clinicId = TenantAccess.currentClinicId();
        var timezone = clinicRepository.findById(clinicId)
                .map(clinic -> clinic.getTimezone())
                .orElse("America/Fortaleza");
        var zoneId = ZoneId.of(timezone);
        var appointments = repository
                .findByClinicIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
                        clinicId,
                        from.atStartOfDay(zoneId).toInstant(),
                        to.plusDays(1).atStartOfDay(zoneId).toInstant()
                )
                .stream()
                .filter(appointment -> professionalId == null
                        || professionalId.equals(appointment.getProfessionalId()))
                .filter(appointment -> specialtyId == null
                        || specialtyId.equals(appointment.getSpecialtyId()))
                .filter(appointment -> status == null || status == appointment.getStatus())
                .toList();

        var completed = countStatus(appointments, AppointmentStatus.COMPLETED);
        var cancelled = countStatus(appointments, AppointmentStatus.CANCELLED);
        var noShows = countStatus(appointments, AppointmentStatus.NO_SHOW);
        var attendanceBase = completed + noShows;
        var attendanceRate = attendanceBase == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(attendanceBase), 1, RoundingMode.HALF_UP);

        return new AppointmentReportResponse(
                from,
                to,
                appointments.size(),
                completed,
                cancelled,
                noShows,
                attendanceRate,
                responses(clinicId, appointments)
        );
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

        validateActive(patient, professional, specialty);
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
        var saved = repository.save(appointment);
        messagingService.rebuildQueue(saved);
        return response(saved, patient, professional, specialty);
    }

    @Transactional
    public AppointmentResponse update(String id, AppointmentRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var appointment = repository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Atendimentos concluídos ou cancelados não podem ser editados");
        }
        validatePeriod(request);

        var patient = patientRepository.findByIdAndClinicId(request.patientId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));
        var professional = professionalRepository
                .findByIdAndClinicId(request.professionalId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));
        var specialty = specialtyRepository.findByIdAndClinicId(request.specialtyId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Especialidade não encontrada"));
        validateActive(patient, professional, specialty);
        if (!professional.getSpecialtyId().equals(specialty.getId())) {
            throw new IllegalArgumentException("Especialidade incompatível com o profissional");
        }
        if (repository.hasConflictExcluding(
                clinicId, appointment.getId(), professional.getId(), request.startAt(), request.endAt()
        )) {
            throw new IllegalArgumentException("O profissional já possui atendimento nesse horário");
        }

        var schedulingChanged = !Objects.equals(appointment.getPatientId(), patient.getId())
                || !Objects.equals(appointment.getProfessionalId(), professional.getId())
                || !Objects.equals(appointment.getSpecialtyId(), specialty.getId())
                || !Objects.equals(appointment.getStartAt(), request.startAt())
                || !Objects.equals(appointment.getEndAt(), request.endAt());
        var previousStatus = appointment.getStatus();
        var nextStatus = request.status() == null ? previousStatus : request.status();
        if (schedulingChanged && !isTerminal(nextStatus)) {
            nextStatus = AppointmentStatus.SCHEDULED;
        }

        appointment.setPatientId(patient.getId());
        appointment.setProfessionalId(professional.getId());
        appointment.setSpecialtyId(specialty.getId());
        appointment.setStartAt(request.startAt());
        appointment.setEndAt(request.endAt());
        appointment.setStatus(nextStatus);
        appointment.setNotes(blankToNull(request.notes()));
        updateStatusTimestamps(appointment, previousStatus, nextStatus, schedulingChanged);
        var saved = repository.save(appointment);

        if (isTerminal(nextStatus) || nextStatus == AppointmentStatus.RESCHEDULE_REQUESTED) {
            messagingService.cancelPendingForAppointment(saved.getId(), clinicId);
        } else if (nextStatus == AppointmentStatus.CONFIRMED
                && previousStatus != AppointmentStatus.CONFIRMED) {
            messagingService.cancelPendingConfirmations(saved.getId(), clinicId);
        } else if (schedulingChanged
                || (previousStatus != AppointmentStatus.SCHEDULED
                && nextStatus == AppointmentStatus.SCHEDULED)) {
            messagingService.rebuildQueue(saved);
        }
        return response(saved, patient, professional, specialty);
    }

    @Transactional
    public AppointmentResponse cancel(String id) {
        var clinicId = TenantAccess.currentClinicId();
        var appointment = repository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Um atendimento concluído não pode ser cancelado");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        var saved = repository.save(appointment);
        messagingService.cancelPendingForAppointment(saved.getId(), clinicId);
        return responses(clinicId, List.of(saved)).get(0);
    }

    private Map<String, Patient> mapPatients(String clinicId, List<Appointment> appointments) {
        return patientRepository.findAllById(
                        appointments.stream().map(Appointment::getPatientId).toList())
                .stream()
                .filter(patient -> clinicId.equals(patient.getClinicId()))
                .collect(Collectors.toMap(Patient::getId, Function.identity()));
    }

    private List<AppointmentResponse> responses(
            String clinicId,
            List<Appointment> appointments
    ) {
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

    private int countStatus(List<Appointment> appointments, AppointmentStatus status) {
        return Math.toIntExact(appointments.stream()
                .filter(appointment -> appointment.getStatus() == status)
                .count());
    }

    private void validateReportPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Informe o período do relatório");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Período do relatório inválido");
        }
        if (ChronoUnit.DAYS.between(from, to) > 365) {
            throw new IllegalArgumentException("O relatório permite no máximo 366 dias");
        }
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
                appointment.getNotes(),
                appointment.getConfirmationRequestedAt(),
                appointment.getConfirmedAt(),
                appointment.getRescheduleRequestedAt()
        );
    }

    private void validatePeriod(AppointmentRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("O fim deve ser posterior ao início");
        }
    }

    private void validateActive(Patient patient, Professional professional, Specialty specialty) {
        if (!patient.isActive()) {
            throw new IllegalArgumentException("O paciente está inativo");
        }
        if (!professional.isActive()) {
            throw new IllegalArgumentException("O profissional está inativo");
        }
        if (!specialty.isActive()) {
            throw new IllegalArgumentException("A especialidade está inativa");
        }
    }

    private boolean isTerminal(AppointmentStatus status) {
        return status == AppointmentStatus.CANCELLED
                || status == AppointmentStatus.COMPLETED
                || status == AppointmentStatus.NO_SHOW;
    }

    private void updateStatusTimestamps(
            Appointment appointment,
            AppointmentStatus previousStatus,
            AppointmentStatus nextStatus,
            boolean schedulingChanged
    ) {
        if (schedulingChanged) {
            appointment.setConfirmedAt(null);
            appointment.setRescheduleRequestedAt(null);
        }
        if (nextStatus == AppointmentStatus.CONFIRMED
                && previousStatus != AppointmentStatus.CONFIRMED) {
            appointment.setConfirmedAt(Instant.now());
            appointment.setRescheduleRequestedAt(null);
        } else if (nextStatus == AppointmentStatus.RESCHEDULE_REQUESTED
                && previousStatus != AppointmentStatus.RESCHEDULE_REQUESTED) {
            appointment.setRescheduleRequestedAt(Instant.now());
        } else if (nextStatus == AppointmentStatus.SCHEDULED
                && previousStatus != AppointmentStatus.SCHEDULED) {
            appointment.setConfirmedAt(null);
            appointment.setRescheduleRequestedAt(null);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
