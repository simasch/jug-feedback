package ch.martinelli.jug.feedback.repository;

import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.entity.FeedbackQuestion;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ch.martinelli.jug.feedback.jooq.Tables.*;
import static java.util.stream.Collectors.groupingBy;

@Repository
public class FeedbackFormRepository {

    private final DSLContext dsl;

    public FeedbackFormRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public FeedbackForm save(FeedbackForm form) {
        if (form.id() == null) {
            var id = dsl.insertInto(FEEDBACK_FORM)
                    .set(FEEDBACK_FORM.TITLE, form.title())
                    .set(FEEDBACK_FORM.SPEAKER_NAME, form.speakerName())
                    .set(FEEDBACK_FORM.EVENT_DATE, form.eventDate())
                    .set(FEEDBACK_FORM.LOCATION, form.location())
                    .set(FEEDBACK_FORM.STATUS, form.status())
                    .set(FEEDBACK_FORM.PUBLIC_TOKEN, form.publicToken())
                    .set(FEEDBACK_FORM.OWNER_EMAIL, form.ownerEmail())
                    .set(FEEDBACK_FORM.CREATED_AT, form.createdAt())
                    .returning(FEEDBACK_FORM.ID)
                    .fetchOne(FEEDBACK_FORM.ID);
            return form.withId(id);
        } else {
            dsl.update(FEEDBACK_FORM)
                    .set(FEEDBACK_FORM.TITLE, form.title())
                    .set(FEEDBACK_FORM.SPEAKER_NAME, form.speakerName())
                    .set(FEEDBACK_FORM.EVENT_DATE, form.eventDate())
                    .set(FEEDBACK_FORM.LOCATION, form.location())
                    .set(FEEDBACK_FORM.STATUS, form.status())
                    .set(FEEDBACK_FORM.PUBLIC_TOKEN, form.publicToken())
                    .set(FEEDBACK_FORM.OWNER_EMAIL, form.ownerEmail())
                    .set(FEEDBACK_FORM.CREATED_AT, form.createdAt())
                    .where(FEEDBACK_FORM.ID.eq(form.id()))
                    .execute();

            // Cascade save questions: delete existing and re-insert
            dsl.deleteFrom(FEEDBACK_QUESTION)
                    .where(FEEDBACK_QUESTION.FORM_ID.eq(form.id()))
                    .execute();
            var savedQuestions = new ArrayList<FeedbackQuestion>();
            for (var question : form.questions()) {
                var q = question.withFormId(form.id());
                var qId = dsl.insertInto(FEEDBACK_QUESTION)
                        .set(FEEDBACK_QUESTION.FORM_ID, q.formId())
                        .set(FEEDBACK_QUESTION.QUESTION_TEXT, q.questionText())
                        .set(FEEDBACK_QUESTION.QUESTION_TYPE, q.questionType())
                        .set(FEEDBACK_QUESTION.ORDER_INDEX, q.orderIndex())
                        .returning(FEEDBACK_QUESTION.ID)
                        .fetchOne(FEEDBACK_QUESTION.ID);
                savedQuestions.add(q.withId(qId));
            }
            return form.withQuestions(savedQuestions);
        }
    }

    public Optional<FeedbackForm> findById(Long id) {
        return dsl.select(FEEDBACK_FORM.ID, FEEDBACK_FORM.TITLE, FEEDBACK_FORM.SPEAKER_NAME, FEEDBACK_FORM.EVENT_DATE, FEEDBACK_FORM.LOCATION, FEEDBACK_FORM.STATUS, FEEDBACK_FORM.PUBLIC_TOKEN, FEEDBACK_FORM.OWNER_EMAIL, FEEDBACK_FORM.CREATED_AT)
                .from(FEEDBACK_FORM)
                .where(FEEDBACK_FORM.ID.eq(id))
                .fetchOptional(Records.mapping((fId, title, speakerName, eventDate, location, status, publicToken, ownerEmail, createdAt) ->
                        new FeedbackForm(fId, title, speakerName, eventDate, location, status, publicToken, ownerEmail, createdAt, new ArrayList<>())))
                .map(this::loadQuestions);
    }

    public Optional<FeedbackForm> findByPublicToken(String publicToken) {
        return dsl.select(FEEDBACK_FORM.ID, FEEDBACK_FORM.TITLE, FEEDBACK_FORM.SPEAKER_NAME, FEEDBACK_FORM.EVENT_DATE, FEEDBACK_FORM.LOCATION, FEEDBACK_FORM.STATUS, FEEDBACK_FORM.PUBLIC_TOKEN, FEEDBACK_FORM.OWNER_EMAIL, FEEDBACK_FORM.CREATED_AT)
                .from(FEEDBACK_FORM)
                .where(FEEDBACK_FORM.PUBLIC_TOKEN.eq(publicToken))
                .fetchOptional(Records.mapping((fId, title, speakerName, eventDate, location, status, pToken, ownerEmail, createdAt) ->
                        new FeedbackForm(fId, title, speakerName, eventDate, location, status, pToken, ownerEmail, createdAt, new ArrayList<>())))
                .map(this::loadQuestions);
    }

    public List<FeedbackForm> findAllAccessibleByEmail(String email) {
        var forms = dsl.select(FEEDBACK_FORM.ID, FEEDBACK_FORM.TITLE, FEEDBACK_FORM.SPEAKER_NAME, FEEDBACK_FORM.EVENT_DATE, FEEDBACK_FORM.LOCATION, FEEDBACK_FORM.STATUS, FEEDBACK_FORM.PUBLIC_TOKEN, FEEDBACK_FORM.OWNER_EMAIL, FEEDBACK_FORM.CREATED_AT)
                .from(FEEDBACK_FORM)
                .where(FEEDBACK_FORM.OWNER_EMAIL.eq(email)
                        .or(FEEDBACK_FORM.ID.in(
                                dsl.select(FORM_SHARE.FORM_ID)
                                        .from(FORM_SHARE)
                                        .where(FORM_SHARE.SHARED_WITH_EMAIL.eq(email)))))
                .orderBy(FEEDBACK_FORM.CREATED_AT.desc())
                .fetch(Records.mapping((fId, title, speakerName, eventDate, location, status, publicToken, ownerEmail, createdAt) ->
                        new FeedbackForm(fId, title, speakerName, eventDate, location, status, publicToken, ownerEmail, createdAt, new ArrayList<>())));

        if (forms.isEmpty()) {
            return forms;
        }

        // Batch-load questions for all forms
        var formIds = forms.stream().map(FeedbackForm::id).toList();
        Map<Long, List<FeedbackQuestion>> questionsByFormId = dsl.select(FEEDBACK_QUESTION.ID, FEEDBACK_QUESTION.FORM_ID, FEEDBACK_QUESTION.QUESTION_TEXT, FEEDBACK_QUESTION.QUESTION_TYPE, FEEDBACK_QUESTION.ORDER_INDEX)
                .from(FEEDBACK_QUESTION)
                .where(FEEDBACK_QUESTION.FORM_ID.in(formIds))
                .orderBy(FEEDBACK_QUESTION.ORDER_INDEX)
                .fetch(Records.mapping(FeedbackQuestion::new))
                .stream()
                .collect(groupingBy(FeedbackQuestion::formId));

        return forms.stream()
                .map(form -> form.withQuestions(questionsByFormId.getOrDefault(form.id(), List.of())))
                .toList();
    }

    @Transactional
    public void deleteById(Long id) {
        dsl.deleteFrom(FEEDBACK_FORM)
                .where(FEEDBACK_FORM.ID.eq(id))
                .execute();
    }

    private FeedbackForm loadQuestions(FeedbackForm form) {
        var questions = dsl.select(FEEDBACK_QUESTION.ID, FEEDBACK_QUESTION.FORM_ID, FEEDBACK_QUESTION.QUESTION_TEXT, FEEDBACK_QUESTION.QUESTION_TYPE, FEEDBACK_QUESTION.ORDER_INDEX)
                .from(FEEDBACK_QUESTION)
                .where(FEEDBACK_QUESTION.FORM_ID.eq(form.id()))
                .orderBy(FEEDBACK_QUESTION.ORDER_INDEX)
                .fetch(Records.mapping(FeedbackQuestion::new));
        return form.withQuestions(questions);
    }
}
