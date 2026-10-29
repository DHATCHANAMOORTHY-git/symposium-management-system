package edu.symposium.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "registrations", uniqueConstraints = @UniqueConstraint(columnNames = {"email", "conference_id"}))
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String phone;
    private String college;
    private String department;
    @Column(name = "academic_year")
    private String academicYear;
    private String foodPreference = "Vegetarian";
    private boolean accommodationRequired;
    private String registrationId;
    private String workshop;

    @Enumerated(EnumType.STRING)
    private ParticipantType participantType;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    private boolean paymentComplete;
    private boolean attended;
    private Instant registeredAt = Instant.now();

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;
}

