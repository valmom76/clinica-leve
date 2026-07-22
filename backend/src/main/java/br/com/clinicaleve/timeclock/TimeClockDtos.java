package br.com.clinicaleve.timeclock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TimeClockDtos {

    private TimeClockDtos() {
    }

    public record PunchRequest(@NotNull TimeEntryType type) {
    }

    public record ManualEntryRequest(
            @NotBlank String userId,
            @NotNull TimeEntryType type,
            @NotNull LocalDateTime occurredAt,
            @Size(max = 500) String notes
    ) {
    }

    public record UpdateEntryRequest(
            @NotNull TimeEntryType type,
            @NotNull LocalDateTime occurredAt,
            @Size(max = 500) String notes
    ) {
    }

    public record EntryResponse(
            String id,
            String userId,
            TimeEntryType type,
            LocalDateTime occurredAt,
            TimeEntrySource source,
            String notes,
            boolean edited
    ) {
    }

    public record DaySummaryResponse(
            String userId,
            String userName,
            LocalDate date,
            TimeDayStatus status,
            int workedMinutes,
            int expectedMinutes,
            int balanceMinutes,
            List<EntryResponse> entries
    ) {
    }

    public record EmployeeReportResponse(
            String userId,
            String userName,
            LocalDate from,
            LocalDate to,
            int daysWithRecords,
            int closedDays,
            int workedMinutes,
            int expectedMinutes,
            int balanceMinutes,
            List<DaySummaryResponse> days
    ) {
    }

    public record ReportEmployeeResponse(String userId, String userName, boolean active) {
    }
}
