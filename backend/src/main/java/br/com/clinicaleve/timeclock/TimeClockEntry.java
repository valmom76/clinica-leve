package br.com.clinicaleve.timeclock;

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
@Table(name = "time_clock_entries")
public class TimeClockEntry extends TenantEntity {

    @Column(nullable = false, length = 36, updatable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 30)
    private TimeEntryType type;

    @Column(nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeEntrySource source;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, length = 36, updatable = false)
    private String createdByUserId;

    @Column(length = 36)
    private String updatedByUserId;
}
