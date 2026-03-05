package ch.martinelli.jug.feedback.repository;

import ch.martinelli.jug.feedback.entity.FeedbackForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackFormRepository extends JpaRepository<FeedbackForm, Long> {
    List<FeedbackForm> findAllByOrderByCreatedAtDesc();
    Optional<FeedbackForm> findByPublicToken(String publicToken);
}
