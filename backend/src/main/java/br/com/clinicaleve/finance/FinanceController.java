package br.com.clinicaleve.finance;

import br.com.clinicaleve.finance.FinanceDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
public class FinanceController {
    private final FinancialCategoryService categoryService;
    private final FinanceService financeService;

    @GetMapping("/categories")
    List<CategoryResponse> categories() { return categoryService.list(); }
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) { return categoryService.create(request); }
    @GetMapping("/entries")
    List<EntryResponse> entries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return financeService.list(from, to);
    }
    @PostMapping("/entries") @ResponseStatus(HttpStatus.CREATED)
    EntryResponse create(@Valid @RequestBody EntryRequest request) { return financeService.create(request); }
    @PutMapping("/entries/{id}")
    EntryResponse update(@PathVariable String id, @Valid @RequestBody EntryRequest request) { return financeService.update(id, request); }
    @PostMapping("/entries/{id}/settle")
    EntryResponse settle(@PathVariable String id, @Valid @RequestBody SettleRequest request) { return financeService.settle(id, request); }
    @PostMapping("/entries/{id}/reopen")
    EntryResponse reopen(@PathVariable String id) { return financeService.reopen(id); }
    @PostMapping("/entries/{id}/cancel")
    EntryResponse cancel(@PathVariable String id) { return financeService.cancel(id); }
}
