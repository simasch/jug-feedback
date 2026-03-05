package ch.martinelli.jug.feedback.repository;

import ch.martinelli.jug.feedback.entity.FeedbackForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackFormRepository extends JpaRepository<FeedbackForm, Long> {
    List<FeedbackForm> findAllByOrderByCreatedAtDesc();
    Optional<FeedbackForm> findByPublicToken(String publicToken);

    @Query("SELECT f FROM FeedbackForm f WHERE f.ownerEmail = :email " +
           "OR f.id IN (SELECT s.form.id FROM FormShare s WHERE s.sharedWithEmail = :email) " +
           "ORDER BY f.createdAt DESC")
    List<FeedbackForm> findAllAccessibleByEmail(@Param("email") String email);
}
