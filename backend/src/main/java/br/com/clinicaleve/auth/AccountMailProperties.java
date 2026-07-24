package br.com.clinicaleve.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.account-mail")
public record AccountMailProperties(
        boolean enabled,
        String from,
        String replyTo,
        int resetExpirationMinutes,
        int invitationExpirationHours
) {
}
