package ch.martinelli.jug.feedback.entity;

import java.time.LocalDateTime;

public record FeedbackResponse(Long id, Long formId, LocalDateTime submittedAt) {

    public FeedbackResponse withId(Long id) {
        return new FeedbackResponse(id, formId, submittedAt);
    }
}
