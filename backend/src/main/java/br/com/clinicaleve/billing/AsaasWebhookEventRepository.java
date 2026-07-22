package br.com.clinicaleve.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AsaasWebhookEventRepository extends JpaRepository<AsaasWebhookEvent, String> {
    Optional<AsaasWebhookEvent> findByAsaasEventId(String asaasEventId);
}
