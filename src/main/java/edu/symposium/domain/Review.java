package edu.symposium.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Getter @Setter @NoArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String reviewerName;
    private Integer technicalScore;
    private Integer originalityScore;
    private Integer qualityScore;
    @Column(length = 2000) private String comments;
    private String recommendation;
    private Instant submittedAt = Instant.now();
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Paper paper;
}
