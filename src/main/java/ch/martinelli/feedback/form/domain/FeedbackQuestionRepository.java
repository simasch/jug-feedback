package ch.martinelli.feedback.form.domain;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static ch.martinelli.feedback.db.tables.FeedbackQuestion.FEEDBACK_QUESTION;

@Repository
public class FeedbackQuestionRepository {

    private final DSLContext dsl;

    public FeedbackQuestionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public FeedbackQuestion save(FeedbackQuestion question) {
        var id = dsl.insertInto(FEEDBACK_QUESTION)
                .set(FEEDBACK_QUESTION.FORM_ID, question.formId())
                .set(FEEDBACK_QUESTION.QUESTION_TEXT, question.questionText())
                .set(FEEDBACK_QUESTION.QUESTION_TYPE, question.questionType())
                .set(FEEDBACK_QUESTION.ORDER_INDEX, question.orderIndex())
                .returning(FEEDBACK_QUESTION.ID)
                .fetchOne(FEEDBACK_QUESTION.ID);
        return question.withId(id);
    }

}
