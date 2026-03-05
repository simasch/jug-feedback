package ch.martinelli.jug.feedback.repository;

import ch.martinelli.jug.feedback.entity.FeedbackAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackAnswerRepository extends JpaRepository<FeedbackAnswer, Long> {

    @Query("SELECT AVG(a.ratingValue) FROM FeedbackAnswer a WHERE a.question.id = :questionId AND a.ratingValue IS NOT NULL")
    Double findAverageRatingByQuestionId(@Param("questionId") Long questionId);

    List<FeedbackAnswer> findByQuestionIdAndTextValueIsNotNull(Long questionId);
}
