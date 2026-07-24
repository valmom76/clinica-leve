package br.com.clinicaleve.user;

import br.com.clinicaleve.user.UserDtos.CreateUserRequest;
import br.com.clinicaleve.user.UserDtos.UpdateUserRequest;
import br.com.clinicaleve.user.UserDtos.UserResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ClinicUserController {

    private final ClinicUserService service;

    @GetMapping
    List<UserResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest servletRequest
    ) {
        return service.create(request, servletRequest.getRemoteAddr());
    }

    @PutMapping("/{id}")
    UserResponse update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return service.update(id, request);
    }
}
