package br.com.clinicaleve.professional;

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
@Table(name = "professionals")
public class Professional extends TenantEntity {

    @Column(nullable = false, length = 36)
    private String specialtyId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 80)
    private String council;

    @Column(length = 190)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;
}
