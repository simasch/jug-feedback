package ch.martinelli.jug.feedback.service;

import ch.martinelli.jug.feedback.entity.*;
import ch.martinelli.jug.feedback.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FormService {

    private final FeedbackFormRepository formRepository;
    private final FeedbackQuestionRepository questionRepository;
    private final FeedbackResponseRepository responseRepository;
    private final FeedbackAnswerRepository answerRepository;

    public FormService(FeedbackFormRepository formRepository,
                       FeedbackQuestionRepository questionRepository,
                       FeedbackResponseRepository responseRepository,
                       FeedbackAnswerRepository answerRepository) {
        this.formRepository = formRepository;
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
    }

    public FeedbackForm createFormFromTemplate(String title, String speakerName, String topic) {
        FeedbackForm form = new FeedbackForm();
        form.setTitle(title);
        form.setSpeakerName(speakerName);
        form.setTopic(topic);
        form = formRepository.save(form);

        String[][] templateQuestions = {
            {"Inhalt des Vortrags", "RATING"},
            {"Präsentation und Aufbau", "RATING"},
            {"Fachkompetenz des Referenten", "RATING"},
            {"Verständlichkeit", "RATING"},
            {"Praxisrelevanz", "RATING"},
            {"Aktualität des Themas", "RATING"},
            {"Tempo des Vortrags", "RATING"},
            {"Interaktivität", "RATING"},
            {"Gesamteindruck", "RATING"},
            {"Was hat Ihnen besonders gut gefallen?", "TEXT"},
            {"Was könnte verbessert werden?", "TEXT"},
            {"Weitere Anmerkungen oder Vorschläge?", "TEXT"},
            {"Welche Themen wünschen Sie sich für zukünftige Veranstaltungen?", "TEXT"}
        };

        for (int i = 0; i < templateQuestions.length; i++) {
            FeedbackQuestion question = new FeedbackQuestion();
            question.setForm(form);
            question.setQuestionText(templateQuestions[i][0]);
            question.setQuestionType(QuestionType.valueOf(templateQuestions[i][1]));
            question.setOrderIndex(i + 1);
            questionRepository.save(question);
        }

        return formRepository.findById(form.getId()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<FeedbackForm> getAllForms() {
        return formRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<FeedbackForm> getFormById(Long id) {
        return formRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<FeedbackForm> getFormByPublicToken(String token) {
        return formRepository.findByPublicToken(token);
    }

    public FeedbackForm saveForm(FeedbackForm form) {
        return formRepository.save(form);
    }

    public void deleteForm(Long id) {
        formRepository.deleteById(id);
    }

    public void publishForm(Long id) {
        formRepository.findById(id).ifPresent(form -> {
            form.setStatus(FormStatus.PUBLIC);
            formRepository.save(form);
        });
    }

    public void closeForm(Long id) {
        formRepository.findById(id).ifPresent(form -> {
            form.setStatus(FormStatus.CLOSED);
            formRepository.save(form);
        });
    }

    public void reopenForm(Long id) {
        formRepository.findById(id).ifPresent(form -> {
            form.setStatus(FormStatus.PUBLIC);
            formRepository.save(form);
        });
    }

    public FeedbackResponse submitResponse(Long formId, List<FeedbackAnswer> answers) {
        FeedbackForm form = formRepository.findById(formId).orElseThrow();
        FeedbackResponse response = new FeedbackResponse();
        response.setForm(form);
        response = responseRepository.save(response);

        for (FeedbackAnswer answer : answers) {
            answer.setResponse(response);
            answerRepository.save(answer);
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
}
