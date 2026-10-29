package edu.symposium.web;

import edu.symposium.domain.Conference;
import edu.symposium.domain.ConferenceStatus;
import edu.symposium.service.RegistrationEligibilityService;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConferenceResponse(
        Long id,
        String title,
        String description,
        String category,
        String organizerName,
        String venue,
        String imageUrl,
        String department,
        boolean online,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate registrationDeadline,
        LocalDate paperDeadline,
        BigDecimal registrationFee,
        Integer maxParticipants,
        ConferenceStatus status,
        String theme,
        String tracks,
        long registeredCount,
        Integer availableSeats,
        boolean registrationOpen,
        String registrationStatus,
        String registrationMessage) {

    public static ConferenceResponse from(
            Conference conference,
            RegistrationEligibilityService.Eligibility eligibility) {
        return new ConferenceResponse(
                conference.getId(),
                conference.getTitle(),
                conference.getDescription(),
                conference.getCategory(),
                conference.getOrganizerName(),
                conference.getVenue(),
                conference.getImageUrl(),
                conference.getDepartment(),
                conference.isOnline(),
                conference.getStartDate(),
                conference.getEndDate(),
                conference.getRegistrationDeadline(),
                conference.getPaperDeadline(),
                conference.getRegistrationFee(),
                conference.getMaxParticipants(),
                conference.getStatus(),
                conference.getTheme(),
                conference.getTracks(),
                eligibility.registeredCount(),
                eligibility.availableSeats(),
                eligibility.registrationOpen(),
                eligibility.registrationStatus(),
                eligibility.registrationMessage());
    }
}
