package br.com.clinicaleve.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubscriptionProvisioningService {
    private static final String DEFAULT_PLAN_CODE = "CLINICA_LEVE_MONTHLY";

    private final ClinicSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionAccessPolicy accessPolicy;

    @Transactional
    public ClinicSubscription currentOrCreate(String clinicId) {
        var subscription = subscriptionRepository.findByClinicId(clinicId)
                .orElseGet(() -> createTrial(clinicId));
        return normalizeTemporalStatus(subscription, Instant.now());
    }

    public SubscriptionAccessMode accessMode(ClinicSubscription subscription) {
        return accessPolicy.evaluate(subscription, Instant.now());
    }

    private ClinicSubscription createTrial(String clinicId) {
        var plan = planRepository.findFirstByCode(DEFAULT_PLAN_CODE)
                .orElseThrow(() -> new IllegalStateException("Plano padrão não configurado"));
        var subscription = new ClinicSubscription();
        subscription.setClinicId(clinicId);
        subscription.setPlanId(plan.getId());
        subscription.setStatus(SubscriptionStatus.TRIAL);
        subscription.setBillingCycle(plan.getBillingCycle());
        subscription.setAmount(plan.getPrice());
        subscription.setTrialEndsAt(Instant.now().plusSeconds(plan.getTrialDays() * 24L * 60L * 60L));
        return subscriptionRepository.save(subscription);
    }

    private ClinicSubscription normalizeTemporalStatus(ClinicSubscription subscription, Instant now) {
        var mode = accessPolicy.evaluate(subscription, now);
        if (mode == SubscriptionAccessMode.FULL) return subscription;

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            subscription.setStatus(SubscriptionStatus.SUSPENDED);
            return subscriptionRepository.save(subscription);
        }
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE && subscription.isCancelAtPeriodEnd()) {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            if (subscription.getCanceledAt() == null) subscription.setCanceledAt(now);
            return subscriptionRepository.save(subscription);
        }
        return subscription;
    }
}
