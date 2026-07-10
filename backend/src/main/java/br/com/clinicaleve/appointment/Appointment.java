package br.com.clinicaleve.appointment;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment extends TenantEntity {

    @Column(nullable = false, length = 36)
    private String patientId;

    @Column(nullable = false, length = 36)
    private String professionalId;

    @Column(nullable = false, length = 36)
    private String specialtyId;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(length = 1000)
    private String notes;
}
