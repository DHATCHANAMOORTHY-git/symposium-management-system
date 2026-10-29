package edu.symposium.repository;

import edu.symposium.domain.Conference;
import edu.symposium.domain.ConferenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ConferenceRepository extends JpaRepository<Conference, Long> {
    List<Conference> findByStatusOrderByStartDateAsc(ConferenceStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select conference from Conference conference where conference.id = :id")
    Optional<Conference> findByIdForUpdate(@Param("id") Long id);
}
