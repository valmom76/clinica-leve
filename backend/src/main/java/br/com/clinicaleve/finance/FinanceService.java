package br.com.clinicaleve.finance;

import br.com.clinicaleve.finance.FinanceDtos.*;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.tenant.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {
    private final FinancialEntryRepository entryRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public List<EntryResponse> list(LocalDate from, LocalDate to) {
        var clinicId = TenantAccess.currentClinicId();
        var today = today(clinicId);
        Map<String, String> categories = categoryRepository.findByClinicIdAndActiveTrueOrderByTypeAscNameAsc(clinicId)
                .stream().collect(Collectors.toMap(FinancialCategory::getId, FinancialCategory::getName));
        return entryRepository.findByClinicIdOrderByDueDateDescCreatedAtDesc(clinicId).stream()
                .filter(e -> from == null || !e.getDueDate().isBefore(from))
                .filter(e -> to == null || !e.getDueDate().isAfter(to))
                .map(e -> response(e, categories.getOrDefault(e.getCategoryId(), "Sem categoria"), today)).toList();
    }

    @Transactional(readOnly = true)
    public FinanceReportResponse report(
            LocalDate from,
            LocalDate to,
            FinancialEntryType type,
            String categoryId,
            FinancialEntryStatus status
    ) {
        validateReportPeriod(from, to);
        var clinicId = TenantAccess.currentClinicId();
        var today = today(clinicId);
        Map<String, String> categories = categoryRepository
                .findByClinicIdAndActiveTrueOrderByTypeAscNameAsc(clinicId)
                .stream()
                .collect(Collectors.toMap(FinancialCategory::getId, FinancialCategory::getName));

        var entries = entryRepository.findForReport(clinicId, from, to).stream()
                .filter(entry -> type == null || entry.getType() == type)
                .filter(entry -> categoryId == null || categoryId.equals(entry.getCategoryId()))
                .filter(entry -> status == null || effectiveStatus(entry, today) == status)
                .sorted(Comparator.comparing(FinancialEntry::getDueDate).reversed())
                .toList();

        var received = totalPaid(entries, FinancialEntryType.INCOME, from, to, today);
        var paid = totalPaid(entries, FinancialEntryType.EXPENSE, from, to, today);
        var receivable = totalOpen(entries, FinancialEntryType.INCOME, from, to, today);
        var payable = totalOpen(entries, FinancialEntryType.EXPENSE, from, to, today);
        var responses = entries.stream()
                .map(entry -> response(
                        entry,
                        categories.getOrDefault(entry.getCategoryId(), "Sem categoria"),
                        today
                ))
                .toList();

        return new FinanceReportResponse(
                from,
                to,
                responses.size(),
                received,
                paid,
                received.subtract(paid),
                receivable,
                payable,
                responses
        );
    }

    @Transactional
    public EntryResponse create(EntryRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var category = category(request.categoryId(), request.type(), clinicId);
        var entry = new FinancialEntry();
        entry.setClinicId(clinicId); entry.setCreatedByUserId(TenantAccess.currentUserId());
        apply(entry, request, category);
        return response(entryRepository.save(entry), category.getName(), today(clinicId));
    }

    @Transactional
    public EntryResponse update(String id, EntryRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var entry = locked(id, clinicId);
        if (entry.getStatus() != FinancialEntryStatus.OPEN) throw new IllegalStateException("Reabra o lançamento antes de editá-lo");
        var category = category(request.categoryId(), request.type(), clinicId);
        apply(entry, request, category);
        return response(entryRepository.save(entry), category.getName(), today(clinicId));
    }

    @Transactional
    public EntryResponse settle(String id, SettleRequest request) {
        var clinicId = TenantAccess.currentClinicId(); var entry = locked(id, clinicId);
        if (entry.getStatus() == FinancialEntryStatus.CANCELLED) throw new IllegalStateException("Um lançamento cancelado não pode ser baixado");
        entry.setStatus(FinancialEntryStatus.PAID);
        entry.setPaymentDate(request.paymentDate() == null ? today(clinicId) : request.paymentDate());
        entry.setPaymentMethod(request.paymentMethod().trim());
        return responseFor(entryRepository.save(entry), clinicId);
    }

    @Transactional
    public EntryResponse reopen(String id) {
        var clinicId = TenantAccess.currentClinicId(); var entry = locked(id, clinicId);
        entry.setStatus(FinancialEntryStatus.OPEN); entry.setPaymentDate(null); entry.setPaymentMethod(null);
        return responseFor(entryRepository.save(entry), clinicId);
    }

    @Transactional
    public EntryResponse cancel(String id) {
        var clinicId = TenantAccess.currentClinicId(); var entry = locked(id, clinicId);
        if (entry.getStatus() == FinancialEntryStatus.PAID) throw new IllegalStateException("Reabra o lançamento antes de cancelá-lo");
        entry.setStatus(FinancialEntryStatus.CANCELLED);
        return responseFor(entryRepository.save(entry), clinicId);
    }

    private FinancialEntry locked(String id, String clinicId) {
        return entryRepository.findForUpdate(id, clinicId).orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado"));
    }
    private FinancialCategory category(String id, FinancialEntryType type, String clinicId) {
        var c = categoryRepository.findByIdAndClinicIdAndActiveTrue(id, clinicId).orElseThrow(() -> new IllegalArgumentException("Categoria financeira não encontrada"));
        if (c.getType() != type) throw new IllegalArgumentException("A categoria não corresponde ao tipo do lançamento"); return c;
    }
    private void apply(FinancialEntry e, EntryRequest r, FinancialCategory c) {
        e.setDescription(r.description().trim()); e.setType(r.type()); e.setCategoryId(c.getId());
        e.setAmount(r.amount()); e.setDueDate(r.dueDate()); e.setCounterparty(clean(r.counterparty())); e.setNotes(clean(r.notes()));
    }
    private EntryResponse responseFor(FinancialEntry e, String clinicId) {
        var name = categoryRepository.findByIdAndClinicIdAndActiveTrue(e.getCategoryId(), clinicId).map(FinancialCategory::getName).orElse("Sem categoria");
        return response(e, name, today(clinicId));
    }
    private EntryResponse response(FinancialEntry e, String category, LocalDate today) {
        return EntryResponse.from(e, category, effectiveStatus(e, today));
    }
    private FinancialEntryStatus effectiveStatus(FinancialEntry entry, LocalDate today) {
        return entry.getStatus() == FinancialEntryStatus.OPEN && entry.getDueDate().isBefore(today)
                ? FinancialEntryStatus.OVERDUE
                : entry.getStatus();
    }
    private BigDecimal totalPaid(
            List<FinancialEntry> entries,
            FinancialEntryType type,
            LocalDate from,
            LocalDate to,
            LocalDate today
    ) {
        return entries.stream()
                .filter(entry -> entry.getType() == type)
                .filter(entry -> effectiveStatus(entry, today) == FinancialEntryStatus.PAID)
                .filter(entry -> {
                    var date = entry.getPaymentDate();
                    return date != null && !date.isBefore(from) && !date.isAfter(to);
                })
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private BigDecimal totalOpen(
            List<FinancialEntry> entries,
            FinancialEntryType type,
            LocalDate from,
            LocalDate to,
            LocalDate today
    ) {
        return entries.stream()
                .filter(entry -> entry.getType() == type)
                .filter(entry -> {
                    var status = effectiveStatus(entry, today);
                    return status == FinancialEntryStatus.OPEN || status == FinancialEntryStatus.OVERDUE;
                })
                .filter(entry -> !entry.getDueDate().isBefore(from) && !entry.getDueDate().isAfter(to))
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private void validateReportPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("Informe o período do relatório");
        if (to.isBefore(from)) throw new IllegalArgumentException("Período do relatório inválido");
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > 365) {
            throw new IllegalArgumentException("O relatório permite no máximo 366 dias");
        }
    }
    private LocalDate today(String clinicId) {
        var timezone = clinicRepository.findById(clinicId).map(c -> c.getTimezone()).orElse("America/Fortaleza");
        return LocalDate.now(ZoneId.of(timezone));
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
