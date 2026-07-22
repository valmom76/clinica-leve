package br.com.clinicaleve.appointment;

import br.com.clinicaleve.appointment.AppointmentDtos.AppointmentRequest;
import br.com.clinicaleve.appointment.AppointmentDtos.AppointmentReportResponse;
import br.com.clinicaleve.appointment.AppointmentDtos.AppointmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'PROFESSIONAL')")
public class AppointmentController {

    private final AppointmentService service;

    @GetMapping
    List<AppointmentResponse> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        return service.list(from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AppointmentResponse create(@Valid @RequestBody AppointmentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    AppointmentResponse update(
            @PathVariable String id,
            @Valid @RequestBody AppointmentRequest request
    ) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    AppointmentResponse cancel(@PathVariable String id) {
        return service.cancel(id);
    }

    @GetMapping("/reports/context")
    AppointmentReportResponse report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String professionalId,
            @RequestParam(required = false) String specialtyId,
            @RequestParam(required = false) AppointmentStatus status
    ) {
        return service.report(from, to, professionalId, specialtyId, status);
    }
}
