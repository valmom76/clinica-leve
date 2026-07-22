package br.com.clinicaleve.billing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AsaasProperties.class)
public class AsaasConfig {

    @Bean
    RestClient asaasRestClient(AsaasProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("accept", "application/json")
                .build();
    }
}
