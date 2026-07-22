package br.com.clinicaleve.clinical.signature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SignatureCredentialRepository extends JpaRepository<SignatureCredential, String> {

    List<SignatureCredential> findByClinicIdAndProfessionalIdOrderByActiveDescCreatedAtDesc(
            String clinicId,
            String professionalId
    );

    Optional<SignatureCredential> findByIdAndClinicIdAndProfessionalId(
            String id,
            String clinicId,
            String professionalId
    );

    boolean existsByClinicIdAndProfessionalIdAndFingerprintSha256(
            String clinicId,
            String professionalId,
            String fingerprint
    );

    Optional<SignatureCredential> findByClinicIdAndProfessionalIdAndFingerprintSha256(
            String clinicId,
            String professionalId,
            String fingerprint
    );
}
