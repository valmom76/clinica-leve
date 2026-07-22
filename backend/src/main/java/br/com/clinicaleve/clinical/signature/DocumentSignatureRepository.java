package br.com.clinicaleve.clinical.signature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentSignatureRepository extends JpaRepository<DocumentSignature, String> {

    Optional<DocumentSignature> findByDocumentIdAndClinicId(String documentId, String clinicId);

    Optional<DocumentSignature> findByIdAndClinicId(String id, String clinicId);
}
