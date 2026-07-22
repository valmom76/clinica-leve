package br.com.clinicaleve.clinical.signature;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
@EnableConfigurationProperties(SignatureProperties.class)
public class SignatureConfig {

    @PostConstruct
    void registerCryptoProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
