package br.com.clinicaleve.auth;

import br.com.clinicaleve.shared.TenantEntity;
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
@Table(name = "app_users")
public class AppUser extends TenantEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(length = 36)
    private String professionalId;

    @Column(nullable = false)
    private int expectedDailyMinutes = 480;

    @Column(nullable = false)
    private boolean active = true;
}
