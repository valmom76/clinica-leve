package br.com.clinicaleve.professional;

import br.com.clinicaleve.professional.ProfessionalDtos.ProfessionalRequest;
import br.com.clinicaleve.professional.ProfessionalDtos.ProfessionalResponse;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.specialty.Specialty;
import br.com.clinicaleve.specialty.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository repository;
    private final SpecialtyRepository specialtyRepository;

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> list() {
        var clinicId = TenantAccess.currentClinicId();
        var specialties = specialtyRepository.findByClinicIdAndActiveTrueOrderByName(clinicId)
                .stream()
                .collect(Collectors.toMap(Specialty::getId, Function.identity()));
        return repository.findByClinicIdAndActiveTrueOrderByName(clinicId)
                .stream()
                .map(professional -> response(professional, specialties.get(professional.getSpecialtyId())))
                .toList();
    }

    @Transactional
    public ProfessionalResponse create(ProfessionalRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var specialty = specialtyRepository.findByIdAndClinicId(request.specialtyId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Especialidade não encontrada"));

        var professional = new Professional();
        professional.setClinicId(clinicId);
        professional.setSpecialtyId(specialty.getId());
        professional.setName(request.name().trim());
        professional.setCouncil(blankToNull(request.council()));
        professional.setEmail(blankToNull(request.email()));
        professional.setPhone(blankToNull(request.phone()));
        return response(repository.save(professional), specialty);
    }

    private ProfessionalResponse response(Professional professional, Specialty specialty) {
        return new ProfessionalResponse(
                professional.getId(),
                professional.getName(),
                professional.getCouncil(),
                professional.getEmail(),
                professional.getPhone(),
                professional.getSpecialtyId(),
                specialty == null ? "Inativa" : specialty.getName(),
                specialty == null ? "#8b9692" : specialty.getColor(),
                professional.isActive()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
