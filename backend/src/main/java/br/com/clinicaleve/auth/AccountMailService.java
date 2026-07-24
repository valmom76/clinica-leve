package br.com.clinicaleve.auth;

import br.com.clinicaleve.tenant.Clinic;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountMailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AccountMailProperties properties;

    @Value("${app.public-url}")
    private String publicUrl;

    public boolean isEnabled() {
        return properties.enabled() && mailSenderProvider.getIfAvailable() != null;
    }

    public void sendPasswordReset(AppUser user, Clinic clinic, String rawToken) {
        send(
                user,
                "Redefinição de senha — Clínica Leve",
                """
                Olá, %s.

                Recebemos uma solicitação para redefinir sua senha de acesso à %s.

                Acesse o endereço abaixo para criar uma nova senha:
                %s

                Se você não solicitou a alteração, ignore esta mensagem. O link expira automaticamente.
                """.formatted(user.getName(), clinic.getName(), resetUrl(rawToken))
        );
    }

    public void sendInvitation(AppUser user, Clinic clinic, String rawToken) {
        send(
                user,
                "Seu acesso ao Clínica Leve",
                """
                Olá, %s.

                Você recebeu um convite para acessar %s pelo Clínica Leve.

                Crie sua senha no endereço abaixo:
                %s

                O convite é individual e expira automaticamente.
                """.formatted(user.getName(), clinic.getName(), resetUrl(rawToken))
        );
    }

    private void send(AppUser user, String subject, String body) {
        if (!properties.enabled()) {
            throw new IllegalStateException("O envio de e-mail ainda não está configurado");
        }
        var mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("O provedor SMTP ainda não está configurado");
        }
        if (properties.from() == null || properties.from().isBlank()) {
            throw new IllegalStateException("MAIL_FROM não foi configurado");
        }
        var message = new SimpleMailMessage();
        message.setFrom(properties.from().trim());
        if (properties.replyTo() != null && !properties.replyTo().isBlank()) {
            message.setReplyTo(properties.replyTo().trim());
        }
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String resetUrl(String rawToken) {
        var base = publicUrl == null ? "" : publicUrl.replaceAll("/+$", "");
        return base + "/reset-password?token=" + rawToken;
    }
}
