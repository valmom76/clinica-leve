package br.com.clinicaleve.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken, String> {

    Optional<AccountActionToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    long countByUserIdAndPurposeAndCreatedAtAfter(
            String userId,
            AccountActionPurpose purpose,
            Instant createdAfter
    );

    @Modifying
    @Query("""
            update AccountActionToken token
               set token.usedAt = :usedAt
             where token.clinicId = :clinicId
               and token.userId = :userId
               and token.usedAt is null
            """)
    int invalidateOutstanding(
            @Param("clinicId") String clinicId,
            @Param("userId") String userId,
            @Param("usedAt") Instant usedAt
    );
}
