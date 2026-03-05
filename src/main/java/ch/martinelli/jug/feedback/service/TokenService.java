package ch.martinelli.jug.feedback.service;

import ch.martinelli.jug.feedback.entity.AccessToken;
import ch.martinelli.jug.feedback.repository.AccessTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private static final SecureRandom random = new SecureRandom();

    private final AccessTokenRepository tokenRepository;

    public TokenService(AccessTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public void sendLoginCode(String email) {
        String code = String.format("%08d", random.nextInt(100_000_000));
        AccessToken token = new AccessToken();
        token.setEmail(email);
        token.setToken(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        tokenRepository.save(token);

        logger.info("=================================================");
        logger.info("Login code for {}: {}", email, code);
        logger.info("=================================================");
    }

    @Transactional
    public boolean validateCode(String email, String code) {
        Optional<AccessToken> optToken = tokenRepository.findByEmailAndTokenAndUsedFalse(email, code);
        if (optToken.isEmpty()) {
            return false;
        }
        AccessToken token = optToken.get();
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        token.setUsed(true);
        tokenRepository.save(token);
        return true;
    }
}
