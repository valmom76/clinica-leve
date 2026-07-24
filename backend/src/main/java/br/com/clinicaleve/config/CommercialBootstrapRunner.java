package br.com.clinicaleve.config;

import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.auth.AppUserRepository;
import br.com.clinicaleve.auth.PasswordPolicy;
import br.com.clinicaleve.auth.Role;
import br.com.clinicaleve.billing.SubscriptionProvisioningService;
import br.com.clinicaleve.clinical.ClinicalTemplateDefaults;
import br.com.clinicaleve.tenant.Clinic;
import br.com.clinicaleve.tenant.ClinicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Profile("prod")
@Order(10)
public class CommercialBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommercialBootstrapRunner.class);

    private final ClinicRepository clinicRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final ClinicalTemplateDefaults templateDefaults;
    private final SubscriptionProvisioningService subscriptionProvisioningService;

    @Value("${app.bootstrap.enabled:false}")
    private boolean enabled;
    @Value("${app.bootstrap.clinic-name:}")
    private String clinicName;
    @Value("${app.bootstrap.clinic-slug:}")
    private String clinicSlug;
    @Value("${app.bootstrap.admin-name:}")
    private String adminName;
    @Value("${app.bootstrap.admin-email:}")
    private String adminEmail;
    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    public CommercialBootstrapRunner(
            ClinicRepository clinicRepository,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            ClinicalTemplateDefaults templateDefaults,
            SubscriptionProvisioningService subscriptionProvisioningService
    ) {
        this.clinicRepository = clinicRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.templateDefaults = templateDefaults;
        this.subscriptionProvisioningService = subscriptionProvisioningService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        var normalizedSlug = clinicSlug.trim().toLowerCase();
        if (clinicRepository.findBySlugAndActiveTrue(normalizedSlug).isPresent()) {
            LOGGER.info("Bootstrap ignorado: a clínica configurada já existe");
            return;
        }
        passwordPolicy.validate(adminPassword);

        var clinic = new Clinic();
        clinic.setName(clinicName.trim());
        clinic.setSlug(normalizedSlug);
        clinic.setTimezone("America/Fortaleza");
        clinicRepository.save(clinic);

        var admin = new AppUser();
        admin.setClinicId(clinic.getId());
        admin.setName(adminName.trim());
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setCredentialsUpdatedAt(Instant.now());
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        templateDefaults.ensureDefaults(clinic.getId(), admin.getId());
        subscriptionProvisioningService.currentOrCreate(clinic.getId());
        LOGGER.info("Bootstrap comercial concluído para a clínica {}", clinic.getId());
    }
}
