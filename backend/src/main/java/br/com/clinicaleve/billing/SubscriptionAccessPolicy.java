package br.com.clinicaleve.billing;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;

@Component
public class SubscriptionAccessPolicy {

    public SubscriptionAccessMode evaluate(ClinicSubscription subscription, Instant now) {
        return switch (subscription.getStatus()) {
            case TRIAL, PENDING -> activeTrial(subscription, now)
                    ? SubscriptionAccessMode.FULL
                    : SubscriptionAccessMode.READ_ONLY;
            case ACTIVE -> canceledPeriodEnded(subscription, now)
                    ? SubscriptionAccessMode.READ_ONLY
                    : SubscriptionAccessMode.FULL;
            case PAST_DUE -> subscription.getGraceEndsAt() != null
                    && !now.isAfter(subscription.getGraceEndsAt())
                    ? SubscriptionAccessMode.FULL
                    : SubscriptionAccessMode.READ_ONLY;
            case SUSPENDED, CANCELED -> SubscriptionAccessMode.READ_ONLY;
        };
    }

    private boolean activeTrial(ClinicSubscription subscription, Instant now) {
        return subscription.getTrialEndsAt() != null && !now.isAfter(subscription.getTrialEndsAt());
    }

    private boolean canceledPeriodEnded(ClinicSubscription subscription, Instant now) {
        if (!subscription.isCancelAtPeriodEnd() || subscription.getNextDueDate() == null) {
            return false;
        }
        return now.atZone(ZoneOffset.UTC).toLocalDate().isAfter(subscription.getNextDueDate())
                || now.atZone(ZoneOffset.UTC).toLocalDate().isEqual(subscription.getNextDueDate());
    }
}
