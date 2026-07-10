package br.com.clinicaleve.patient;

import br.com.clinicaleve.patient.PatientDtos.PatientRequest;
import br.com.clinicaleve.patient.PatientDtos.PatientResponse;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository repository;

    @Transactional(readOnly = true)
    public List<PatientResponse> list(String search) {
        var clinicId = TenantAccess.currentClinicId();
        var patients = search == null || search.isBlank()
                ? repository.findByClinicIdAndActiveTrueOrderByName(clinicId)
                : repository.findByClinicIdAndActiveTrueAndNameContainingIgnoreCaseOrderByName(
                        clinicId,
                        search.trim()
                );
        return patients.stream().map(PatientResponse::from).toList();
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        var patient = new Patient();
        patient.setClinicId(TenantAccess.currentClinicId());
        patient.setName(request.name().trim());
        patient.setCpf(blankToNull(request.cpf()));
        patient.setBirthDate(request.birthDate());
        patient.setEmail(blankToNull(request.email()));
        patient.setPhone(request.phone().trim());
        return PatientResponse.from(repository.save(patient));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
