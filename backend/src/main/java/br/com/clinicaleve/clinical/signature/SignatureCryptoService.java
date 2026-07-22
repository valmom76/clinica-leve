package br.com.clinicaleve.clinical.signature;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SignatureCryptoService {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SignatureProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptedPayload encrypt(byte[] clearContent, String context) {
        var iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new EncryptedPayload(cipher.doFinal(clearContent), iv);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível proteger a credencial de assinatura", exception);
        }
    }

    public byte[] decrypt(byte[] encryptedContent, byte[] iv, String context) {
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(encryptedContent);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível abrir a credencial de assinatura", exception);
        }
    }

    public void requireConfigured() {
        if (!properties.enabled()) {
            throw new IllegalStateException("O módulo de assinatura digital ainda não foi habilitado");
        }
        key();
    }

    private SecretKeySpec key() {
        if (properties.masterKey() == null || properties.masterKey().isBlank()) {
            throw new IllegalStateException("SIGNATURE_MASTER_KEY não configurada");
        }
        try {
            var decoded = Base64.getDecoder().decode(properties.masterKey().trim());
            if (decoded.length != 32) {
                throw new IllegalStateException("SIGNATURE_MASTER_KEY deve conter 32 bytes em Base64");
            }
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("SIGNATURE_MASTER_KEY não está em Base64 válido", exception);
        }
    }

    public record EncryptedPayload(byte[] content, byte[] iv) {
    }
}
