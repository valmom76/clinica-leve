package br.com.clinicaleve.shared;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @Column(nullable = false, length = 36, updatable = false)
    private String clinicId;
}
