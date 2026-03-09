package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.service.FormService;
import ch.martinelli.jug.feedback.views.DashboardView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

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
        var form = formService.createFormFromTemplate("Share Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.getId();
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
                .orElseThrow(() -> new AssertionError("Button '" + text + "' not found"));
    }

    @Test
    void share_button_opens_dialog() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Share"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(_get(EmailField.class, spec -> spec.withLabel("Share with email")).isVisible()).isTrue();
    }

    @Test
    void share_with_valid_email() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Share"));

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Share with email")), SHARED_EMAIL);
        _click(_get(Button.class, spec -> spec.withText("Share")));

        expectNotifications("Form shared with " + SHARED_EMAIL);

        var shares = formService.getShares(formId);
        assertThat(shares).hasSize(1);
        assertThat(shares.get(0).getSharedWithEmail()).isEqualTo(SHARED_EMAIL);
    }

    @Test
    void share_with_self_shows_error() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Share"));

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Share with email")), OWNER_EMAIL);
        _click(_get(Button.class, spec -> spec.withText("Share")));

        expectNotifications("You cannot share with yourself");
    }

    @Test
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
        assertThat(items.get(0).getTitle()).isEqualTo("Share Test");
    }

    @Test
    void duplicate_share_is_prevented() {
        formService.shareForm(formId, SHARED_EMAIL);

        // Share again with same email
        formService.shareForm(formId, SHARED_EMAIL);

        var shares = formService.getShares(formId);
        assertThat(shares).hasSize(1);
    }

    @Test
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
