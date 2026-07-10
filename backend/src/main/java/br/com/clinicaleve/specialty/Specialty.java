package br.com.clinicaleve.specialty;

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
@Table(name = "specialties")
public class Specialty extends TenantEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String color = "#5e9f89";

    @Column(nullable = false)
    private boolean active = true;
}
