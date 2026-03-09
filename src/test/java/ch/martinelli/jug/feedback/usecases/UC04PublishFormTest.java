package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.service.FormService;
import ch.martinelli.jug.feedback.views.DashboardView;
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
        var form = formService.createFormFromTemplate("Publish Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.getId();
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
    void draft_form_shows_publish_button() {
        UI.getCurrent().navigate(DashboardView.class);

        var actions = getActionButtons(0);
        assertThat(findActionButton(actions, "Publish")).isNotNull();
        assertThat(findActionButton(actions, "Edit")).isNotNull();
    }

    @Test
    void publish_changes_status_to_public() {
        UI.getCurrent().navigate(DashboardView.class);

        var actions = getActionButtons(0);
        var publishButton = findActionButton(actions, "Publish");
        _click(publishButton);

        var updatedForm = formService.getFormById(formId).orElseThrow();
        assertThat(updatedForm.getStatus().name()).isEqualTo("PUBLIC");
    }

    @Test
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
