package br.com.clinicaleve.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface ClinicSubscriptionRepository extends JpaRepository<ClinicSubscription, String> {
    Optional<ClinicSubscription> findByClinicId(String clinicId);
    Optional<ClinicSubscription> findByAsaasSubscriptionId(String asaasSubscriptionId);
    Optional<ClinicSubscription> findByAsaasCheckoutId(String asaasCheckoutId);
    long countByPlanIdAndStatusIn(String planId, Collection<SubscriptionStatus> statuses);
}
