package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.ui.DashboardView;
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

class UC10ReopenFormTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc10-reopen@example.com";

    @Autowired
    private FormService formService;

    private Long formId;
    private String publicToken;

    @BeforeEach
    void createClosedForm() {
        var form = formService.createForm("Reopen Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.id();
        publicToken = form.publicToken();
        formService.publishForm(formId);
        formService.closeForm(formId);
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
    @UseCase(id = "UC-10")
    void closed_form_shows_reopen_button() {
        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButton("Reopen")).isNotNull();
    }

    @Test
    @UseCase(id = "UC-10")
    void reopen_changes_status_to_public() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Reopen"));

        var updatedForm = formService.getFormById(formId).orElseThrow();
        assertThat(updatedForm.status().name()).isEqualTo("PUBLIC");
    }

    @Test
    @UseCase(id = "UC-10", scenario = "Postcondition")
    void reopened_form_shows_close_button() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Reopen"));

        assertThat(findActionButton("Close")).isNotNull();
        assertThat(findActionButton("Reopen")).isNull();
    }

    @Test
    @UseCase(id = "UC-10", scenario = "Postcondition")
    void reopened_form_accepts_feedback() {
        formService.reopenForm(formId);

        UI.getCurrent().navigate(PublicFormView.class, publicToken);

        // Form should be accessible, showing the form title
        assertThat(_get(H2.class, spec -> spec.withText("Reopen Test")).isVisible()).isTrue();
        assertThat(_get(Button.class, spec -> spec.withText("Submit Feedback")).isVisible()).isTrue();
    }
}
