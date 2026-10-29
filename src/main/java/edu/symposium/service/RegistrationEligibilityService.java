package edu.symposium.service;

import edu.symposium.domain.Conference;
import edu.symposium.domain.ConferenceStatus;
import edu.symposium.domain.RegistrationStatus;
import edu.symposium.repository.RegistrationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class RegistrationEligibilityService {
    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Kolkata");
    private static final List<RegistrationStatus> CAPACITY_STATUSES =
            List.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED);

    private final RegistrationRepository registrations;

    public RegistrationEligibilityService(RegistrationRepository registrations) {
        this.registrations = registrations;
    }

    public Eligibility evaluate(Conference conference) {
        long registeredCount = registrations.countByConferenceIdAndStatusIn(
                conference.getId(), CAPACITY_STATUSES);
        Integer capacity = conference.getMaxParticipants();
        Integer availableSeats = capacity == null || capacity <= 0
                ? null
                : (int) Math.max(0, capacity - registeredCount);
        LocalDate today = LocalDate.now(APPLICATION_ZONE);

        if (conference.getStatus() == ConferenceStatus.CANCELLED) {
            return new Eligibility(registeredCount, availableSeats, false,
                    "CANCELLED", "This event has been cancelled.");
        }
        if (conference.getStatus() == ConferenceStatus.COMPLETED
                || isEventEnded(conference, today)) {
            return new Eligibility(registeredCount, availableSeats, false,
                    "EVENT_ENDED", "This event has already ended.");
        }
        if (conference.getStatus() != ConferenceStatus.PUBLISHED) {
            return new Eligibility(registeredCount, availableSeats, false,
                    "REGISTRATION_CLOSED", "This event is not accepting registrations.");
        }
        if (conference.getRegistrationDeadline() != null
                && today.isAfter(conference.getRegistrationDeadline())) {
            return new Eligibility(registeredCount, availableSeats, false,
                    "REGISTRATION_DEADLINE_PASSED", "Registration deadline has passed.");
        }
        if (capacity != null && capacity > 0 && availableSeats == 0) {
            return new Eligibility(registeredCount, availableSeats, false,
                    "EVENT_FULL", "Registration is full for this event.");
        }
        return new Eligibility(registeredCount, availableSeats, true,
                "OPEN", "Registration is open.");
    }

    private boolean isEventEnded(Conference conference, LocalDate today) {
        LocalDate endDate = conference.getEndDate() != null
                ? conference.getEndDate()
                : conference.getStartDate();
        return endDate != null && today.isAfter(endDate);
    }

    public record Eligibility(
            long registeredCount,
            Integer availableSeats,
            boolean registrationOpen,
            String registrationStatus,
            String registrationMessage) {
    }
}
