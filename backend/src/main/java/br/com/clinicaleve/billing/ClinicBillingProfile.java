package br.com.clinicaleve.billing;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clinic_billing_profiles")
public class ClinicBillingProfile extends TenantEntity {

    @Column(nullable = false, length = 160)
    private String legalName;

    @Column(nullable = false, length = 14)
    private String cpfCnpj;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 8)
    private String postalCode;

    @Column(length = 180)
    private String address;

    @Column(length = 30)
    private String addressNumber;

    @Column(length = 120)
    private String complement;

    @Column(length = 100)
    private String province;

    @Column(length = 40, unique = true)
    private String asaasCustomerId;
}
