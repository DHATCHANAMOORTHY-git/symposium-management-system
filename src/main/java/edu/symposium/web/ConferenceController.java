package edu.symposium.web;

import edu.symposium.domain.Conference;
import edu.symposium.domain.ConferenceStatus;
import edu.symposium.repository.ConferenceRepository;
import edu.symposium.service.RegistrationEligibilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ConferenceController {
    private final ConferenceRepository conferences;
    private final RegistrationEligibilityService registrationEligibility;

    public ConferenceController(
            ConferenceRepository conferences,
            RegistrationEligibilityService registrationEligibility) {
        this.conferences = conferences;
        this.registrationEligibility = registrationEligibility;
    }

    @GetMapping("/events")
    public List<ConferenceResponse> list(
            @RequestParam(required = false) ConferenceStatus status,
            @RequestParam(required = false) String category) {
        List<Conference> matchingStatus = status == null
                ? conferences.findAll()
                : conferences.findByStatusOrderByStartDateAsc(status);
        return matchingStatus.stream()
                .filter(conference -> category == null || category.isBlank()
                        || category.equalsIgnoreCase(conference.getCategory()))
                .sorted((left, right) -> {
                    if (left.getStartDate() == null) return right.getStartDate() == null ? 0 : 1;
                    if (right.getStartDate() == null) return -1;
                    return left.getStartDate().compareTo(right.getStartDate());
                })
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/events/{id}")
    public ConferenceResponse registrationDetails(@PathVariable @NonNull Long id) {
        Conference conference = conferences.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conference not found."));
        return toResponse(conference);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public Conference create(@Valid @RequestBody @NonNull Conference conference) {
        return conferences.save(conference);
    }

    @PutMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public Conference update(@PathVariable @NonNull Long id, @Valid @RequestBody @NonNull Conference incoming) {
        Conference current = conferences.findById(id).orElseThrow();
        current.setTitle(incoming.getTitle());
        current.setDescription(incoming.getDescription());
        current.setCategory(incoming.getCategory());
        current.setOrganizerName(incoming.getOrganizerName());
        current.setStartDate(incoming.getStartDate());
        current.setEndDate(incoming.getEndDate());
        current.setVenue(incoming.getVenue());
        current.setOnline(incoming.isOnline());
        current.setRegistrationDeadline(incoming.getRegistrationDeadline());
        current.setPaperDeadline(incoming.getPaperDeadline());
        current.setRegistrationFee(incoming.getRegistrationFee());
        current.setMaxParticipants(incoming.getMaxParticipants());
        current.setStatus(incoming.getStatus());
        current.setTheme(incoming.getTheme());
        current.setTracks(incoming.getTracks());
        current.setImageUrl(incoming.getImageUrl());
        return conferences.save(current);
    }

    @DeleteMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public void delete(@PathVariable @NonNull Long id) {
        conferences.deleteById(id);
    }

    private ConferenceResponse toResponse(Conference conference) {
        return ConferenceResponse.from(conference, registrationEligibility.evaluate(conference));
    }
}
