package br.com.clinicaleve.finance;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class FinanceDtos {
    private FinanceDtos() {}

    public record CategoryRequest(@NotBlank @Size(max = 120) String name, @NotNull FinancialEntryType type) {}
    public record CategoryResponse(String id, String name, FinancialEntryType type) {
        static CategoryResponse from(FinancialCategory c) { return new CategoryResponse(c.getId(), c.getName(), c.getType()); }
    }
    public record EntryRequest(
            @NotBlank @Size(max = 180) String description,
            @NotNull FinancialEntryType type,
            @NotBlank String categoryId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull LocalDate dueDate,
            @Size(max = 160) String counterparty,
            @Size(max = 500) String notes
    ) {}
    public record SettleRequest(LocalDate paymentDate, @NotBlank @Size(max = 40) String paymentMethod) {}
    public record EntryResponse(
            String id, String description, FinancialEntryType type,
            String categoryId, String categoryName, BigDecimal amount,
            LocalDate dueDate, LocalDate paymentDate, FinancialEntryStatus status,
            String counterparty, String paymentMethod, String notes
    ) {
        static EntryResponse from(FinancialEntry e, String categoryName, FinancialEntryStatus status) {
            return new EntryResponse(e.getId(), e.getDescription(), e.getType(), e.getCategoryId(),
                    categoryName, e.getAmount(), e.getDueDate(), e.getPaymentDate(), status,
                    e.getCounterparty(), e.getPaymentMethod(), e.getNotes());
        }
    }
}
