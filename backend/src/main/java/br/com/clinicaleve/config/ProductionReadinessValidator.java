package br.com.clinicaleve.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;

@Component
@Profile("prod")
@Order(0)
public class ProductionReadinessValidator implements ApplicationRunner {

    private final Environment environment;

    public ProductionReadinessValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        var problems = new ArrayList<String>();
        requireFalse("app.seed.enabled", problems);
        requireStrongSecret("security.jwt.secret", 48, problems);
        requireHttps("app.public-url", problems);
        requireHttpsOrigins("app.cors.allowed-origins", problems);
        requireChanged("spring.datasource.password", "clinicaleve", problems);

        if (environment.getProperty("app.bootstrap.enabled", Boolean.class, false)) {
            requireNonBlank("app.bootstrap.clinic-name", problems);
            requireNonBlank("app.bootstrap.clinic-slug", problems);
            requireNonBlank("app.bootstrap.admin-name", problems);
            requireNonBlank("app.bootstrap.admin-email", problems);
            requireStrongSecret("app.bootstrap.admin-password", 10, problems);
        }

        if (environment.getProperty("app.account-mail.enabled", Boolean.class, false)) {
            requireNonBlank("app.account-mail.from", problems);
            requireNonBlank("spring.mail.host", problems);
            requireNonBlank("spring.mail.username", problems);
            requireNonBlank("spring.mail.password", problems);
        }
        if (environment.getProperty("app.asaas.enabled", Boolean.class, false)) {
            var asaasEnvironment = environment.getProperty("app.asaas.environment", "");
            if (!"production".equalsIgnoreCase(asaasEnvironment)) {
                problems.add("app.asaas.environment deve ser production quando o Asaas estiver ativo");
            }
            requireNonBlank("app.asaas.api-key", problems);
            requireStrongSecret("app.asaas.webhook-token", 32, problems);
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Configuração de produção recusada:\n - " + String.join("\n - ", problems)
            );
        }
    }

    private void requireFalse(String key, ArrayList<String> problems) {
        if (environment.getProperty(key, Boolean.class, true)) {
            problems.add(key + " deve estar desabilitado");
        }
    }

    private void requireHttps(String key, ArrayList<String> problems) {
        var value = environment.getProperty(key, "").trim();
        try {
            if (!"https".equalsIgnoreCase(URI.create(value).getScheme())) {
                problems.add(key + " deve usar HTTPS");
            }
        } catch (IllegalArgumentException exception) {
            problems.add(key + " deve ser uma URL válida");
        }
    }

    private void requireHttpsOrigins(String key, ArrayList<String> problems) {
        var origins = environment.getProperty(key, "").split(",");
        if (origins.length == 0) {
            problems.add(key + " deve conter ao menos uma origem");
            return;
        }
        for (var origin : origins) {
            if (!origin.trim().startsWith("https://")) {
                problems.add(key + " deve conter somente origens HTTPS explícitas");
                return;
            }
        }
    }

    private void requireStrongSecret(String key, int minimumLength, ArrayList<String> problems) {
        var value = environment.getProperty(key, "").trim();
        if (value.length() < minimumLength || value.toLowerCase().contains("troque")) {
            problems.add(key + " deve ser um segredo exclusivo com ao menos " + minimumLength + " caracteres");
        }
    }

    private void requireNonBlank(String key, ArrayList<String> problems) {
        if (environment.getProperty(key, "").isBlank()) {
            problems.add(key + " não pode ficar vazio");
        }
    }

    private void requireChanged(String key, String insecureDefault, ArrayList<String> problems) {
        var value = environment.getProperty(key, "");
        if (value.isBlank() || insecureDefault.equals(value) || value.toLowerCase().contains("troque")) {
            problems.add(key + " deve ser alterado para produção");
        }
    }
}
