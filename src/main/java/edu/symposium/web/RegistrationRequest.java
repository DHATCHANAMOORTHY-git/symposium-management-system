package edu.symposium.web;

import edu.symposium.domain.ParticipantType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationRequest(
        @NotBlank(message = "Full name is required.")
        String fullName,
        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        String email,
        String phone,
        String college,
        String department,
        String academicYear,
        String foodPreference,
        boolean accommodationRequired,
        ParticipantType participantType) {
}
