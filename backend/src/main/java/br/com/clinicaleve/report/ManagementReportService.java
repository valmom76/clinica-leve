package br.com.clinicaleve.report;

import br.com.clinicaleve.appointment.Appointment;
import br.com.clinicaleve.appointment.AppointmentRepository;
import br.com.clinicaleve.appointment.AppointmentStatus;
import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.auth.AppUserRepository;
import br.com.clinicaleve.finance.FinancialEntry;
import br.com.clinicaleve.finance.FinancialEntryRepository;
import br.com.clinicaleve.finance.FinancialEntryStatus;
import br.com.clinicaleve.finance.FinancialEntryType;
import br.com.clinicaleve.inventory.MaterialBatchRepository;
import br.com.clinicaleve.inventory.MaterialRepository;
import br.com.clinicaleve.report.ReportDtos.AppointmentSummary;
import br.com.clinicaleve.report.ReportDtos.EmployeeHours;
import br.com.clinicaleve.report.ReportDtos.FinanceSummary;
import br.com.clinicaleve.report.ReportDtos.InventorySummary;
import br.com.clinicaleve.report.ReportDtos.ManagementReportResponse;
import br.com.clinicaleve.report.ReportDtos.PeriodResponse;
import br.com.clinicaleve.report.ReportDtos.SpecialtyPerformance;
import br.com.clinicaleve.report.ReportDtos.TimeSummary;
import br.com.clinicaleve.report.ReportDtos.TrendPoint;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.specialty.Specialty;
import br.com.clinicaleve.specialty.SpecialtyRepository;
import br.com.clinicaleve.tenant.ClinicRepository;
import br.com.clinicaleve.timeclock.TimeClockEntry;
import br.com.clinicaleve.timeclock.TimeClockEntryRepository;
import br.com.clinicaleve.timeclock.TimeEntryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagementReportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final AppointmentRepository appointmentRepository;
    private final FinancialEntryRepository financialEntryRepository;
    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository materialBatchRepository;
    private final TimeClockEntryRepository timeClockEntryRepository;
    private final SpecialtyRepository specialtyRepository;
    private final AppUserRepository userRepository;
    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public ManagementReportResponse management(LocalDate requestedFrom, LocalDate requestedTo) {
        var clinicId = TenantAccess.currentClinicId();
        var zone = clinicZone(clinicId);
        var today = LocalDate.now(zone);
        var to = requestedTo == null ? today : requestedTo;
        var from = requestedFrom == null ? to.withDayOfMonth(1) : requestedFrom;
        validatePeriod(from, to);

        var range = range(from, to, zone);
        var appointments = appointmentRepository
                .findByClinicIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
                        clinicId, range.from(), range.to()
                );
        var finances = financialEntryRepository.findForReport(clinicId, from, to);
        var timeEntries = timeClockEntryRepository.findDayForClinic(clinicId, range.from(), range.to());
        var users = userRepository.findByClinicIdOrderByActiveDescNameAsc(clinicId);
        var specialties = specialtyRepository.findByClinicIdAndActiveTrueOrderByName(clinicId);

        var bucketMode = ChronoUnit.DAYS.between(from, to) < 45 ? BucketMode.DAILY : BucketMode.MONTHLY;
        var buckets = buckets(from, to, bucketMode);
        fillAppointmentBuckets(buckets, appointments, zone, bucketMode);
        fillFinanceBuckets(buckets, finances, from, to, bucketMode);

        var employeeHours = employeeHours(users, timeEntries, zone, today);
        return new ManagementReportResponse(
                new PeriodResponse(
                        from,
                        to,
                        Math.toIntExact(ChronoUnit.DAYS.between(from, to) + 1),
                        bucketMode.name()
                ),
                appointmentSummary(appointments),
                financeSummary(finances, from, to),
                inventorySummary(clinicId, today),
                timeSummary(employeeHours),
                buckets.values().stream().map(Bucket::response).toList(),
                specialtyPerformance(appointments, specialties),
                employeeHours
        );
    }

    private AppointmentSummary appointmentSummary(List<Appointment> appointments) {
        var completed = countStatus(appointments, AppointmentStatus.COMPLETED);
        var cancelled = countStatus(appointments, AppointmentStatus.CANCELLED);
        var noShows = countStatus(appointments, AppointmentStatus.NO_SHOW);
        return new AppointmentSummary(
                appointments.size(),
                completed,
                cancelled,
                noShows,
                percentage(completed, completed + noShows)
        );
    }

    private FinanceSummary financeSummary(
            List<FinancialEntry> entries,
            LocalDate from,
            LocalDate to
    ) {
        var received = paidAmount(entries, FinancialEntryType.INCOME, from, to);
        var paid = paidAmount(entries, FinancialEntryType.EXPENSE, from, to);
        var receivable = openAmount(entries, FinancialEntryType.INCOME, from, to);
        var payable = openAmount(entries, FinancialEntryType.EXPENSE, from, to);
        return new FinanceSummary(received, paid, received.subtract(paid), receivable, payable);
    }

    private InventorySummary inventorySummary(String clinicId, LocalDate today) {
        var materials = materialRepository.findByClinicIdAndActiveTrueOrderByName(clinicId);
        var lowStock = (int) materials.stream()
                .filter(material -> material.getMinimumStock().signum() > 0
                        && material.getCurrentStock().compareTo(material.getMinimumStock()) <= 0)
                .count();
        var batches = materialBatchRepository.findByClinicIdAndCurrentQuantityGreaterThan(clinicId, BigDecimal.ZERO);
        var expired = (int) batches.stream()
                .filter(batch -> batch.getExpirationDate() != null && batch.getExpirationDate().isBefore(today))
                .count();
        var expiring = (int) batches.stream()
                .filter(batch -> batch.getExpirationDate() != null)
                .filter(batch -> !batch.getExpirationDate().isBefore(today))
                .filter(batch -> !batch.getExpirationDate().isAfter(today.plusDays(30)))
                .count();
        return new InventorySummary(today, materials.size(), lowStock, expired, expiring);
    }

    private List<SpecialtyPerformance> specialtyPerformance(
            List<Appointment> appointments,
            List<Specialty> specialties
    ) {
        Map<String, Specialty> byId = specialties.stream()
                .collect(Collectors.toMap(Specialty::getId, Function.identity()));
        return appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getSpecialtyId))
                .entrySet()
                .stream()
                .map(entry -> {
                    var specialty = byId.get(entry.getKey());
                    var items = entry.getValue();
                    var completed = countStatus(items, AppointmentStatus.COMPLETED);
                    var noShows = countStatus(items, AppointmentStatus.NO_SHOW);
                    return new SpecialtyPerformance(
                            specialty == null ? "Outras especialidades" : specialty.getName(),
                            specialty == null ? "#7f918c" : specialty.getColor(),
                            items.size(),
                            completed,
                            noShows,
                            percentage(completed, completed + noShows)
                    );
                })
                .sorted(Comparator.comparingInt(SpecialtyPerformance::total).reversed()
                        .thenComparing(SpecialtyPerformance::specialtyName))
                .limit(8)
                .toList();
    }

    private List<EmployeeHours> employeeHours(
            List<AppUser> users,
            List<TimeClockEntry> entries,
            ZoneId zone,
            LocalDate today
    ) {
        Map<String, AppUser> usersById = users.stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
        Map<String, Map<LocalDate, List<TimeClockEntry>>> grouped = entries.stream()
                .filter(entry -> usersById.containsKey(entry.getUserId()))
                .collect(Collectors.groupingBy(
                        TimeClockEntry::getUserId,
                        Collectors.groupingBy(entry -> LocalDate.ofInstant(entry.getOccurredAt(), zone))
                ));

        return users.stream()
                .map(user -> {
                    var days = grouped.getOrDefault(user.getId(), Map.of());
                    var worked = days.entrySet().stream()
                            .mapToInt(day -> workedMinutes(day.getValue(), day.getKey(), today))
                            .sum();
                    var expected = Math.multiplyExact(user.getExpectedDailyMinutes(), days.size());
                    return new EmployeeHours(
                            user.getId(), user.getName(), days.size(), worked, expected, worked - expected
                    );
                })
                .filter(item -> item.daysWithRecords() > 0)
                .sorted(Comparator.comparingInt(EmployeeHours::workedMinutes).reversed()
                        .thenComparing(EmployeeHours::userName))
                .toList();
    }

    private TimeSummary timeSummary(List<EmployeeHours> employeeHours) {
        var days = employeeHours.stream().mapToInt(EmployeeHours::daysWithRecords).sum();
        var worked = employeeHours.stream().mapToInt(EmployeeHours::workedMinutes).sum();
        var expected = employeeHours.stream().mapToInt(EmployeeHours::expectedMinutes).sum();
        return new TimeSummary(employeeHours.size(), days, worked, expected, worked - expected);
    }

    private int workedMinutes(List<TimeClockEntry> unordered, LocalDate date, LocalDate today) {
        var entries = unordered.stream().sorted(Comparator.comparing(TimeClockEntry::getOccurredAt)).toList();
        Instant activeSince = null;
        long seconds = 0;
        for (var entry : entries) {
            if (entry.getType() == TimeEntryType.CLOCK_IN || entry.getType() == TimeEntryType.BREAK_END) {
                activeSince = entry.getOccurredAt();
            } else if (activeSince != null) {
                seconds += Math.max(0, Duration.between(activeSince, entry.getOccurredAt()).getSeconds());
                activeSince = null;
            }
        }
        if (activeSince != null && date.equals(today)) {
            seconds += Math.max(0, Duration.between(activeSince, Instant.now()).getSeconds());
        }
        return Math.toIntExact(seconds / 60);
    }

    private void fillAppointmentBuckets(
            Map<String, Bucket> buckets,
            List<Appointment> appointments,
            ZoneId zone,
            BucketMode mode
    ) {
        appointments.forEach(appointment -> {
            var date = LocalDate.ofInstant(appointment.getStartAt(), zone);
            var bucket = buckets.get(bucketKey(date, mode));
            if (bucket != null) {
                bucket.appointments++;
                if (appointment.getStatus() == AppointmentStatus.COMPLETED) bucket.completed++;
            }
        });
    }

    private void fillFinanceBuckets(
            Map<String, Bucket> buckets,
            List<FinancialEntry> entries,
            LocalDate from,
            LocalDate to,
            BucketMode mode
    ) {
        entries.stream()
                .filter(entry -> entry.getStatus() == FinancialEntryStatus.PAID)
                .filter(entry -> entry.getPaymentDate() != null && between(entry.getPaymentDate(), from, to))
                .forEach(entry -> {
                    var bucket = buckets.get(bucketKey(entry.getPaymentDate(), mode));
                    if (bucket == null) return;
                    if (entry.getType() == FinancialEntryType.INCOME) {
                        bucket.received = bucket.received.add(entry.getAmount());
                    } else {
                        bucket.paid = bucket.paid.add(entry.getAmount());
                    }
                });
    }

    private Map<String, Bucket> buckets(LocalDate from, LocalDate to, BucketMode mode) {
        Map<String, Bucket> result = new LinkedHashMap<>();
        if (mode == BucketMode.DAILY) {
            for (var date = from; !date.isAfter(to); date = date.plusDays(1)) {
                var label = date.format(DateTimeFormatter.ofPattern("dd/MM"));
                result.put(bucketKey(date, mode), new Bucket(bucketKey(date, mode), label, date));
            }
            return result;
        }

        for (var month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            var monthName = month.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
            var label = monthName.substring(0, 1).toUpperCase(PT_BR) + monthName.substring(1) + "/" + String.valueOf(month.getYear()).substring(2);
            result.put(month.toString(), new Bucket(month.toString(), label, month.atDay(1)));
        }
        return result;
    }

    private BigDecimal paidAmount(
            List<FinancialEntry> entries,
            FinancialEntryType type,
            LocalDate from,
            LocalDate to
    ) {
        return entries.stream()
                .filter(entry -> entry.getType() == type && entry.getStatus() == FinancialEntryStatus.PAID)
                .filter(entry -> entry.getPaymentDate() != null && between(entry.getPaymentDate(), from, to))
                .map(FinancialEntry::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal openAmount(
            List<FinancialEntry> entries,
            FinancialEntryType type,
            LocalDate from,
            LocalDate to
    ) {
        return entries.stream()
                .filter(entry -> entry.getType() == type
                        && (entry.getStatus() == FinancialEntryStatus.OPEN
                        || entry.getStatus() == FinancialEntryStatus.OVERDUE))
                .filter(entry -> between(entry.getDueDate(), from, to))
                .map(FinancialEntry::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private int countStatus(List<Appointment> appointments, AppointmentStatus status) {
        return (int) appointments.stream().filter(item -> item.getStatus() == status).count();
    }

    private BigDecimal percentage(int value, int total) {
        if (total == 0) return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private String bucketKey(LocalDate date, BucketMode mode) {
        return mode == BucketMode.DAILY ? date.toString() : YearMonth.from(date).toString();
    }

    private boolean between(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("A data inicial deve ser anterior à data final");
        if (ChronoUnit.DAYS.between(from, to) > 365) {
            throw new IllegalArgumentException("O relatório pode abranger no máximo 366 dias");
        }
    }

    private ZoneId clinicZone(String clinicId) {
        var timezone = clinicRepository.findById(clinicId)
                .map(clinic -> clinic.getTimezone())
                .orElse("America/Fortaleza");
        return ZoneId.of(timezone);
    }

    private DateRange range(LocalDate from, LocalDate to, ZoneId zone) {
        return new DateRange(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private enum BucketMode { DAILY, MONTHLY }

    private record DateRange(Instant from, Instant to) {
    }

    private static final class Bucket {
        private final String key;
        private final String label;
        private final LocalDate periodStart;
        private int appointments;
        private int completed;
        private BigDecimal received = ZERO;
        private BigDecimal paid = ZERO;

        private Bucket(String key, String label, LocalDate periodStart) {
            this.key = key;
            this.label = label;
            this.periodStart = periodStart;
        }

        private TrendPoint response() {
            return new TrendPoint(key, label, periodStart, appointments, completed, received, paid);
        }
    }
}
