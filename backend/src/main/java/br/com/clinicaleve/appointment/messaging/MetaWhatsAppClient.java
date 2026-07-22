package br.com.clinicaleve.appointment.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MetaWhatsAppClient implements AppointmentMessageSender {

    private final MessagingProperties properties;

    @Override
    public MessageChannel channel() {
        return MessageChannel.WHATSAPP;
    }

    @Override
    public String send(TemplateMessage message) {
        if (!properties.metaConfigured()) {
            throw new IllegalStateException("A integração WhatsApp da plataforma não está configurada");
        }
        var template = new LinkedHashMap<String, Object>();
        template.put("name", message.templateName());
        template.put("language", Map.of("code", message.languageCode()));
        var components = new ArrayList<Map<String, Object>>();
        components.add(Map.of(
                "type", "body",
                "parameters", message.bodyParameters().stream()
                        .map(value -> Map.of("type", "text", "text", value))
                        .toList()
        ));
        if (message.confirmationButtons()) {
            components.add(quickReply(0, "CL_CONFIRM:" + message.messageId()));
            components.add(quickReply(1, "CL_RESCHEDULE:" + message.messageId()));
        }
        template.put("components", components);
        var body = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", message.recipient(),
                "type", "template",
                "template", template
        );
        try {
            var response = RestClient.create("https://graph.facebook.com")
                    .post()
                    .uri("/{version}/{phoneNumberId}/messages", properties.graphVersion(), properties.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.accessToken()))
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            var id = response == null ? "" : response.path("messages").path(0).path("id").asText("");
            if (id.isBlank()) {
                throw new IllegalStateException("A Meta não retornou o identificador da mensagem");
            }
            return id;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "A Meta recusou a mensagem (HTTP " + exception.getStatusCode().value() + ")",
                    exception
            );
        }
    }

    private Map<String, Object> quickReply(int index, String payload) {
        return Map.of(
                "type", "button",
                "sub_type", "quick_reply",
                "index", Integer.toString(index),
                "parameters", List.of(Map.of("type", "payload", "payload", payload))
        );
    }

}
