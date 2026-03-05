package ch.martinelli.jug.feedback.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "feedback_answer")
public class FeedbackAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    private FeedbackResponse response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private FeedbackQuestion question;

    private Integer ratingValue;

    @Column(length = 2000)
    private String textValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FeedbackResponse getResponse() { return response; }
    public void setResponse(FeedbackResponse response) { this.response = response; }

    public FeedbackQuestion getQuestion() { return question; }
    public void setQuestion(FeedbackQuestion question) { this.question = question; }

    public Integer getRatingValue() { return ratingValue; }
    public void setRatingValue(Integer ratingValue) { this.ratingValue = ratingValue; }

    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }
}
