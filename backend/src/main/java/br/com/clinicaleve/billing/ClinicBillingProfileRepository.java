package br.com.clinicaleve.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicBillingProfileRepository extends JpaRepository<ClinicBillingProfile, String> {
    Optional<ClinicBillingProfile> findByClinicId(String clinicId);
    Optional<ClinicBillingProfile> findByAsaasCustomerId(String asaasCustomerId);
}
