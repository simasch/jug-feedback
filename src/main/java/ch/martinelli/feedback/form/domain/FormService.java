package ch.martinelli.feedback.form.domain;

import ch.martinelli.feedback.response.domain.FeedbackAnswer;
import ch.martinelli.feedback.response.domain.FeedbackAnswerRepository;
import ch.martinelli.feedback.response.domain.FeedbackResponse;
import ch.martinelli.feedback.response.domain.FeedbackResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FormService {

    private final FeedbackFormRepository formRepository;
    private final FeedbackResponseRepository responseRepository;
    private final FeedbackAnswerRepository answerRepository;
    private final FormShareRepository formShareRepository;
    private final FormTemplateRepository formTemplateRepository;

    public FormService(FeedbackFormRepository formRepository,
                       FeedbackResponseRepository responseRepository,
                       FeedbackAnswerRepository answerRepository,
                       FormShareRepository formShareRepository,
                       FormTemplateRepository formTemplateRepository) {
        this.formRepository = formRepository;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
        this.formShareRepository = formShareRepository;
        this.formTemplateRepository = formTemplateRepository;
    }

    @Transactional
    public FeedbackForm createForm(String title, String speakerName, LocalDate eventDate, String location, String ownerEmail) {
        return formRepository.save(new FeedbackForm(title, speakerName, eventDate, location, ownerEmail));
    }

    public List<FeedbackForm> getFormsForUser(String email) {
        return formRepository.findAllAccessibleByEmail(email);
    }

    public Optional<FeedbackForm> getFormById(Long id) {
        return formRepository.findById(id);
    }

    public Optional<FeedbackForm> getFormByPublicToken(String token) {
        return formRepository.findByPublicToken(token);
    }

    public boolean hasAccess(Long formId, String email) {
        return formRepository.findById(formId)
                .map(form -> email.equals(form.ownerEmail())
                        || formShareRepository.existsByFormIdAndSharedWithEmail(formId, email))
                .orElse(false);
    }

    public boolean isOwner(Long formId, String email) {
        return formRepository.findById(formId)
                .map(form -> email.equals(form.ownerEmail()))
                .orElse(false);
    }

    @Transactional
    public FeedbackForm saveForm(FeedbackForm form) {
        return formRepository.save(form);
    }

    @Transactional
    public void deleteForm(Long id) {
        formRepository.deleteById(id);
    }

    @Transactional
    public void publishForm(Long id) {
        formRepository.findById(id).ifPresent(form ->
                formRepository.save(form.withStatus(FormStatus.PUBLIC)));
    }

    @Transactional
    public void closeForm(Long id) {
        formRepository.findById(id).ifPresent(form ->
                formRepository.save(form.withStatus(FormStatus.CLOSED)));
    }

    @Transactional
    public void reopenForm(Long id) {
        formRepository.findById(id).ifPresent(form ->
                formRepository.save(form.withStatus(FormStatus.PUBLIC)));
    }

    @Transactional
    public void shareForm(Long formId, String email) {
        if (formShareRepository.existsByFormIdAndSharedWithEmail(formId, email)) {
            return;
        }
        formShareRepository.save(new FormShare(null, formId, email));
    }

    @Transactional
    public void unshareForm(Long formId, String email) {
        formShareRepository.deleteByFormIdAndSharedWithEmail(formId, email);
    }

    public List<FormShare> getShares(Long formId) {
        return formShareRepository.findByFormId(formId);
    }

    @Transactional
    public FeedbackResponse submitResponse(Long formId, List<FeedbackAnswer> answers) {
        var response = responseRepository.save(new FeedbackResponse(null, formId, LocalDateTime.now()));

        for (FeedbackAnswer answer : answers) {
            answerRepository.save(answer.withResponseId(response.id()));
        }

        return response;
    }

    public long getResponseCount(Long formId) {
        return responseRepository.countByFormId(formId);
    }

    public Double getAverageRating(Long questionId) {
        return answerRepository.findAverageRatingByQuestionId(questionId);
    }

    public Map<Integer, Long> getRatingDistribution(Long questionId) {
        return answerRepository.findRatingDistributionByQuestionId(questionId);
    }

    public List<FeedbackAnswer> getTextAnswers(Long questionId) {
        return answerRepository.findByQuestionIdAndTextValueIsNotNull(questionId);
    }

    public List<FormTemplate> getTemplatesForUser(String email) {
        return formTemplateRepository.findByOwnerEmailWithQuestions(email);
    }

    @Transactional
    public void renameTemplate(Long id, String name) {
        formTemplateRepository.updateName(id, name);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        formTemplateRepository.deleteById(id);
    }

    @Transactional
    public FeedbackForm createFormFromTemplate(FormTemplate template, String title, String speakerName,
                                                LocalDate eventDate, String location, String ownerEmail) {
        var form = formRepository.save(new FeedbackForm(title, speakerName, eventDate, location, ownerEmail));

        var questions = template.questions().stream()
                .map(tq -> new FeedbackQuestion(null, form.id(), tq.questionText(), tq.questionType(), tq.orderIndex()))
                .toList();

        return formRepository.save(form.withQuestions(questions));
    }

    @Transactional
    public FormTemplate saveFormAsTemplate(Long formId, String templateName) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formId));

        var templateQuestions = form.questions().stream()
                .map(q -> new TemplateQuestion(null, null, q.questionText(), q.questionType(), q.orderIndex()))
                .toList();

        var template = new FormTemplate(templateName, form.ownerEmail()).withQuestions(templateQuestions);
        return formTemplateRepository.save(template);
    }

}
