package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.ui.DashboardView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
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
    private Component getActionButtons(int rowIndex) {
        Grid<FeedbackForm> grid = _get(Grid.class);
        return _getCellComponent(grid, rowIndex, "actions");
    }

    private Button findActionButton(Component actions, String text) {
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
    @UseCase(id = "UC-04", scenario = "Precondition")
    void non_owner_does_not_see_publish_button() {
        var sharedEmail = "uc04-shared@example.com";
        formService.shareForm(formId, sharedEmail);

        logout();
        login(sharedEmail, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        var actions = getActionButtons(0);
        assertThat(findActionButton(actions, "Publish")).isNull();
    }

    @Test
    @UseCase(id = "UC-04", scenario = "Precondition")
    void public_form_does_not_show_publish_button() {
        formService.publishForm(formId);

        UI.getCurrent().navigate(DashboardView.class);

        var actions = getActionButtons(0);
        assertThat(findActionButton(actions, "Publish")).isNull();
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
