package br.com.clinicaleve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClinicaLeveApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicaLeveApplication.class, args);
    }
}
