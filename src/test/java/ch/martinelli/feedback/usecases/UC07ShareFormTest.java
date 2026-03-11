package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.ui.DashboardView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.EmailField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.assertj.core.api.Assertions.assertThat;

class UC07ShareFormTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc07-share@example.com";
    private static final String SHARED_EMAIL = "uc07-shared@example.com";

    @Autowired
    private FormService formService;

    private Long formId;

    @BeforeEach
    void createForm() {
        var form = formService.createForm("Share Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.id();
        login(OWNER_EMAIL, List.of("USER"));
    }

    @SuppressWarnings("unchecked")
    private Button findActionButton(String text) {
        Grid<FeedbackForm> grid = _get(Grid.class);
        var actions = _getCellComponent(grid, 0, "actions");
        var button = findButtonInComponent(actions, text);
        if (button == null) {
            throw new AssertionError("Button '" + text + "' not found");
        }
        return button;
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
    @UseCase(id = "UC-07", businessRules = "BR-014")
    void share_button_opens_dialog() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Share"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(_get(EmailField.class, spec -> spec.withLabel("Share with email")).isVisible()).isTrue();
    }

    @Test
    @UseCase(id = "UC-07", businessRules = "BR-016")
    void share_with_valid_email() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Share"));

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Share with email")), SHARED_EMAIL);
        _click(_get(Button.class, spec -> spec.withText("Share")));

        expectNotifications("Form shared with " + SHARED_EMAIL);

        var shares = formService.getShares(formId);
        assertThat(shares).hasSize(1);
        assertThat(shares.getFirst().sharedWithEmail()).isEqualTo(SHARED_EMAIL);
    }

    @Test
    @UseCase(id = "UC-07", scenario = "A2")
    void share_with_self_shows_error() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Share"));

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Share with email")), OWNER_EMAIL);
        _click(_get(Button.class, spec -> spec.withText("Share")));

        expectNotifications("You cannot share with yourself");
    }

    @Test
    @UseCase(id = "UC-07", scenario = "Postcondition")
    void shared_form_appears_in_other_users_dashboard() {
        formService.shareForm(formId, SHARED_EMAIL);

        // Login as shared user
        logout();
        login(SHARED_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        @SuppressWarnings("unchecked")
        Grid<FeedbackForm> grid = _get(Grid.class);
        var items = com.github.mvysny.kaributesting.v10.GridKt._findAll(grid);
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().title()).isEqualTo("Share Test");
    }

    @Test
    @UseCase(id = "UC-07", scenario = "A1", businessRules = "BR-016")
    void share_with_invalid_email_shows_error() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Share"));

        var emailField = _get(EmailField.class, spec -> spec.withLabel("Share with email"));
        _setValue(emailField, "not-an-email");

        _click(_get(Button.class, spec -> spec.withText("Share")));

        expectNotifications("Please enter a valid email");
    }

    @Test
    @UseCase(id = "UC-07", businessRules = "BR-014")
    void non_owner_does_not_see_share_button() {
        var otherEmail = "uc07-other@example.com";
        formService.shareForm(formId, otherEmail);

        logout();
        login(otherEmail, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        @SuppressWarnings("unchecked")
        Grid<FeedbackForm> grid = _get(Grid.class);
        var actions = com.github.mvysny.kaributesting.v10.GridKt._getCellComponent(grid, 0, "actions");
        var shareButton = findButtonInComponent(actions, "Share");
        assertThat(shareButton).isNull();
    }

    @Test
    @UseCase(id = "UC-07", businessRules = "BR-015")
    void shared_user_does_not_see_share_button() {
        formService.shareForm(formId, SHARED_EMAIL);

        logout();
        login(SHARED_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        @SuppressWarnings("unchecked")
        Grid<FeedbackForm> grid = _get(Grid.class);
        var actions = com.github.mvysny.kaributesting.v10.GridKt._getCellComponent(grid, 0, "actions");
        var shareButton = findButtonInComponent(actions, "Share");
        assertThat(shareButton).isNull();
    }

    @Test
    @UseCase(id = "UC-07", scenario = "A3", businessRules = "BR-017")
    void duplicate_share_is_prevented() {
        formService.shareForm(formId, SHARED_EMAIL);

        // Share again with same email
        formService.shareForm(formId, SHARED_EMAIL);

        var shares = formService.getShares(formId);
        assertThat(shares).hasSize(1);
    }

    @Test
    @UseCase(id = "UC-07", scenario = "A4")
    void remove_share() {
        formService.shareForm(formId, SHARED_EMAIL);
        assertThat(formService.getShares(formId)).hasSize(1);

        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Share"));

        // Click Remove button in the share dialog
        _click(_get(Button.class, spec -> spec.withText("Remove")));

        assertThat(formService.getShares(formId)).isEmpty();
    }
}
