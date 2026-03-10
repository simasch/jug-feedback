package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.ui.DashboardView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class UC04PublishFormTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc04-publish@example.com";

    @Autowired
    private FormService formService;

    private Long formId;

    @BeforeEach
    void createDraftForm() {
        var form = formService.createForm("Publish Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.id();
        login(OWNER_EMAIL, List.of("USER"));
    }

    @SuppressWarnings("unchecked")
    private HorizontalLayout getActionButtons(int rowIndex) {
        Grid<FeedbackForm> grid = _get(Grid.class);
        return (HorizontalLayout) _getCellComponent(grid, rowIndex, "actions");
    }

    private Button findActionButton(HorizontalLayout actions, String text) {
        return actions.getChildren()
                .filter(c -> c instanceof Button btn && text.equals(btn.getText()))
                .map(c -> (Button) c)
                .findFirst()
                .orElse(null);
    }

    @Test
    @UseCase(id = "UC-04")
    void draft_form_shows_publish_button() {
        UI.getCurrent().navigate(DashboardView.class);

        var actions = getActionButtons(0);
        assertThat(findActionButton(actions, "Publish")).isNotNull();
        assertThat(findActionButton(actions, "Edit")).isNotNull();
    }

    @Test
    @UseCase(id = "UC-04")
    void publish_changes_status_to_public() {
        UI.getCurrent().navigate(DashboardView.class);

        var actions = getActionButtons(0);
        var publishButton = findActionButton(actions, "Publish");
        _click(publishButton);

        var updatedForm = formService.getFormById(formId).orElseThrow();
        assertThat(updatedForm.status().name()).isEqualTo("PUBLIC");
    }

    @Test
    @UseCase(id = "UC-04", scenario = "Postcondition")
    void published_form_shows_close_button_instead_of_publish() {
        UI.getCurrent().navigate(DashboardView.class);

        var actions = getActionButtons(0);
        _click(findActionButton(actions, "Publish"));

        // After publishing, get the refreshed action buttons
        var updatedActions = getActionButtons(0);
        assertThat(findActionButton(updatedActions, "Close")).isNotNull();
        assertThat(findActionButton(updatedActions, "Publish")).isNull();
        assertThat(findActionButton(updatedActions, "Edit")).isNull();
    }
}
