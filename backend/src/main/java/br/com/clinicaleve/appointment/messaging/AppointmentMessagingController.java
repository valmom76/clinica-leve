package br.com.clinicaleve.appointment.messaging;

import br.com.clinicaleve.appointment.messaging.MessagingDtos.MessageResponse;
import br.com.clinicaleve.appointment.messaging.MessagingDtos.SettingsRequest;
import br.com.clinicaleve.appointment.messaging.MessagingDtos.SettingsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'PROFESSIONAL')")
public class AppointmentMessagingController {

    private final AppointmentMessagingService service;

    @GetMapping("/messaging/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    SettingsResponse settings() {
        return service.settings();
    }

    @PutMapping("/messaging/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    SettingsResponse saveSettings(@Valid @RequestBody SettingsRequest request) {
        return service.saveSettings(request);
    }

    @GetMapping("/{appointmentId}/messages")
    List<MessageResponse> history(@PathVariable String appointmentId) {
        return service.history(appointmentId);
    }

    @PostMapping("/{appointmentId}/messages/confirmation")
    MessageResponse sendConfirmation(@PathVariable String appointmentId) {
        return service.sendConfirmationNow(appointmentId);
    }
}
