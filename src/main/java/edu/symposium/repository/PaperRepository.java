package edu.symposium.repository;

import edu.symposium.domain.Paper;
import edu.symposium.domain.PaperStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    long countByStatus(PaperStatus status);
}
