package edu.symposium.web;

import edu.symposium.domain.Registration;
import edu.symposium.domain.RegistrationStatus;
import edu.symposium.repository.RegistrationRepository;
import edu.symposium.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RegistrationController {
    private final RegistrationRepository registrations;
    private final RegistrationService registrationService;

    public RegistrationController(
            RegistrationRepository registrations,
            RegistrationService registrationService) {
        this.registrations = registrations;
        this.registrationService = registrationService;
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public List<Registration> list(@RequestParam(required = false) Long conferenceId,
                                  @RequestParam(required = false) String email) {
        if (conferenceId != null) {
            return registrations.findByConferenceIdOrderByRegisteredAtDesc(conferenceId);
        }
        if (email != null && !email.isBlank()) {
            return registrations.findByEmailOrderByRegisteredAtDesc(email.trim());
        }
        return registrations.findAll();
    }

    @GetMapping("/registrations/me")
    @PreAuthorize("hasAnyRole('PARTICIPANT','ADMIN','ORGANIZER')")
    public List<Registration> registrationsForCurrentUser(Principal principal,
                                                         @RequestParam(required = false) String email) {
        String resolvedEmail = (email == null || email.isBlank()) ? "" : email.trim();
        if ((resolvedEmail.isBlank() && principal == null) || (resolvedEmail.isBlank() && principal.getName() == null)) {
            return registrations.findAll();
        }

        if (resolvedEmail.isBlank()) {
            String currentUser = principal.getName().trim();
            boolean isRestrictedRole = !("admin".equalsIgnoreCase(currentUser) || "organizer".equalsIgnoreCase(currentUser));
            if (isRestrictedRole) {
                return registrations.findByEmailOrderByRegisteredAtDesc(currentUser);
            }
            return registrations.findAll();
        }

        return registrations.findByEmailOrderByRegisteredAtDesc(resolvedEmail);
    }

    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public Registration register(
            @RequestParam @NonNull Long conferenceId,
            @Valid @RequestBody RegistrationRequest registration) {
        return registrationService.register(conferenceId, registration);
    }

    @PatchMapping("/registrations/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','PARTICIPANT')")
    public Registration markPayment(@PathVariable @NonNull Long id, @RequestParam boolean complete) {
        Registration registration = registrations.findById(id).orElseThrow();
        registration.setPaymentComplete(complete);
        return registrations.save(registration);
    }

    @PatchMapping("/registrations/{id}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public Registration markAttendance(@PathVariable @NonNull Long id, @RequestParam boolean attended) {
        Registration registration = registrations.findById(id).orElseThrow();
        registration.setAttended(attended);
        return registrations.save(registration);
    }

    @DeleteMapping("/registrations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','PARTICIPANT')")
    public Registration cancel(@PathVariable @NonNull Long id) {
        Registration registration = registrations.findById(id).orElseThrow();
        registration.setStatus(RegistrationStatus.CANCELLED);
        return registrations.save(registration);
    }
}
