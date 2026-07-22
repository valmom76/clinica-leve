package br.com.clinicaleve.billing;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AsaasClient {
    private final RestClient asaasRestClient;
    private final AsaasProperties properties;
    private final ObjectMapper objectMapper;

    public String syncCustomer(ClinicBillingProfile profile) {
        ensureConfigured();
        var body = customerBody(profile);
        if (profile.getAsaasCustomerId() == null) {
            var response = post("/customers", body);
            return requiredText(response, "id", "O Asaas não retornou o identificador do cliente");
        }
        var response = put("/customers/{id}", profile.getAsaasCustomerId(), body);
        return requiredText(response, "id", "O Asaas não retornou o identificador do cliente");
    }

    public CheckoutResult createCardCheckout(
            ClinicSubscription subscription,
            SubscriptionPlan plan,
            String customerId,
            LocalDate firstDueDate
    ) {
        ensureConfigured();
        var callback = Map.of(
                "successUrl", properties.callbackUrl("success"),
                "cancelUrl", properties.callbackUrl("cancel"),
                "expiredUrl", properties.callbackUrl("expired")
        );
        var item = new LinkedHashMap<String, Object>();
        item.put("externalReference", plan.getCode());
        item.put("name", plan.getName());
        item.put("description", plan.getDescription());
        item.put("quantity", 1);
        item.put("value", plan.getPrice());

        var recurring = new LinkedHashMap<String, Object>();
        recurring.put("cycle", plan.getBillingCycle().name());
        recurring.put("nextDueDate", firstDueDate + " 12:00:00");

        var body = new LinkedHashMap<String, Object>();
        body.put("billingTypes", List.of("CREDIT_CARD"));
        body.put("chargeTypes", List.of("RECURRENT"));
        body.put("minutesToExpire", 60);
        body.put("externalReference", subscription.getId());
        body.put("callback", callback);
        body.put("items", List.of(item));
        body.put("customer", customerId);
        body.put("subscription", recurring);

        var response = post("/checkouts", body);
        return new CheckoutResult(
                requiredText(response, "id", "O Asaas não retornou o checkout"),
                requiredText(response, "link", "O Asaas não retornou o link do checkout")
        );
    }

    public SubscriptionResult createPixSubscription(
            ClinicSubscription subscription,
            SubscriptionPlan plan,
            String customerId,
            LocalDate firstDueDate
    ) {
        ensureConfigured();
        var body = new LinkedHashMap<String, Object>();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("nextDueDate", firstDueDate.toString());
        body.put("value", plan.getPrice());
        body.put("cycle", plan.getBillingCycle().name());
        body.put("description", plan.getName());
        body.put("externalReference", subscription.getId());
        var response = post("/subscriptions", body);
        return new SubscriptionResult(
                requiredText(response, "id", "O Asaas não retornou a assinatura"),
                response.path("status").asText("ACTIVE")
        );
    }

    public List<RemotePayment> listSubscriptionPayments(String asaasSubscriptionId) {
        ensureConfigured();
        try {
            RestClient.RequestHeadersSpec<?> request = asaasRestClient.get()
                    .uri(uri -> uri.path("/subscriptions/{id}/payments")
                            .queryParam("limit", 100)
                            .queryParam("offset", 0)
                            .build(asaasSubscriptionId));
            request.header("access_token", properties.apiKey());
            JsonNode response = request
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("data").isArray()) return List.of();
            return objectMapper.convertValue(
                    response.path("data"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RemotePayment.class)
            );
        } catch (RestClientResponseException exception) {
            throw providerException(exception);
        } catch (RuntimeException exception) {
            throw new AsaasIntegrationException("Não foi possível consultar as cobranças no Asaas", exception);
        }
    }

    public void cancelSubscription(String asaasSubscriptionId) {
        ensureConfigured();
        try {
            RestClient.RequestHeadersSpec<?> request = asaasRestClient.delete()
                    .uri("/subscriptions/{id}", asaasSubscriptionId);
            request.header("access_token", properties.apiKey());
            request
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw providerException(exception);
        } catch (RuntimeException exception) {
            throw new AsaasIntegrationException("Não foi possível cancelar a assinatura no Asaas", exception);
        }
    }

    private Map<String, Object> customerBody(ClinicBillingProfile profile) {
        var body = new LinkedHashMap<String, Object>();
        body.put("name", profile.getLegalName());
        body.put("cpfCnpj", profile.getCpfCnpj());
        body.put("email", profile.getEmail());
        body.put("mobilePhone", profile.getPhone());
        body.put("externalReference", profile.getClinicId());
        putIfPresent(body, "postalCode", profile.getPostalCode());
        putIfPresent(body, "address", profile.getAddress());
        putIfPresent(body, "addressNumber", profile.getAddressNumber());
        putIfPresent(body, "complement", profile.getComplement());
        putIfPresent(body, "province", profile.getProvince());
        return body;
    }

    private JsonNode post(String path, Object body) {
        try {
            return asaasRestClient.post()
                    .uri(path)
                    .header("access_token", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw providerException(exception);
        } catch (RuntimeException exception) {
            throw new AsaasIntegrationException("Não foi possível comunicar com o Asaas", exception);
        }
    }

    private JsonNode put(String path, String id, Object body) {
        try {
            return asaasRestClient.put()
                    .uri(path, id)
                    .header("access_token", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw providerException(exception);
        } catch (RuntimeException exception) {
            throw new AsaasIntegrationException("Não foi possível comunicar com o Asaas", exception);
        }
    }

    private AsaasIntegrationException providerException(RestClientResponseException exception) {
        try {
            var payload = objectMapper.readTree(exception.getResponseBodyAsString());
            var description = payload.path("errors").path(0).path("description").asText();
            if (!description.isBlank()) {
                return new AsaasIntegrationException("Asaas: " + description, exception);
            }
        } catch (Exception ignored) {
            // A resposta pode não estar em JSON. A mensagem segura abaixo é suficiente para o usuário.
        }
        return new AsaasIntegrationException("O Asaas recusou a operação. Verifique os dados e tente novamente.", exception);
    }

    private String requiredText(JsonNode response, String field, String message) {
        if (response == null || response.path(field).asText().isBlank()) {
            throw new AsaasIntegrationException(message);
        }
        return response.path(field).asText();
    }

    private void putIfPresent(Map<String, Object> body, String key, String value) {
        if (value != null && !value.isBlank()) body.put(key, value);
    }

    private void ensureConfigured() {
        if (!properties.configured()) {
            throw new IllegalStateException("A integração Asaas ainda não foi configurada neste ambiente");
        }
    }

    public record CheckoutResult(String id, String link) {
    }

    public record SubscriptionResult(String id, String status) {
    }

    public record RemotePayment(
            String id,
            String status,
            String billingType,
            BigDecimal value,
            LocalDate dueDate,
            String paymentDate,
            String clientPaymentDate,
            String invoiceUrl,
            String bankSlipUrl,
            String description,
            String subscription,
            String externalReference,
            String customer
    ) {
    }
}
