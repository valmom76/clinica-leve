package br.com.clinicaleve.auth;

import br.com.clinicaleve.tenant.Clinic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public TokenResult create(AppUser user, Clinic clinic) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("clinicaleve")
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .subject(user.getId())
                .claim("clinicId", clinic.getId())
                .claim("clinicSlug", clinic.getSlug())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .claim("tokenVersion", user.getTokenVersion())
                .build();
        var headers = org.springframework.security.oauth2.jwt.JwsHeader
                .with(MacAlgorithm.HS256)
                .build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
        return new TokenResult(token, expiration.toSeconds());
    }

    public record TokenResult(String value, long expiresInSeconds) {
    }
}
