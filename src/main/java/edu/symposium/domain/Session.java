package edu.symposium.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor
public class Session {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String title;
    private String type;
    private String speaker;
    private String room;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conference_id") private Conference conference;
}
