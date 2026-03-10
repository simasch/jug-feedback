package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FeedbackQuestion;
import ch.martinelli.feedback.form.domain.FeedbackQuestionRepository;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.QuestionType;
import ch.martinelli.feedback.response.domain.FeedbackAnswer;
import ch.martinelli.feedback.response.ui.ResultsView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class UC06ViewResultsTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc06-results@example.com";
    private static final String SHARED_EMAIL = "uc06-shared@example.com";
    private static final String OTHER_EMAIL = "uc06-other@example.com";

    @Autowired
    private FormService formService;

    @Autowired
    private FeedbackQuestionRepository questionRepository;

    private Long formId;

    @BeforeEach
    void createFormWithResponses() {
        var form = formService.createForm("Results Test", "Test Speaker", LocalDate.of(2026, 3, 15), "Zurich", OWNER_EMAIL);
        formId = form.id();

        // Add questions: 2 RATING + 1 TEXT
        questionRepository.save(new FeedbackQuestion(null, formId, "Content quality", QuestionType.RATING, 1));
        questionRepository.save(new FeedbackQuestion(null, formId, "Speaker competence", QuestionType.RATING, 2));
        questionRepository.save(new FeedbackQuestion(null, formId, "Additional comments", QuestionType.TEXT, 3));

        formService.publishForm(formId);
    }

    @Test
    @UseCase(id = "UC-06", businessRules = "BR-011")
    void owner_can_view_results() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        assertThat(_get(H2.class, spec -> spec.withText("Results: Results Test")).isVisible()).isTrue();
    }

    @Test
    @UseCase(id = "UC-06", scenario = "A2")
    void no_responses_shows_message() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        assertThat(_get(Paragraph.class, spec -> spec.withText("Total responses: 0")).isVisible()).isTrue();
        assertThat(_get(Paragraph.class, spec -> spec.withText("No responses yet.")).isVisible()).isTrue();
    }

    @Test
    @UseCase(id = "UC-06", businessRules = "BR-012")
    void results_show_average_ratings() {
        var form = formService.getFormById(formId).orElseThrow();
        var answers = new ArrayList<FeedbackAnswer>();
        for (var question : form.questions()) {
            Integer ratingValue = null;
            String textValue = null;
            if (question.questionType().name().equals("RATING")) {
                ratingValue = 4;
            } else {
                textValue = "Great talk!";
            }
            answers.add(new FeedbackAnswer(null, null, question.id(), ratingValue, textValue));
        }
        formService.submitResponse(formId, answers);

        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        assertThat(_get(Paragraph.class, spec -> spec.withText("Total responses: 1")).isVisible()).isTrue();
        var avgParagraphs = _find(Paragraph.class, spec -> spec.withText("Average rating: 4.00 / 5 (1 ratings)"));
        assertThat(avgParagraphs).isNotEmpty();
    }

    @Test
    @UseCase(id = "UC-06", businessRules = "BR-013")
    void results_show_text_answers() {
        var form = formService.getFormById(formId).orElseThrow();
        var answers = new ArrayList<FeedbackAnswer>();
        for (var question : form.questions()) {
            String textValue = null;
            if (question.questionType().name().equals("TEXT")) {
                textValue = "Excellent content!";
            }
            answers.add(new FeedbackAnswer(null, null, question.id(), null, textValue));
        }
        formService.submitResponse(formId, answers);

        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        var textParagraphs = _find(Paragraph.class).stream()
                .filter(p -> p.getText().contains("Excellent content!"))
                .toList();
        assertThat(textParagraphs).isNotEmpty();
    }

    @Test
    @UseCase(id = "UC-06", businessRules = "BR-014")
    void results_show_rating_distribution_chart() {
        var form = formService.getFormById(formId).orElseThrow();

        // Submit 3 responses with different ratings
        for (int rating : new int[]{5, 4, 4}) {
            var answers = new ArrayList<FeedbackAnswer>();
            for (var question : form.questions()) {
                Integer ratingValue = null;
                String textValue = null;
                if (question.questionType() == QuestionType.RATING) {
                    ratingValue = rating;
                } else {
                    textValue = "Comment " + rating;
                }
                answers.add(new FeedbackAnswer(null, null, question.id(), ratingValue, textValue));
            }
            formService.submitResponse(formId, answers);
        }

        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        // Verify the rating distribution chart container exists
        var chartDivs = _find(Div.class, spec -> spec.withClasses("rating-distribution"));
        assertThat(chartDivs).hasSize(2); // 2 rating questions

        // Verify rating labels (5, 4, 3, 2, 1) are present
        var spans = _find(Span.class);
        var ratingLabels = spans.stream()
                .filter(s -> s.getText().matches("[1-5]"))
                .toList();
        // 2 questions x 5 ratings = 10 labels
        assertThat(ratingLabels).hasSize(10);

        // Verify count labels are present (e.g., "2 (67%)" for rating 4)
        var countLabels = spans.stream()
                .filter(s -> s.getText().matches("\\d+ \\(\\d+%\\)"))
                .toList();
        assertThat(countLabels).isNotEmpty();

        // Verify average shows total count
        var avgParagraphs = _find(Paragraph.class, spec -> spec.withText("Average rating: 4.33 / 5 (3 ratings)"));
        assertThat(avgParagraphs).isNotEmpty();
    }

    @Test
    @UseCase(id = "UC-06")
    void shared_user_can_view_results() {
        formService.shareForm(formId, SHARED_EMAIL);

        login(SHARED_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        assertThat(_get(H2.class, spec -> spec.withText("Results: Results Test")).isVisible()).isTrue();
    }

    @Test
    @UseCase(id = "UC-06", scenario = "A1")
    void non_authorized_user_is_redirected() {
        login(OTHER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-06")
    void back_button_navigates_to_dashboard() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        _click(_get(Button.class, spec -> spec.withText("Back to Dashboard")));

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }
}
