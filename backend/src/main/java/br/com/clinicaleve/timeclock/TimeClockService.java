package br.com.clinicaleve.timeclock;

import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.auth.AppUserRepository;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.tenant.ClinicRepository;
import br.com.clinicaleve.timeclock.TimeClockDtos.DaySummaryResponse;
import br.com.clinicaleve.timeclock.TimeClockDtos.EntryResponse;
import br.com.clinicaleve.timeclock.TimeClockDtos.EmployeeReportResponse;
import br.com.clinicaleve.timeclock.TimeClockDtos.ReportEmployeeResponse;
import br.com.clinicaleve.timeclock.TimeClockDtos.ManualEntryRequest;
import br.com.clinicaleve.timeclock.TimeClockDtos.UpdateEntryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TimeClockService {

    private final TimeClockEntryRepository entryRepository;
    private final AppUserRepository userRepository;
    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public DaySummaryResponse myDay(LocalDate requestedDate) {
        var clinicId = TenantAccess.currentClinicId();
        var zone = clinicZone(clinicId);
        var date = requestedDate == null ? LocalDate.now(zone) : requestedDate;
        var user = activeUser(TenantAccess.currentUserId(), clinicId);
        return summary(user, date, dayEntries(clinicId, user.getId(), date, zone), zone);
    }

    @Transactional
    public DaySummaryResponse punch(TimeEntryType type) {
        var clinicId = TenantAccess.currentClinicId();
        var userId = TenantAccess.currentUserId();
        var zone = clinicZone(clinicId);
        var user = userRepository.findForUpdate(userId, clinicId)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado ou inativo"));
        var now = Instant.now();
        var date = LocalDate.ofInstant(now, zone);
        var entries = dayEntries(clinicId, userId, date, zone);

        validateNext(entries, type);

        var entry = new TimeClockEntry();
        entry.setClinicId(clinicId);
        entry.setUserId(userId);
        entry.setType(type);
        entry.setOccurredAt(now);
        entry.setSource(TimeEntrySource.SELF_SERVICE);
        entry.setCreatedByUserId(userId);
        entryRepository.save(entry);

        entries = new ArrayList<>(entries);
        entries.add(entry);
        entries.sort(entryComparator());
        return summary(user, date, entries, zone);
    }

    @Transactional(readOnly = true)
    public List<DaySummaryResponse> teamDay(LocalDate requestedDate) {
        var clinicId = TenantAccess.currentClinicId();
        var zone = clinicZone(clinicId);
        var date = requestedDate == null ? LocalDate.now(zone) : requestedDate;
        var range = range(date, zone);
        Map<String, List<TimeClockEntry>> byUser = entryRepository
                .findDayForClinic(clinicId, range.from(), range.to())
                .stream()
                .collect(Collectors.groupingBy(TimeClockEntry::getUserId));

        return userRepository.findByClinicIdAndActiveTrueOrderByNameAsc(clinicId)
                .stream()
                .map(user -> summary(user, date, byUser.getOrDefault(user.getId(), List.of()), zone))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeReportResponse employeeReport(String userId, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        var clinicId = TenantAccess.currentClinicId();
        var zone = clinicZone(clinicId);
        var user = userRepository.findByIdAndClinicId(userId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
        var period = new DayRange(
                from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant()
        );
        var grouped = entryRepository.findDayForUser(clinicId, userId, period.from(), period.to())
                .stream()
                .collect(Collectors.groupingBy(entry -> LocalDate.ofInstant(entry.getOccurredAt(), zone)));
        var days = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> summary(user, entry.getKey(), entry.getValue(), zone))
                .toList();
        var worked = days.stream().mapToInt(DaySummaryResponse::workedMinutes).sum();
        var expected = days.stream().mapToInt(DaySummaryResponse::expectedMinutes).sum();
        return new EmployeeReportResponse(
                user.getId(),
                user.getName(),
                from,
                to,
                days.size(),
                (int) days.stream().filter(day -> day.status() == TimeDayStatus.CLOSED).count(),
                worked,
                expected,
                worked - expected,
                days
        );
    }

    @Transactional(readOnly = true)
    public List<ReportEmployeeResponse> reportEmployees() {
        return userRepository.findByClinicIdOrderByActiveDescNameAsc(TenantAccess.currentClinicId())
                .stream()
                .map(user -> new ReportEmployeeResponse(user.getId(), user.getName(), user.isActive()))
                .toList();
    }

    @Transactional
    public EntryResponse createManual(ManualEntryRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var zone = clinicZone(clinicId);
        var user = activeUser(request.userId(), clinicId);
        var occurredAt = toInstant(request.occurredAt(), zone);
        ensureNotFuture(occurredAt);
        var date = request.occurredAt().toLocalDate();
        var entries = new ArrayList<>(dayEntries(clinicId, user.getId(), date, zone));

        var entry = new TimeClockEntry();
        entry.setClinicId(clinicId);
        entry.setUserId(user.getId());
        entry.setType(request.type());
        entry.setOccurredAt(occurredAt);
        entry.setSource(TimeEntrySource.MANUAL);
        entry.setNotes(clean(request.notes()));
        entry.setCreatedByUserId(TenantAccess.currentUserId());
        entries.add(entry);
        entries.sort(entryComparator());
        validateSequence(entries);

        return response(entryRepository.save(entry), zone);
    }

    @Transactional
    public EntryResponse update(String id, UpdateEntryRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var zone = clinicZone(clinicId);
        var entry = locked(id, clinicId);
        var originalDate = LocalDate.ofInstant(entry.getOccurredAt(), zone);
        var newDate = request.occurredAt().toLocalDate();
        if (!originalDate.equals(newDate)) {
            throw new IllegalArgumentException("A data da marcação não pode ser alterada; exclua e crie uma nova");
        }

        var occurredAt = toInstant(request.occurredAt(), zone);
        ensureNotFuture(occurredAt);
        var entries = new ArrayList<>(dayEntries(clinicId, entry.getUserId(), originalDate, zone));
        entries.removeIf(candidate -> candidate.getId().equals(id));

        entry.setType(request.type());
        entry.setOccurredAt(occurredAt);
        entry.setSource(TimeEntrySource.MANUAL);
        entry.setNotes(clean(request.notes()));
        entry.setUpdatedByUserId(TenantAccess.currentUserId());
        entries.add(entry);
        entries.sort(entryComparator());
        validateSequence(entries);
        return response(entryRepository.save(entry), zone);
    }

    @Transactional
    public void delete(String id) {
        var clinicId = TenantAccess.currentClinicId();
        var zone = clinicZone(clinicId);
        var entry = locked(id, clinicId);
        var date = LocalDate.ofInstant(entry.getOccurredAt(), zone);
        var entries = new ArrayList<>(dayEntries(clinicId, entry.getUserId(), date, zone));
        entries.removeIf(candidate -> candidate.getId().equals(id));
        validateSequence(entries);
        entryRepository.delete(entry);
    }

    private DaySummaryResponse summary(
            AppUser user,
            LocalDate date,
            List<TimeClockEntry> unorderedEntries,
            ZoneId zone
    ) {
        var entries = unorderedEntries.stream().sorted(entryComparator()).toList();
        var status = status(entries);
        var worked = workedMinutes(entries, date, zone, status);
        var expected = user.getExpectedDailyMinutes();
        return new DaySummaryResponse(
                user.getId(),
                user.getName(),
                date,
                status,
                worked,
                expected,
                worked - expected,
                entries.stream().map(entry -> response(entry, zone)).toList()
        );
    }

    private int workedMinutes(
            List<TimeClockEntry> entries,
            LocalDate date,
            ZoneId zone,
            TimeDayStatus status
    ) {
        Instant activeSince = null;
        long seconds = 0;
        for (var entry : entries) {
            switch (entry.getType()) {
                case CLOCK_IN, BREAK_END -> activeSince = entry.getOccurredAt();
                case BREAK_START, CLOCK_OUT -> {
                    if (activeSince != null) {
                        seconds += Duration.between(activeSince, entry.getOccurredAt()).getSeconds();
                        activeSince = null;
                    }
                }
            }
        }
        if (activeSince != null && status == TimeDayStatus.WORKING && date.equals(LocalDate.now(zone))) {
            seconds += Math.max(0, Duration.between(activeSince, Instant.now()).getSeconds());
        }
        return Math.toIntExact(Math.max(0, seconds / 60));
    }

    private void validateNext(List<TimeClockEntry> entries, TimeEntryType next) {
        if (entries.isEmpty()) {
            if (next != TimeEntryType.CLOCK_IN) {
                throw new IllegalStateException("A primeira marcação do dia deve ser a entrada");
            }
            return;
        }
        var previous = entries.stream().max(entryComparator()).orElseThrow().getType();
        var valid = switch (previous) {
            case CLOCK_IN, BREAK_END -> next == TimeEntryType.BREAK_START || next == TimeEntryType.CLOCK_OUT;
            case BREAK_START -> next == TimeEntryType.BREAK_END;
            case CLOCK_OUT -> false;
        };
        if (!valid) {
            throw new IllegalStateException("Esta marcação não é válida para o estado atual da jornada");
        }
    }

    private void validateSequence(List<TimeClockEntry> entries) {
        var accepted = new ArrayList<TimeClockEntry>();
        for (var entry : entries.stream().sorted(entryComparator()).toList()) {
            validateNext(accepted, entry.getType());
            accepted.add(entry);
        }
    }

    private TimeDayStatus status(List<TimeClockEntry> entries) {
        if (entries.isEmpty()) return TimeDayStatus.NOT_STARTED;
        return switch (entries.get(entries.size() - 1).getType()) {
            case CLOCK_IN, BREAK_END -> TimeDayStatus.WORKING;
            case BREAK_START -> TimeDayStatus.ON_BREAK;
            case CLOCK_OUT -> TimeDayStatus.CLOSED;
        };
    }

    private List<TimeClockEntry> dayEntries(
            String clinicId,
            String userId,
            LocalDate date,
            ZoneId zone
    ) {
        var range = range(date, zone);
        return entryRepository.findDayForUser(clinicId, userId, range.from(), range.to());
    }

    private AppUser activeUser(String userId, String clinicId) {
        return userRepository.findByIdAndClinicId(userId, clinicId)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado ou inativo"));
    }

    private TimeClockEntry locked(String id, String clinicId) {
        return entryRepository.findForUpdate(id, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Marcação não encontrada"));
    }

    private EntryResponse response(TimeClockEntry entry, ZoneId zone) {
        return new EntryResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getType(),
                LocalDateTime.ofInstant(entry.getOccurredAt(), zone),
                entry.getSource(),
                entry.getNotes(),
                entry.getUpdatedByUserId() != null
        );
    }

    private ZoneId clinicZone(String clinicId) {
        var timezone = clinicRepository.findById(clinicId)
                .map(clinic -> clinic.getTimezone())
                .orElse("America/Fortaleza");
        return ZoneId.of(timezone);
    }

    private Instant toInstant(LocalDateTime dateTime, ZoneId zone) {
        return dateTime.atZone(zone).toInstant();
    }

    private void ensureNotFuture(Instant instant) {
        if (instant.isAfter(Instant.now().plusSeconds(60))) {
            throw new IllegalArgumentException("Não é possível registrar uma marcação futura");
        }
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("Informe o período do relatório");
        if (from.isAfter(to)) throw new IllegalArgumentException("A data inicial deve ser anterior à data final");
        if (ChronoUnit.DAYS.between(from, to) > 365) {
            throw new IllegalArgumentException("O relatório pode abranger no máximo 366 dias");
        }
    }

    private DayRange range(LocalDate date, ZoneId zone) {
        return new DayRange(date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private Comparator<TimeClockEntry> entryComparator() {
        return Comparator.comparing(TimeClockEntry::getOccurredAt)
                .thenComparing(entry -> entry.getId() == null ? "" : entry.getId());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DayRange(Instant from, Instant to) {
    }
}
