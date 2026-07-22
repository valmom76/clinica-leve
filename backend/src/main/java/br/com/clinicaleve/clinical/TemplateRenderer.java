package br.com.clinicaleve.clinical;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateRenderer {

    public String render(String template, Map<String, String> values) {
        var result = template;
        for (var entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", safe(entry.getValue()));
        }
        return result;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
