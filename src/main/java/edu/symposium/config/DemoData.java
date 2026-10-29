package edu.symposium.config;

import edu.symposium.domain.Conference;
import edu.symposium.domain.ConferenceStatus;
import edu.symposium.repository.ConferenceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DemoData {
    @Bean
    CommandLineRunner seed(ConferenceRepository conferences) {
        return args -> {
            if (conferences.count() > 0) {
                return;
            }

            Conference ai = new Conference();
            ai.setTitle("AI & Machine Learning Symposium 2026");
            ai.setCategory("AI & ML");
            ai.setOrganizerName("Anna University Research Cell");
            ai.setDepartment("Computer Science");
            ai.setTheme("Build the future with responsible AI");
            ai.setDescription("A two-day deep dive into applied AI, ethical systems, and emerging ML workflows for students and professionals.");
            ai.setStartDate(LocalDate.of(2026, 9, 30));
            ai.setEndDate(LocalDate.of(2026, 10, 1));
            ai.setVenue("Anna University Convention Center, Chennai");
            ai.setOnline(false);
            ai.setRegistrationDeadline(LocalDate.of(2026, 9, 12));
            ai.setPaperDeadline(LocalDate.of(2026, 8, 20));
            ai.setRegistrationFee(new BigDecimal("500.00"));
            ai.setMaxParticipants(250);
            ai.setStatus(ConferenceStatus.PUBLISHED);
            ai.setTracks("Responsible AI, NLP, ML Ops");
            ai.setImageUrl("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80");
            conferences.save(ai);

            Conference cyber = new Conference();
            cyber.setTitle("Cybersecurity & Digital Trust Summit");
            cyber.setCategory("Security");
            cyber.setOrganizerName("Cyber Defense Council");
            cyber.setDepartment("Information Security");
            cyber.setTheme("Secure systems for a connected world");
            cyber.setDescription("Explore cyber resilience strategies, zero trust architectures, and modern security engineering practices.");
            cyber.setStartDate(LocalDate.of(2026, 10, 14));
            cyber.setEndDate(LocalDate.of(2026, 10, 15));
            cyber.setVenue("Tech Park Auditorium, Bengaluru");
            cyber.setOnline(false);
            cyber.setRegistrationDeadline(LocalDate.of(2026, 10, 1));
            cyber.setPaperDeadline(LocalDate.of(2026, 9, 10));
            cyber.setRegistrationFee(new BigDecimal("650.00"));
            cyber.setMaxParticipants(180);
            cyber.setStatus(ConferenceStatus.PUBLISHED);
            cyber.setTracks("Zero Trust, Threat Intelligence, Cloud Security");
            cyber.setImageUrl("https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=1200&q=80");
            conferences.save(cyber);

            Conference health = new Conference();
            health.setTitle("Healthcare Innovation Forum");
            health.setCategory("Healthcare");
            health.setOrganizerName("MedTech Alliance");
            health.setDepartment("Healthcare Informatics");
            health.setTheme("Designing healthier digital experiences");
            health.setDescription("Discuss digital transformation, patient-centered design, and next generation clinical workflows.");
            health.setStartDate(LocalDate.of(2026, 11, 18));
            health.setEndDate(LocalDate.of(2026, 11, 20));
            health.setVenue("Global Health Center, Hyderabad");
            health.setOnline(true);
            health.setRegistrationDeadline(LocalDate.of(2026, 11, 2));
            health.setPaperDeadline(LocalDate.of(2026, 10, 7));
            health.setRegistrationFee(new BigDecimal("450.00"));
            health.setMaxParticipants(300);
            health.setStatus(ConferenceStatus.PUBLISHED);
            health.setTracks("AI in Medicine, Digital Health, Clinical Innovation");
            health.setImageUrl("https://images.unsplash.com/photo-1576091160550-2173dba999ef?auto=format&fit=crop&w=1200&q=80");
            conferences.save(health);
        };
    }
}

