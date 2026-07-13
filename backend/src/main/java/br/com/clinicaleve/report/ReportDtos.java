package br.com.clinicaleve.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record PeriodResponse(
            LocalDate from,
            LocalDate to,
            int days,
            String granularity
    ) {
    }

    public record AppointmentSummary(
            int total,
            int completed,
            int cancelled,
            int noShows,
            BigDecimal attendanceRate
    ) {
    }

    public record FinanceSummary(
            BigDecimal received,
            BigDecimal paid,
            BigDecimal net,
            BigDecimal receivable,
            BigDecimal payable
    ) {
    }

    public record InventorySummary(
            LocalDate snapshotDate,
            int activeMaterials,
            int lowStock,
            int expiredBatches,
            int expiringIn30Days
    ) {
    }

    public record TimeSummary(
            int employeesWithRecords,
            int daysWithRecords,
            int workedMinutes,
            int expectedMinutes,
            int balanceMinutes
    ) {
    }

    public record TrendPoint(
            String key,
            String label,
            LocalDate periodStart,
            int appointments,
            int completed,
            BigDecimal received,
            BigDecimal paid
    ) {
    }

    public record SpecialtyPerformance(
            String specialtyName,
            String color,
            int total,
            int completed,
            int noShows,
            BigDecimal attendanceRate
    ) {
    }

    public record EmployeeHours(
            String userId,
            String userName,
            int daysWithRecords,
            int workedMinutes,
            int expectedMinutes,
            int balanceMinutes
    ) {
    }

    public record ManagementReportResponse(
            PeriodResponse period,
            AppointmentSummary appointments,
            FinanceSummary finance,
            InventorySummary inventory,
            TimeSummary time,
            List<TrendPoint> trend,
            List<SpecialtyPerformance> specialties,
            List<EmployeeHours> employeeHours
    ) {
    }
}
