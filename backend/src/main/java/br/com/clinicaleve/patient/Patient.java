package br.com.clinicaleve.patient;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patients")
public class Patient extends TenantEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 14)
    private String cpf;

    private LocalDate birthDate;

    @Column(length = 190)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false)
    private boolean whatsappOptIn;

    private Instant whatsappOptInAt;

    @Column(length = 36)
    private String whatsappOptInRecordedBy;

    @Column(nullable = false)
    private boolean active = true;
}
