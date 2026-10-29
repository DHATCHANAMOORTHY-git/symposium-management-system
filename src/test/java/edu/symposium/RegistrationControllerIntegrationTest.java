package edu.symposium;

import edu.symposium.domain.Conference;
import edu.symposium.domain.ConferenceStatus;
import edu.symposium.domain.Registration;
import edu.symposium.repository.ConferenceRepository;
import edu.symposium.repository.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Test
    void duplicateRegistrationShouldBeBlocked() throws Exception {
        Conference conference = new Conference();
        conference.setTitle("Duplicate Test Symposium");
        conference.setDescription("For integration check");
        conference.setStartDate(LocalDate.now().plusDays(20));
        conference.setEndDate(LocalDate.now().plusDays(21));
        conference.setVenue("Innovation Hall");
        conference.setRegistrationDeadline(LocalDate.now().plusDays(10));
        conference.setPaperDeadline(LocalDate.now().plusDays(5));
        conference.setRegistrationFee(new BigDecimal("350.00"));
        conference.setMaxParticipants(200);
        conference.setStatus(ConferenceStatus.PUBLISHED);
        conference.setTheme("AI");
        conferenceRepository.save(conference);

        String payload = "{\"fullName\":\"Alice Example\",\"email\":\"alice@example.com\",\"participantType\":\"STUDENT\"}";

        MockHttpServletRequestBuilder firstRequest = post("/api/registrations")
                .param("conferenceId", String.valueOf(conference.getId()))
                .contentType("application/json")
                .content(payload);

        MockHttpServletRequestBuilder secondRequest = post("/api/registrations")
                .param("conferenceId", String.valueOf(conference.getId()))
                .contentType("application/json")
                .content(payload);

        mockMvc.perform(firstRequest)
                .andExpect(status().isCreated());

        mockMvc.perform(secondRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void fullConferenceShouldRejectNewRegistration() throws Exception {
        Conference conference = new Conference();
        conference.setTitle("Capacity Test Symposium");
        conference.setDescription("For integration check");
        conference.setStartDate(LocalDate.now().plusDays(25));
        conference.setEndDate(LocalDate.now().plusDays(26));
        conference.setVenue("Innovation Hall");
        conference.setRegistrationDeadline(LocalDate.now().plusDays(20));
        conference.setPaperDeadline(LocalDate.now().plusDays(5));
        conference.setRegistrationFee(new BigDecimal("350.00"));
        conference.setMaxParticipants(1);
        conference.setStatus(ConferenceStatus.PUBLISHED);
        conference.setTheme("AI");
        conference = conferenceRepository.save(conference);

        Registration registration = new Registration();
        registration.setConference(conference);
        registration.setFullName("Existing Participant");
        registration.setEmail("existing@example.com");
        registration.setParticipantType(edu.symposium.domain.ParticipantType.STUDENT);
        registrationRepository.save(registration);

        String payload = "{\"fullName\":\"New Participant\",\"email\":\"new@example.com\",\"participantType\":\"STUDENT\"}";

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationDeadlinePassedShouldBeRejectedAndReportedByEventApi() throws Exception {
        LocalDate today = today();
        Conference conference = saveConference("Deadline Test", today.plusDays(5), today.plusDays(6),
                today.minusDays(1), 20, ConferenceStatus.PUBLISHED);

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content(registrationPayload("deadline@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REGISTRATION_DEADLINE_PASSED"));

        mockMvc.perform(get("/api/events/{id}", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationOpen").value(false))
                .andExpect(jsonPath("$.registrationStatus").value("REGISTRATION_DEADLINE_PASSED"))
                .andExpect(jsonPath("$.availableSeats").value(20));
    }

    @Test
    void endedConferenceShouldRejectRegistration() throws Exception {
        LocalDate today = today();
        Conference conference = saveConference("Ended Test", today.minusDays(2), today.minusDays(1),
                today.plusDays(2), 20, ConferenceStatus.PUBLISHED);

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content(registrationPayload("ended@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_ENDED"));
    }

    @Test
    void cancelledRegistrationShouldNotConsumeCapacity() throws Exception {
        LocalDate today = today();
        Conference conference = saveConference("Cancelled Seat Test", today.plusDays(5), today.plusDays(6),
                today.plusDays(3), 2, ConferenceStatus.PUBLISHED);
        Registration cancelled = new Registration();
        cancelled.setConference(conference);
        cancelled.setFullName("Cancelled Participant");
        cancelled.setEmail("cancelled@example.com");
        cancelled.setParticipantType(edu.symposium.domain.ParticipantType.STUDENT);
        cancelled.setStatus(edu.symposium.domain.RegistrationStatus.CANCELLED);
        registrationRepository.save(cancelled);

        mockMvc.perform(get("/api/events/{id}", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(2))
                .andExpect(jsonPath("$.registrationOpen").value(true));

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content(registrationPayload("new-participant@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content(registrationPayload("cancelled@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events/{id}", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredCount").value(2))
                .andExpect(jsonPath("$.availableSeats").value(0))
                .andExpect(jsonPath("$.registrationOpen").value(false));
    }

    @Test
    void todayDeadlineAndEndDateRemainInclusiveAndSuccessfulRegistrationUpdatesSeats() throws Exception {
        LocalDate today = today();
        Conference conference = saveConference("Today Eligibility Test", today, today, today, 2,
                ConferenceStatus.PUBLISHED);

        mockMvc.perform(get("/api/events/{id}", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationOpen").value(true))
                .andExpect(jsonPath("$.availableSeats").value(2));

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content(registrationPayload("today@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events/{id}", conference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredCount").value(1))
                .andExpect(jsonPath("$.availableSeats").value(1))
                .andExpect(jsonPath("$.registrationOpen").value(true));
    }

    @Test
    void publishedConferenceWithoutDeadlineCanAcceptRegistration() throws Exception {
        LocalDate today = today();
        Conference conference = saveConference("No Deadline Test", today.plusDays(5), today.plusDays(6),
                null, 2, ConferenceStatus.PUBLISHED);

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content(registrationPayload("no-deadline@example.com")))
                .andExpect(status().isCreated());
    }

    @Test
    void draftConferenceShouldRejectRegistration() throws Exception {
        LocalDate today = today();
        Conference conference = saveConference("Draft Test", today.plusDays(5), today.plusDays(6),
                today.plusDays(3), 20, ConferenceStatus.DRAFT);

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content(registrationPayload("draft@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_CLOSED"));
    }

    @Test
    void cancelledConferenceShouldRejectRegistration() throws Exception {
        LocalDate today = today();
        Conference conference = saveConference("Cancelled Event Test", today.plusDays(5), today.plusDays(6),
                today.plusDays(3), 20, ConferenceStatus.CANCELLED);

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content(registrationPayload("cancelled-event@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("This event has been cancelled."));
    }

    @Test
    void nonexistentConferenceShouldReturnStructuredNotFound() throws Exception {
        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", "999999")
                        .contentType("application/json")
                        .content(registrationPayload("missing@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFERENCE_NOT_FOUND"));
    }

    @Test
    void invalidRegistrationShouldReturnBadRequest() throws Exception {
        Conference conference = saveConference("Invalid Registration Test", today().plusDays(5),
                today().plusDays(6), today().plusDays(3), 20, ConferenceStatus.PUBLISHED);

        mockMvc.perform(post("/api/registrations")
                        .param("conferenceId", String.valueOf(conference.getId()))
                        .contentType("application/json")
                        .content("{\"fullName\":\" \",\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REGISTRATION_DATA"));
    }

    @Test
    @WithMockUser(username = "participant", roles = "PARTICIPANT")
    void currentUserEndpointShouldReturnPrincipalDetails() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("participant"))
                .andExpect(jsonPath("$.role").value("PARTICIPANT"));
    }

    private Conference saveConference(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationDeadline,
            int capacity,
            ConferenceStatus status) {
        Conference conference = new Conference();
        conference.setTitle(title);
        conference.setStartDate(startDate);
        conference.setEndDate(endDate);
        conference.setRegistrationDeadline(registrationDeadline);
        conference.setMaxParticipants(capacity);
        conference.setStatus(status);
        conference.setRegistrationFee(BigDecimal.ZERO);
        return conferenceRepository.save(conference);
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }

    private String registrationPayload(String email) {
        return "{\"fullName\":\"Test Participant\",\"email\":\"" + email
                + "\",\"participantType\":\"STUDENT\"}";
    }
}
