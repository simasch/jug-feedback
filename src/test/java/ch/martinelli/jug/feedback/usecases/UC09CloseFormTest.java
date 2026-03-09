package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.service.FormService;
import ch.martinelli.jug.feedback.views.DashboardView;
import ch.martinelli.jug.feedback.views.PublicFormView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class UC09CloseFormTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc09-close@example.com";

    @Autowired
    private FormService formService;

    private Long formId;
    private String publicToken;

    @BeforeEach
    void createPublicForm() {
        var form = formService.createFormFromTemplate("Close Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.getId();
        publicToken = form.getPublicToken();
        formService.publishForm(formId);
        login(OWNER_EMAIL, List.of("USER"));
    }

    @SuppressWarnings("unchecked")
    private Button findActionButton(String text) {
        Grid<FeedbackForm> grid = _get(Grid.class);
        var actions = (HorizontalLayout) _getCellComponent(grid, 0, "actions");
        return actions.getChildren()
                .filter(c -> c instanceof Button btn && text.equals(btn.getText()))
                .map(c -> (Button) c)
                .findFirst()
                .orElse(null);
    }

    @Test
    void public_form_shows_close_button() {
        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButton("Close")).isNotNull();
    }

    @Test
    void close_changes_status_to_closed() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Close"));

        var updatedForm = formService.getFormById(formId).orElseThrow();
        assertThat(updatedForm.getStatus().name()).isEqualTo("CLOSED");
    }

    @Test
    void closed_form_shows_reopen_and_delete_buttons() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Close"));

        // After closing, Reopen and Delete buttons should appear
        assertThat(findActionButton("Reopen")).isNotNull();
        assertThat(findActionButton("Delete")).isNotNull();
        assertThat(findActionButton("Close")).isNull();
    }

    @Test
    void closed_form_rejects_feedback() {
        formService.closeForm(formId);

        // Try to access the public form
        UI.getCurrent().navigate(PublicFormView.class, publicToken);

        assertThat(_get(H2.class, spec -> spec.withText("Form not available")).isVisible()).isTrue();
    }
}
