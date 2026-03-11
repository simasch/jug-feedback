package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FeedbackQuestion;
import ch.martinelli.feedback.form.domain.FeedbackQuestionRepository;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.QuestionType;
import ch.martinelli.feedback.form.ui.DashboardView;
import ch.martinelli.feedback.response.domain.FeedbackAnswer;
import ch.martinelli.feedback.response.ui.PublicFormView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class UC16UnpublishFormTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc16-unpublish@example.com";

    @Autowired
    private FormService formService;

    @Autowired
    private FeedbackQuestionRepository questionRepository;

    private Long formId;
    private String publicToken;

    @BeforeEach
    void createPublicForm() {
        var form = formService.createForm("Unpublish Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.id();
        publicToken = form.publicToken();
        questionRepository.save(new FeedbackQuestion(null, formId, "Rating", QuestionType.RATING, 1));
        formService.publishForm(formId);
        login(OWNER_EMAIL, List.of("USER"));
    }

    @SuppressWarnings("unchecked")
    private Button findActionButton(String text) {
        Grid<FeedbackForm> grid = _get(Grid.class);
        var actions = _getCellComponent(grid, 0, "actions");
        return findButtonInComponent(actions, text);
    }

    private Button findButtonInComponent(Component component, String text) {
        if (component instanceof Button btn && text.equals(btn.getText())) {
            return btn;
        }
        return component.getChildren()
                .map(child -> findButtonInComponent(child, text))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Test
    @UseCase(id = "UC-16")
    void public_form_without_responses_shows_unpublish_button() {
        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButton("Unpublish")).isNotNull();
    }

    @Test
    @UseCase(id = "UC-16")
    void unpublish_changes_status_to_draft() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Unpublish"));

        var updatedForm = formService.getFormById(formId).orElseThrow();
        assertThat(updatedForm.status().name()).isEqualTo("DRAFT");
    }

    @Test
    @UseCase(id = "UC-16", scenario = "Postcondition")
    void unpublished_form_shows_edit_and_publish_buttons() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Unpublish"));

        assertThat(findActionButton("Edit")).isNotNull();
        assertThat(findActionButton("Publish")).isNotNull();
        assertThat(findActionButton("Unpublish")).isNull();
        assertThat(findActionButton("Close")).isNull();
    }

    @Test
    @UseCase(id = "UC-16", scenario = "A1", businessRules = "BR-035")
    void public_form_with_responses_hides_unpublish_button() {
        // Submit a response to the form
        var form = formService.getFormById(formId).orElseThrow();
        var qId = form.questions().getFirst().id();
        formService.submitResponse(formId, List.of(
                new FeedbackAnswer(null, null, qId, 4, null)));

        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButton("Unpublish")).isNull();
        assertThat(findActionButton("Close")).isNotNull();
    }

    @Test
    @UseCase(id = "UC-16", scenario = "Postcondition")
    void unpublished_form_is_not_accessible_via_public_link() {
        // Unpublish the form
        formService.unpublishForm(formId);

        // Try to access via public link (anonymous)
        logout();
        UI.getCurrent().navigate(PublicFormView.class, publicToken);

        assertThat(_get(H2.class, spec -> spec.withText("Form not available")).isVisible()).isTrue();
    }
}
