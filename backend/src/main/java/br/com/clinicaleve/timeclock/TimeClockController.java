package br.com.clinicaleve.timeclock;

import br.com.clinicaleve.timeclock.TimeClockDtos.DaySummaryResponse;
import br.com.clinicaleve.timeclock.TimeClockDtos.EntryResponse;
import br.com.clinicaleve.timeclock.TimeClockDtos.EmployeeReportResponse;
import br.com.clinicaleve.timeclock.TimeClockDtos.ReportEmployeeResponse;
import br.com.clinicaleve.timeclock.TimeClockDtos.ManualEntryRequest;
import br.com.clinicaleve.timeclock.TimeClockDtos.PunchRequest;
import br.com.clinicaleve.timeclock.TimeClockDtos.UpdateEntryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/time-clock")
@RequiredArgsConstructor
public class TimeClockController {

    private final TimeClockService service;

    @GetMapping("/me")
    DaySummaryResponse myDay(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return service.myDay(date);
    }

    @PostMapping("/me/punch")
    DaySummaryResponse punch(@Valid @RequestBody PunchRequest request) {
        return service.punch(request.type());
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HR')")
    List<DaySummaryResponse> teamDay(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return service.teamDay(date);
    }

    @GetMapping("/reports/employee")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HR')")
    EmployeeReportResponse employeeReport(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.employeeReport(userId, from, to);
    }

    @GetMapping("/reports/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HR')")
    List<ReportEmployeeResponse> reportEmployees() {
        return service.reportEmployees();
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HR')")
    EntryResponse create(@Valid @RequestBody ManualEntryRequest request) {
        return service.createManual(request);
    }

    @PutMapping("/entries/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HR')")
    EntryResponse update(
            @PathVariable String id,
            @Valid @RequestBody UpdateEntryRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HR')")
    void delete(@PathVariable String id) {
        service.delete(id);
    }
}
