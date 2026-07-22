package br.com.clinicaleve.tenant;

import br.com.clinicaleve.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clinics")
public class Clinic extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 60)
    private String timezone = "America/Fortaleza";

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 180)
    private String logoFileName;

    @Column(length = 40)
    private String logoContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ClinicTheme themeKey = ClinicTheme.CLINICAL_SERENE;
}
