package ch.martinelli.jug.feedback.repository;

import ch.martinelli.jug.feedback.entity.AccessToken;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static ch.martinelli.jug.feedback.jooq.Tables.ACCESS_TOKEN;

@Repository
public class AccessTokenRepository {

    private final DSLContext dsl;

    public AccessTokenRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public AccessToken save(AccessToken token) {
        if (token.id() == null) {
            var id = dsl.insertInto(ACCESS_TOKEN)
                    .set(ACCESS_TOKEN.EMAIL, token.email())
                    .set(ACCESS_TOKEN.TOKEN, token.token())
                    .set(ACCESS_TOKEN.USED, token.used())
                    .set(ACCESS_TOKEN.CREATED_AT, token.createdAt())
                    .set(ACCESS_TOKEN.EXPIRES_AT, token.expiresAt())
                    .returning(ACCESS_TOKEN.ID)
                    .fetchOne(ACCESS_TOKEN.ID);
            return token.withId(id);
        } else {
            dsl.update(ACCESS_TOKEN)
                    .set(ACCESS_TOKEN.EMAIL, token.email())
                    .set(ACCESS_TOKEN.TOKEN, token.token())
                    .set(ACCESS_TOKEN.USED, token.used())
                    .set(ACCESS_TOKEN.CREATED_AT, token.createdAt())
                    .set(ACCESS_TOKEN.EXPIRES_AT, token.expiresAt())
                    .where(ACCESS_TOKEN.ID.eq(token.id()))
                    .execute();
            return token;
        }
    }

    public Optional<AccessToken> findByToken(String token) {
        return dsl.select(ACCESS_TOKEN.ID, ACCESS_TOKEN.EMAIL, ACCESS_TOKEN.TOKEN, ACCESS_TOKEN.USED, ACCESS_TOKEN.CREATED_AT, ACCESS_TOKEN.EXPIRES_AT)
                .from(ACCESS_TOKEN)
                .where(ACCESS_TOKEN.TOKEN.eq(token))
                .fetchOptional(Records.mapping(AccessToken::new));
    }

    public Optional<AccessToken> findByEmailAndTokenAndUsedFalse(String email, String token) {
        return dsl.select(ACCESS_TOKEN.ID, ACCESS_TOKEN.EMAIL, ACCESS_TOKEN.TOKEN, ACCESS_TOKEN.USED, ACCESS_TOKEN.CREATED_AT, ACCESS_TOKEN.EXPIRES_AT)
                .from(ACCESS_TOKEN)
                .where(ACCESS_TOKEN.EMAIL.eq(email)
                        .and(ACCESS_TOKEN.TOKEN.eq(token))
                        .and(ACCESS_TOKEN.USED.eq(false)))
                .fetchOptional(Records.mapping(AccessToken::new));
    }

    @Transactional
    public void deleteByEmail(String email) {
        dsl.deleteFrom(ACCESS_TOKEN)
                .where(ACCESS_TOKEN.EMAIL.eq(email))
                .execute();
    }
}
