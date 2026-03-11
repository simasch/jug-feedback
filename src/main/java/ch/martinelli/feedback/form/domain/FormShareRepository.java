package ch.martinelli.feedback.form.domain;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.feedback.db.tables.FormShare.FORM_SHARE;

@Repository
public class FormShareRepository {

    private final DSLContext dsl;

    public FormShareRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public FormShare insert(FormShare share) {
        var id = dsl.insertInto(FORM_SHARE)
                .set(FORM_SHARE.FORM_ID, share.formId())
                .set(FORM_SHARE.SHARED_WITH_EMAIL, share.sharedWithEmail())
                .returning(FORM_SHARE.ID)
                .fetchOne(FORM_SHARE.ID);
        return share.withId(id);
    }

    public List<FormShare> findByFormId(Long formId) {
        return dsl.select(FORM_SHARE.ID, FORM_SHARE.FORM_ID, FORM_SHARE.SHARED_WITH_EMAIL)
                .from(FORM_SHARE)
                .where(FORM_SHARE.FORM_ID.eq(formId))
                .fetch(Records.mapping(FormShare::new));
    }

    @Transactional
    public void deleteByFormIdAndSharedWithEmail(Long formId, String email) {
        dsl.deleteFrom(FORM_SHARE)
                .where(FORM_SHARE.FORM_ID.eq(formId)
                        .and(FORM_SHARE.SHARED_WITH_EMAIL.eq(email)))
                .execute();
    }

    public boolean existsByFormIdAndSharedWithEmail(Long formId, String email) {
        return dsl.fetchExists(
                dsl.selectFrom(FORM_SHARE)
                        .where(FORM_SHARE.FORM_ID.eq(formId)
                                .and(FORM_SHARE.SHARED_WITH_EMAIL.eq(email))));
    }
}
