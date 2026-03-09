package ch.martinelli.jug.feedback.repository;

import ch.martinelli.jug.feedback.entity.FormShare;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.jug.feedback.jooq.Tables.FORM_SHARE;

@Repository
public class FormShareRepository {

    private final DSLContext dsl;

    public FormShareRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public FormShare save(FormShare share) {
        if (share.id() == null) {
            var id = dsl.insertInto(FORM_SHARE)
                    .set(FORM_SHARE.FORM_ID, share.formId())
                    .set(FORM_SHARE.SHARED_WITH_EMAIL, share.sharedWithEmail())
                    .returning(FORM_SHARE.ID)
                    .fetchOne(FORM_SHARE.ID);
            return share.withId(id);
        } else {
            dsl.update(FORM_SHARE)
                    .set(FORM_SHARE.FORM_ID, share.formId())
                    .set(FORM_SHARE.SHARED_WITH_EMAIL, share.sharedWithEmail())
                    .where(FORM_SHARE.ID.eq(share.id()))
                    .execute();
            return share;
        }
    }

    public List<FormShare> findByFormId(Long formId) {
        return dsl.select(FORM_SHARE.ID, FORM_SHARE.FORM_ID, FORM_SHARE.SHARED_WITH_EMAIL)
                .from(FORM_SHARE)
                .where(FORM_SHARE.FORM_ID.eq(formId))
                .fetch(Records.mapping(FormShare::new));
    }

    public List<FormShare> findBySharedWithEmail(String email) {
        return dsl.select(FORM_SHARE.ID, FORM_SHARE.FORM_ID, FORM_SHARE.SHARED_WITH_EMAIL)
                .from(FORM_SHARE)
                .where(FORM_SHARE.SHARED_WITH_EMAIL.eq(email))
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
