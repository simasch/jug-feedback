package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.entity.FeedbackAnswer;
import ch.martinelli.jug.feedback.service.FormService;
import ch.martinelli.jug.feedback.views.ResultsView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
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

    private Long formId;

    @BeforeEach
    void createFormWithResponses() {
        var form = formService.createFormFromTemplate("Results Test", "Test Speaker", LocalDate.of(2026, 3, 15), "Zurich", OWNER_EMAIL);
        formId = form.getId();
        formService.publishForm(formId);
    }

    @Test
    void owner_can_view_results() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        assertThat(_get(H2.class, spec -> spec.withText("Results: Results Test")).isVisible()).isTrue();
    }

    @Test
    void no_responses_shows_message() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        assertThat(_get(Paragraph.class, spec -> spec.withText("Total responses: 0")).isVisible()).isTrue();
        assertThat(_get(Paragraph.class, spec -> spec.withText("No responses yet.")).isVisible()).isTrue();
    }

    @Test
    void results_show_average_ratings() {
        // Submit some feedback
        var form = formService.getFormById(formId).orElseThrow();
        var answers = new ArrayList<FeedbackAnswer>();
        for (var question : form.getQuestions()) {
            var answer = new FeedbackAnswer();
            answer.setQuestion(question);
            if (question.getQuestionType().name().equals("RATING")) {
                answer.setRatingValue(4);
            } else {
                answer.setTextValue("Great talk!");
            }
            answers.add(answer);
        }
        formService.submitResponse(formId, answers);

        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        assertThat(_get(Paragraph.class, spec -> spec.withText("Total responses: 1")).isVisible()).isTrue();
        // Check average rating is displayed
        var avgParagraphs = _find(Paragraph.class, spec -> spec.withText("Average rating: 4.00 / 5"));
        assertThat(avgParagraphs).isNotEmpty();
    }

    @Test
    void results_show_text_answers() {
        var form = formService.getFormById(formId).orElseThrow();
        var answers = new ArrayList<FeedbackAnswer>();
        for (var question : form.getQuestions()) {
            var answer = new FeedbackAnswer();
            answer.setQuestion(question);
            if (question.getQuestionType().name().equals("TEXT")) {
                answer.setTextValue("Excellent content!");
            }
            answers.add(answer);
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
    void shared_user_can_view_results() {
        formService.shareForm(formId, SHARED_EMAIL);

        login(SHARED_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        assertThat(_get(H2.class, spec -> spec.withText("Results: Results Test")).isVisible()).isTrue();
    }

    @Test
    void non_authorized_user_is_redirected() {
        login(OTHER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        // Should be redirected to dashboard
        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }

    @Test
    void back_button_navigates_to_dashboard() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        _click(_get(Button.class, spec -> spec.withText("Back to Dashboard")));

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }
}
