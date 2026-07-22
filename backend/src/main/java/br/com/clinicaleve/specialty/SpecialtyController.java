package br.com.clinicaleve.specialty;

import br.com.clinicaleve.shared.TenantAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
public class SpecialtyController {

    private final SpecialtyRepository repository;

    @GetMapping
    List<SpecialtyResponse> list() {
        return repository.findByClinicIdAndActiveTrueOrderByName(TenantAccess.currentClinicId())
                .stream()
                .map(SpecialtyResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    SpecialtyResponse create(@Valid @RequestBody SpecialtyRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        if (repository.existsByClinicIdAndNameIgnoreCase(clinicId, request.name().trim())) {
            throw new IllegalArgumentException("Já existe uma especialidade com este nome");
        }
        var entity = new Specialty();
        entity.setClinicId(clinicId);
        entity.setName(request.name().trim());
        entity.setColor(request.color());
        return SpecialtyResponse.from(repository.save(entity));
    }

    public record SpecialtyRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color
    ) {
    }

    public record SpecialtyResponse(String id, String name, String color, boolean active) {
        static SpecialtyResponse from(Specialty entity) {
            return new SpecialtyResponse(
                    entity.getId(),
                    entity.getName(),
                    entity.getColor(),
                    entity.isActive()
            );
        }
    }
}
