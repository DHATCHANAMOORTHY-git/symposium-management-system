package edu.symposium.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Getter @Setter @NoArgsConstructor
public class Paper {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String authorName;
    @Column(nullable = false) private String authorEmail;
    private String track;
    private String documentUrl;
    @Enumerated(EnumType.STRING) private PaperStatus status = PaperStatus.SUBMITTED;
    private Instant submittedAt = Instant.now();
    @ManyToOne(optional = false, fetch = FetchType.EAGER) private Conference conference;
}
