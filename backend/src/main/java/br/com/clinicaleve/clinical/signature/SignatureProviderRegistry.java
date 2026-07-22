package br.com.clinicaleve.clinical.signature;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;

@Component
public class SignatureProviderRegistry {

    private final EnumMap<SignatureMode, DigitalSignatureProvider> providers = new EnumMap<>(SignatureMode.class);

    public SignatureProviderRegistry(List<DigitalSignatureProvider> implementations) {
        implementations.forEach(provider -> providers.put(provider.mode(), provider));
    }

    public DigitalSignatureProvider require(SignatureMode mode) {
        var provider = providers.get(mode);
        if (provider == null) {
            throw new IllegalStateException("Método de assinatura não disponível: " + mode);
        }
        return provider;
    }
}
