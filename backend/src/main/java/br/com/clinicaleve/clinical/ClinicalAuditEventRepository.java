package br.com.clinicaleve.clinical;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalAuditEventRepository extends JpaRepository<ClinicalAuditEvent, String> {
}
