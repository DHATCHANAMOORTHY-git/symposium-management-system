package edu.symposium.repository;

import edu.symposium.domain.Registration;
import edu.symposium.domain.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    long countByPaymentComplete(boolean paymentComplete);
    long countByAttended(boolean attended);
    long countByConferenceId(Long conferenceId);
    long countByConferenceIdAndStatusIn(Long conferenceId, List<RegistrationStatus> statuses);
    List<Registration> findByConferenceIdOrderByRegisteredAtDesc(Long conferenceId);
    List<Registration> findByEmailOrderByRegisteredAtDesc(String email);
    boolean existsByConferenceIdAndEmail(Long conferenceId, String email);
    boolean existsByConferenceIdAndEmailAndStatusNot(Long conferenceId, String email, RegistrationStatus status);
    boolean existsByConferenceIdAndEmailIgnoreCaseAndStatusNot(Long conferenceId, String email, RegistrationStatus status);
    java.util.Optional<Registration> findByConferenceIdAndEmailIgnoreCaseAndStatus(
            Long conferenceId, String email, RegistrationStatus status);
}
