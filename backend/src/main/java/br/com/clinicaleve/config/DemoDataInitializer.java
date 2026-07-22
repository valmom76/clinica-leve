package br.com.clinicaleve.config;

import br.com.clinicaleve.appointment.Appointment;
import br.com.clinicaleve.appointment.AppointmentRepository;
import br.com.clinicaleve.appointment.AppointmentStatus;
import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.auth.AppUserRepository;
import br.com.clinicaleve.auth.Role;
import br.com.clinicaleve.clinical.ClinicalTemplateDefaults;
import br.com.clinicaleve.patient.Patient;
import br.com.clinicaleve.patient.PatientRepository;
import br.com.clinicaleve.professional.Professional;
import br.com.clinicaleve.professional.ProfessionalRepository;
import br.com.clinicaleve.specialty.Specialty;
import br.com.clinicaleve.specialty.SpecialtyRepository;
import br.com.clinicaleve.tenant.Clinic;
import br.com.clinicaleve.tenant.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final ClinicRepository clinicRepository;
    private final AppUserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ProfessionalRepository professionalRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClinicalTemplateDefaults clinicalTemplateDefaults;

    @Value("${app.seed.enabled}")
    private boolean enabled;
    @Value("${app.seed.clinic-name}")
    private String clinicName;
    @Value("${app.seed.clinic-slug}")
    private String clinicSlug;
    @Value("${app.seed.admin-name}")
    private String adminName;
    @Value("${app.seed.admin-email}")
    private String adminEmail;
    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var normalizedSlug = clinicSlug.trim().toLowerCase();
        if (!enabled || clinicRepository.findBySlugAndActiveTrue(normalizedSlug).isPresent()) {
            return;
        }

        var clinic = new Clinic();
        clinic.setName(clinicName);
        clinic.setSlug(normalizedSlug);
        clinic.setTimezone("America/Fortaleza");
        clinicRepository.save(clinic);

        var admin = new AppUser();
        admin.setClinicId(clinic.getId());
        admin.setName(adminName);
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        clinicalTemplateDefaults.ensureDefaults(clinic.getId(), admin.getId());

        var fisioterapia = specialty(clinic, "Fisioterapia", "#5e9f89");
        var psicologia = specialty(clinic, "Psicologia", "#7b7096");
        var odontologia = specialty(clinic, "Odontologia", "#c97861");
        specialtyRepository.saveAll(List.of(fisioterapia, psicologia, odontologia));

        var marta = professional(
                clinic,
                fisioterapia,
                "Dra. Marta Rizzini",
                "CREFITO 3843-F"
        );
        var camila = professional(clinic, psicologia, "Dra. Camila Souza", "CRP 22/04123");
        var marina = professional(clinic, odontologia, "Dra. Marina Alves", "CRO-MA 8821");
        professionalRepository.saveAll(List.of(marta, camila, marina));

        var ana = patient(clinic, "Ana Beatriz Costa", "(98) 98812-4401");
        var julia = patient(clinic, "Júlia Ramos Ferreira", "(98) 98291-1250");
        var roberto = patient(clinic, "Roberto Lima Santos", "(98) 98440-0192");
        patientRepository.saveAll(List.of(ana, julia, roberto));

        var zone = ZoneId.of(clinic.getTimezone());
        var baseDate = LocalDate.now(zone).plusDays(1);
        appointmentRepository.saveAll(List.of(
                appointment(
                        clinic,
                        ana,
                        marta,
                        fisioterapia,
                        LocalDateTime.of(baseDate, java.time.LocalTime.of(8, 0)),
                        zone
                ),
                appointment(
                        clinic,
                        julia,
                        camila,
                        psicologia,
                        LocalDateTime.of(baseDate, java.time.LocalTime.of(10, 0)),
                        zone
                ),
                appointment(
                        clinic,
                        roberto,
                        marina,
                        odontologia,
                        LocalDateTime.of(baseDate, java.time.LocalTime.of(14, 0)),
                        zone
                )
        ));
    }

    private Specialty specialty(Clinic clinic, String name, String color) {
        var specialty = new Specialty();
        specialty.setClinicId(clinic.getId());
        specialty.setName(name);
        specialty.setColor(color);
        return specialty;
    }

    private Professional professional(
            Clinic clinic,
            Specialty specialty,
            String name,
            String council
    ) {
        var professional = new Professional();
        professional.setClinicId(clinic.getId());
        professional.setSpecialtyId(specialty.getId());
        professional.setName(name);
        professional.setCouncil(council);
        return professional;
    }

    private Patient patient(Clinic clinic, String name, String phone) {
        var patient = new Patient();
        patient.setClinicId(clinic.getId());
        patient.setName(name);
        patient.setPhone(phone);
        return patient;
    }

    private Appointment appointment(
            Clinic clinic,
            Patient patient,
            Professional professional,
            Specialty specialty,
            LocalDateTime start,
            ZoneId zone
    ) {
        var appointment = new Appointment();
        appointment.setClinicId(clinic.getId());
        appointment.setPatientId(patient.getId());
        appointment.setProfessionalId(professional.getId());
        appointment.setSpecialtyId(specialty.getId());
        appointment.setStartAt(start.atZone(zone).toInstant());
        appointment.setEndAt(start.plusHours(1).atZone(zone).toInstant());
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointment;
    }
}
