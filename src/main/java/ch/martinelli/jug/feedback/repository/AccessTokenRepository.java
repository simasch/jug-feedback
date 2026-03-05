package ch.martinelli.jug.feedback.repository;

import ch.martinelli.jug.feedback.entity.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessTokenRepository extends JpaRepository<AccessToken, Long> {
    Optional<AccessToken> findByToken(String token);
    Optional<AccessToken> findByEmailAndTokenAndUsedFalse(String email, String token);
    void deleteByEmail(String email);
}
