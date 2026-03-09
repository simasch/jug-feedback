package ch.martinelli.jug.feedback.service;

import ch.martinelli.jug.feedback.entity.*;
import ch.martinelli.jug.feedback.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FormService {

    private final FeedbackFormRepository formRepository;
    private final FeedbackQuestionRepository questionRepository;
    private final FeedbackResponseRepository responseRepository;
    private final FeedbackAnswerRepository answerRepository;
    private final FormShareRepository formShareRepository;

    public FormService(FeedbackFormRepository formRepository,
                       FeedbackQuestionRepository questionRepository,
                       FeedbackResponseRepository responseRepository,
                       FeedbackAnswerRepository answerRepository,
                       FormShareRepository formShareRepository) {
        this.formRepository = formRepository;
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
        this.formShareRepository = formShareRepository;
    }

    public FeedbackForm createFormFromTemplate(String title, String speakerName, LocalDate eventDate, String location, String ownerEmail) {
        var form = formRepository.save(new FeedbackForm(title, speakerName, eventDate, location, ownerEmail));

        var templateQuestions = List.of(
                new TemplateQuestion("Inhalt des Vortrags", QuestionType.RATING),
                new TemplateQuestion("Präsentation und Aufbau", QuestionType.RATING),
                new TemplateQuestion("Fachkompetenz des Referenten", QuestionType.RATING),
                new TemplateQuestion("Verständlichkeit", QuestionType.RATING),
                new TemplateQuestion("Praxisrelevanz", QuestionType.RATING),
                new TemplateQuestion("Aktualität des Themas", QuestionType.RATING),
                new TemplateQuestion("Tempo des Vortrags", QuestionType.RATING),
                new TemplateQuestion("Interaktivität", QuestionType.RATING),
                new TemplateQuestion("Gesamteindruck", QuestionType.RATING),
                new TemplateQuestion("Was hat Ihnen besonders gut gefallen?", QuestionType.TEXT),
                new TemplateQuestion("Was könnte verbessert werden?", QuestionType.TEXT),
                new TemplateQuestion("Weitere Anmerkungen oder Vorschläge?", QuestionType.TEXT),
                new TemplateQuestion("Welche Themen wünschen Sie sich für zukünftige Veranstaltungen?", QuestionType.TEXT)
        );

        for (var i = 0; i < templateQuestions.size(); i++) {
            var tq = templateQuestions.get(i);
            questionRepository.save(new FeedbackQuestion(null, form.id(), tq.text(), tq.type(), i + 1));
        }

        return formRepository.findById(form.id()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<FeedbackForm> getFormsForUser(String email) {
        return formRepository.findAllAccessibleByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<FeedbackForm> getFormById(Long id) {
        return formRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<FeedbackForm> getFormByPublicToken(String token) {
        return formRepository.findByPublicToken(token);
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(Long formId, String email) {
        return formRepository.findById(formId)
                .map(form -> email.equals(form.ownerEmail())
                        || formShareRepository.existsByFormIdAndSharedWithEmail(formId, email))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long formId, String email) {
        return formRepository.findById(formId)
                .map(form -> email.equals(form.ownerEmail()))
                .orElse(false);
    }

    public FeedbackForm saveForm(FeedbackForm form) {
        return formRepository.save(form);
    }

    public void deleteForm(Long id) {
        formRepository.deleteById(id);
    }

    public void publishForm(Long id) {
        formRepository.findById(id).ifPresent(form ->
                formRepository.save(form.withStatus(FormStatus.PUBLIC)));
    }

    public void closeForm(Long id) {
        formRepository.findById(id).ifPresent(form ->
                formRepository.save(form.withStatus(FormStatus.CLOSED)));
    }

    public void reopenForm(Long id) {
        formRepository.findById(id).ifPresent(form ->
                formRepository.save(form.withStatus(FormStatus.PUBLIC)));
    }

    public void shareForm(Long formId, String email) {
        if (formShareRepository.existsByFormIdAndSharedWithEmail(formId, email)) {
            return;
        }
        formShareRepository.save(new FormShare(null, formId, email));
    }

    public void unshareForm(Long formId, String email) {
        formShareRepository.deleteByFormIdAndSharedWithEmail(formId, email);
    }

    @Transactional(readOnly = true)
    public List<FormShare> getShares(Long formId) {
        return formShareRepository.findByFormId(formId);
    }

    public FeedbackResponse submitResponse(Long formId, List<FeedbackAnswer> answers) {
        var response = responseRepository.save(new FeedbackResponse(null, formId, LocalDateTime.now()));

        for (FeedbackAnswer answer : answers) {
            answerRepository.save(answer.withResponseId(response.id()));
        }

        return response;
    }

    @Transactional(readOnly = true)
    public long getResponseCount(Long formId) {
        return responseRepository.countByFormId(formId);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long questionId) {
        return answerRepository.findAverageRatingByQuestionId(questionId);
    }

    @Transactional(readOnly = true)
    public List<FeedbackAnswer> getTextAnswers(Long questionId) {
        return answerRepository.findByQuestionIdAndTextValueIsNotNull(questionId);
    }

    private record TemplateQuestion(String text, QuestionType type) {
    }
}
