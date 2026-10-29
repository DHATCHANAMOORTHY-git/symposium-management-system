package edu.symposium.web;

import edu.symposium.domain.PaperStatus;
import edu.symposium.repository.ConferenceRepository;
import edu.symposium.repository.PaperRepository;
import edu.symposium.repository.RegistrationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final ConferenceRepository conferences;
    private final RegistrationRepository registrations;
    private final PaperRepository papers;

    public DashboardController(ConferenceRepository conferences, RegistrationRepository registrations, PaperRepository papers) {
        this.conferences = conferences;
        this.registrations = registrations;
        this.papers = papers;
    }

    @GetMapping("/dashboard")
    public Map<String, Long> summary() {
        return Map.of(
                "conferences", conferences.count(),
                "registrations", registrations.count(),
                "paidRegistrations", registrations.countByPaymentComplete(true),
                "attendees", registrations.countByAttended(true),
                "papers", papers.count(),
                "acceptedPapers", papers.countByStatus(PaperStatus.ACCEPTED),
                "pendingPapers", papers.countByStatus(PaperStatus.SUBMITTED) + papers.countByStatus(PaperStatus.UNDER_REVIEW)
        );
    }

    @GetMapping("/me")
    public Map<String, String> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        String role = "PARTICIPANT";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority != null && authority.getAuthority() != null
                    && authority.getAuthority().startsWith("ROLE_")) {
                role = authority.getAuthority().substring(5);
                break;
            }
        }

        return Map.of(
                "username", authentication.getName(),
                "role", role.toUpperCase(Locale.ROOT)
        );
    }
}

