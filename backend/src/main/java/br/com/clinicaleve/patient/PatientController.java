package br.com.clinicaleve.patient;

import br.com.clinicaleve.patient.PatientDtos.PatientRequest;
import br.com.clinicaleve.patient.PatientDtos.PatientResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService service;

    @GetMapping
    List<PatientResponse> list(@RequestParam(required = false) String search) {
        return service.list(search);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PatientResponse create(@Valid @RequestBody PatientRequest request) {
        return service.create(request);
    }
}
