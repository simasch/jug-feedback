package ch.martinelli.jug.feedback.repository;

import ch.martinelli.jug.feedback.entity.FeedbackAnswer;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.jug.feedback.jooq.Tables.FEEDBACK_ANSWER;

@Repository
public class FeedbackAnswerRepository {

    private final DSLContext dsl;

    public FeedbackAnswerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public FeedbackAnswer save(FeedbackAnswer answer) {
        if (answer.id() == null) {
            var id = dsl.insertInto(FEEDBACK_ANSWER)
                    .set(FEEDBACK_ANSWER.RESPONSE_ID, answer.responseId())
                    .set(FEEDBACK_ANSWER.QUESTION_ID, answer.questionId())
                    .set(FEEDBACK_ANSWER.RATING_VALUE, answer.ratingValue())
                    .set(FEEDBACK_ANSWER.TEXT_VALUE, answer.textValue())
                    .returning(FEEDBACK_ANSWER.ID)
                    .fetchOne(FEEDBACK_ANSWER.ID);
            return answer.withId(id);
        } else {
            dsl.update(FEEDBACK_ANSWER)
                    .set(FEEDBACK_ANSWER.RESPONSE_ID, answer.responseId())
                    .set(FEEDBACK_ANSWER.QUESTION_ID, answer.questionId())
                    .set(FEEDBACK_ANSWER.RATING_VALUE, answer.ratingValue())
                    .set(FEEDBACK_ANSWER.TEXT_VALUE, answer.textValue())
                    .where(FEEDBACK_ANSWER.ID.eq(answer.id()))
                    .execute();
            return answer;
        }
    }

    public Double findAverageRatingByQuestionId(Long questionId) {
        return dsl.select(DSL.avg(FEEDBACK_ANSWER.RATING_VALUE))
                .from(FEEDBACK_ANSWER)
                .where(FEEDBACK_ANSWER.QUESTION_ID.eq(questionId)
                        .and(FEEDBACK_ANSWER.RATING_VALUE.isNotNull()))
                .fetchOne(0, Double.class);
    }

    public List<FeedbackAnswer> findByQuestionIdAndTextValueIsNotNull(Long questionId) {
        return dsl.select(FEEDBACK_ANSWER.ID, FEEDBACK_ANSWER.RESPONSE_ID, FEEDBACK_ANSWER.QUESTION_ID, FEEDBACK_ANSWER.RATING_VALUE, FEEDBACK_ANSWER.TEXT_VALUE)
                .from(FEEDBACK_ANSWER)
                .where(FEEDBACK_ANSWER.QUESTION_ID.eq(questionId)
                        .and(FEEDBACK_ANSWER.TEXT_VALUE.isNotNull()))
                .fetch(Records.mapping(FeedbackAnswer::new));
    }
}
