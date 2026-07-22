package br.com.clinicaleve.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> {
    List<SubscriptionPlan> findByActiveTrueAndVisibleTrueOrderByDisplayOrderAsc();
    Optional<SubscriptionPlan> findByCodeAndActiveTrue(String code);
    Optional<SubscriptionPlan> findFirstByCode(String code);
}
