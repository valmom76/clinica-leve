package br.com.clinicaleve.billing;

import br.com.clinicaleve.billing.BillingDtos.BillingOverviewResponse;
import br.com.clinicaleve.billing.BillingDtos.BillingProfileRequest;
import br.com.clinicaleve.billing.BillingDtos.BillingProfileResponse;
import br.com.clinicaleve.billing.BillingDtos.StartSubscriptionRequest;
import br.com.clinicaleve.billing.BillingDtos.StartSubscriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BillingController {
    private final BillingService service;

    @GetMapping("/overview")
    BillingOverviewResponse overview() {
        return service.overview();
    }

    @PutMapping("/profile")
    BillingProfileResponse saveProfile(@Valid @RequestBody BillingProfileRequest request) {
        return service.saveProfile(request);
    }

    @PostMapping("/subscription/start")
    StartSubscriptionResponse start(@Valid @RequestBody StartSubscriptionRequest request) {
        return service.start(request);
    }

    @PostMapping("/subscription/refresh")
    BillingOverviewResponse refresh() {
        return service.refresh();
    }

    @PostMapping("/subscription/cancel")
    BillingOverviewResponse cancel() {
        return service.cancel();
    }
}
