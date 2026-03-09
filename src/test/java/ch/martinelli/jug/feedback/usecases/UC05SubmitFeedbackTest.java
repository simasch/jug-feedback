package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.entity.FormStatus;
import ch.martinelli.jug.feedback.service.FormService;
import ch.martinelli.jug.feedback.views.PublicFormView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class UC05SubmitFeedbackTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc05-feedback@example.com";

    @Autowired
    private FormService formService;

    private String publicToken;
    private Long formId;

    @BeforeEach
    void createPublicForm() {
        var form = formService.createFormFromTemplate("Feedback Test", "Test Speaker", LocalDate.of(2026, 3, 15), "Zurich", OWNER_EMAIL);
        formId = form.getId();
        publicToken = form.getPublicToken();
        formService.publishForm(form.getId());
    }

    @Test
    void public_form_displays_title_and_questions() {
        UI.getCurrent().navigate(PublicFormView.class, publicToken);

        assertThat(_get(H2.class, spec -> spec.withText("Feedback Test")).isVisible()).isTrue();

        // Should have 9 rating groups and 4 text areas
        var ratingGroups = _find(RadioButtonGroup.class);
        var textAreas = _find(TextArea.class);
        assertThat(ratingGroups).hasSize(9);
        assertThat(textAreas).hasSize(4);

        assertThat(_get(Button.class, spec -> spec.withText("Submit Feedback")).isVisible()).isTrue();
    }

    @Test
    void submit_feedback_shows_thank_you_page() {
        UI.getCurrent().navigate(PublicFormView.class, publicToken);

        // Fill in some ratings
        var ratingGroups = _find(RadioButtonGroup.class);
        for (var group : ratingGroups) {
            @SuppressWarnings("unchecked")
            RadioButtonGroup<Integer> rg = (RadioButtonGroup<Integer>) group;
            _setValue(rg, 4);
        }

        // Fill in a text answer
        var textAreas = _find(TextArea.class);
        _setValue(textAreas.get(0), "Great presentation!");

        _click(_get(Button.class, spec -> spec.withText("Submit Feedback")));

        // Should show thank you message
        assertThat(_get(H2.class, spec -> spec.withText("Thank you!")).isVisible()).isTrue();

        // Verify response was saved
        assertThat(formService.getResponseCount(formId)).isEqualTo(1);
    }

    @Test
    void form_not_found_shows_error() {
        UI.getCurrent().navigate(PublicFormView.class, "nonexistent-token");

        assertThat(_get(H2.class, spec -> spec.withText("Form not found")).isVisible()).isTrue();
    }

    @Test
    void closed_form_shows_not_available() {
        formService.closeForm(formId);

        UI.getCurrent().navigate(PublicFormView.class, publicToken);

        assertThat(_get(H2.class, spec -> spec.withText("Form not available")).isVisible()).isTrue();
    }

    @Test
    void draft_form_shows_not_available() {
        // Create a draft form (not published)
        var draftForm = formService.createFormFromTemplate("Draft Form", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);

        UI.getCurrent().navigate(PublicFormView.class, draftForm.getPublicToken());

        assertThat(_get(H2.class, spec -> spec.withText("Form not available")).isVisible()).isTrue();
    }

    @Test
    void multiple_submissions_create_separate_responses() {
        // First submission
        UI.getCurrent().navigate(PublicFormView.class, publicToken);
        _click(_get(Button.class, spec -> spec.withText("Submit Feedback")));

        // Second submission
        UI.getCurrent().navigate(PublicFormView.class, publicToken);
        _click(_get(Button.class, spec -> spec.withText("Submit Feedback")));

        assertThat(formService.getResponseCount(formId)).isEqualTo(2);
    }
}
