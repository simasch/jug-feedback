package ch.martinelli.jug.feedback.service;

import ch.martinelli.jug.feedback.entity.AccessToken;
import ch.martinelli.jug.feedback.repository.AccessTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final JavaMailSender mailSender;

    public TokenService(AccessTokenRepository tokenRepository, JavaMailSender mailSender) {
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void sendLoginCode(String email) {
        String code = String.format("%08d", random.nextInt(100_000_000));
        AccessToken token = new AccessToken();
        token.setEmail(email);
        token.setToken(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        tokenRepository.save(token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("simon@martinelli.ch");
        message.setTo(email);
        message.setSubject("JUG Feedback - Your Login Code");
        message.setText("Your login code is: " + code + "\n\nThis code expires in 10 minutes.");
        mailSender.send(message);

        logger.info("Login code sent to {}", email);
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
