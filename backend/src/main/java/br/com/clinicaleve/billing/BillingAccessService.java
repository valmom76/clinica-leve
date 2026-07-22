package br.com.clinicaleve.billing;

import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BillingAccessService {
    private final SubscriptionProvisioningService provisioningService;
    private final AsaasProperties asaasProperties;

    public void requireWriteAccess() {
        if (!asaasProperties.enabled()) {
            return;
        }
        var subscription = provisioningService.currentOrCreate(TenantAccess.currentClinicId());
        if (provisioningService.accessMode(subscription) == SubscriptionAccessMode.READ_ONLY) {
            throw new SubscriptionPaymentRequiredException(
                    "A assinatura da clínica precisa ser regularizada. Os dados continuam disponíveis para consulta e exportação."
            );
        }
    }
}
