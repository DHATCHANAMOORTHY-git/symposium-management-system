package edu.symposium.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Conference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String category = "General";

    @Column(length = 500)
    private String organizerName = "Symposium Team";

    @Column(length = 500)
    private String venue = "TBA";

    @Column(length = 500)
    private String imageUrl = "https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1200&q=80";

    @Column(length = 500)
    private String department = "Technology";

    private boolean online = false;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registrationDeadline;
    private LocalDate paperDeadline;
    private BigDecimal registrationFee = BigDecimal.ZERO;
    private Integer maxParticipants = 100;

    @Enumerated(EnumType.STRING)
    private ConferenceStatus status = ConferenceStatus.DRAFT;

    private String theme;
    private String tracks;

    @JsonIgnore
    @OneToMany(mappedBy = "conference", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Session> sessions = new ArrayList<>();
}

