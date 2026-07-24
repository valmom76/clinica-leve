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

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "account_action_tokens")
public class AccountActionToken extends TenantEntity {

    @Column(nullable = false, length = 36, updatable = false)
    private String userId;

    @Column(nullable = false, length = 64, unique = true, updatable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AccountActionPurpose purpose;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant usedAt;

    @Column(length = 64, updatable = false)
    private String requestedIp;
}
