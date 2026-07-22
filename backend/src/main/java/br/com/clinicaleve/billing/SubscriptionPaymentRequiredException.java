package br.com.clinicaleve.billing;

public class SubscriptionPaymentRequiredException extends RuntimeException {
    public SubscriptionPaymentRequiredException(String message) {
        super(message);
    }
}
