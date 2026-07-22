package br.com.clinicaleve.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, String> {
    Optional<SubscriptionPayment> findByAsaasPaymentId(String asaasPaymentId);
    List<SubscriptionPayment> findByClinicIdOrderByDueDateDescCreatedAtDesc(String clinicId);
}
