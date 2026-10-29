package edu.symposium.service;

import edu.symposium.domain.Conference;
import edu.symposium.domain.Registration;
import edu.symposium.domain.RegistrationStatus;
import edu.symposium.repository.ConferenceRepository;
import edu.symposium.repository.RegistrationRepository;
import edu.symposium.web.RegistrationException;
import edu.symposium.web.RegistrationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegistrationService {
    private final RegistrationRepository registrations;
    private final ConferenceRepository conferences;
    private final RegistrationEligibilityService eligibility;

    public RegistrationService(
            RegistrationRepository registrations,
            ConferenceRepository conferences,
            RegistrationEligibilityService eligibility) {
        this.registrations = registrations;
        this.conferences = conferences;
        this.eligibility = eligibility;
    }

    @Transactional
    public Registration register(Long conferenceId, RegistrationRequest request) {
        Conference conference = conferences.findByIdForUpdate(conferenceId)
                .orElseThrow(() -> new RegistrationException(
                        HttpStatus.NOT_FOUND, "CONFERENCE_NOT_FOUND", "Conference not found."));

        RegistrationEligibilityService.Eligibility currentEligibility =
                eligibility.evaluate(conference);
        if (!currentEligibility.registrationOpen()) {
            throw new RegistrationException(
                    HttpStatus.CONFLICT,
                    currentEligibility.registrationStatus(),
                    currentEligibility.registrationMessage());
        }

        String normalizedEmail = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (registrations.existsByConferenceIdAndEmailIgnoreCaseAndStatusNot(
                conferenceId, normalizedEmail, RegistrationStatus.CANCELLED)) {
            throw new RegistrationException(HttpStatus.CONFLICT, "ALREADY_REGISTERED",
                    "You are already registered for this event.");
        }

        Registration registration = registrations.findByConferenceIdAndEmailIgnoreCaseAndStatus(
                        conferenceId, normalizedEmail, RegistrationStatus.CANCELLED)
                .orElseGet(Registration::new);
        registration.setConference(conference);
        registration.setFullName(request.fullName().trim());
        registration.setEmail(normalizedEmail);
        registration.setPhone(request.phone());
        registration.setCollege(request.college());
        registration.setDepartment(request.department());
        registration.setAcademicYear(request.academicYear());
        registration.setFoodPreference(request.foodPreference() == null
                ? "Vegetarian"
                : request.foodPreference());
        registration.setAccommodationRequired(request.accommodationRequired());
        registration.setParticipantType(request.participantType());
        registration.setStatus(RegistrationStatus.PENDING);
        registration.setPaymentComplete(false);
        registration.setAttended(false);
        registration.setRegisteredAt(java.time.Instant.now());
        registration.setRegistrationId("REG-" + UUID.randomUUID());
        return registrations.save(registration);
    }
}
