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
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class UC08GenerateQrCodeTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc08-qr@example.com";
    private static final String SHARED_EMAIL = "uc08-shared@example.com";

    @Autowired
    private FormService formService;

    private Long formId;
    private String publicToken;

    @BeforeEach
    void createForm() {
        var form = formService.createForm("QR Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.id();
        publicToken = form.publicToken();
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
    @UseCase(id = "UC-08", businessRules = {"BR-018", "BR-019"})
    void qr_code_button_opens_dialog_with_image_and_url() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("QR Code"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();

        // QR code image should be displayed
        assertThat(_get(Image.class).isVisible()).isTrue();

        // URL should be a clickable link containing the public token
        var urlAnchor = _find(Anchor.class).stream()
                .filter(a -> a.getHref().contains(publicToken))
                .findFirst();
        assertThat(urlAnchor).isPresent();

        // Copy URL button should be available
        assertThat(_get(Button.class, spec -> spec.withText("Copy URL")).isVisible()).isTrue();
    }

    @Test
    @UseCase(id = "UC-08", businessRules = "BR-020")
    void qr_code_available_for_draft_form() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        // Form is DRAFT by default - QR Code button should still be available
        _click(findActionButton("QR Code"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
    }

    @Test
    @UseCase(id = "UC-08", businessRules = "BR-020")
    void qr_code_available_for_public_form() {
        formService.publishForm(formId);

        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("QR Code"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(_get(Image.class).isVisible()).isTrue();
    }

    @Test
    @UseCase(id = "UC-08", businessRules = "BR-020")
    void qr_code_available_for_closed_form() {
        formService.publishForm(formId);
        formService.closeForm(formId);

        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("QR Code"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(_get(Image.class).isVisible()).isTrue();
    }

    @Test
    @UseCase(id = "UC-08")
    void qr_code_available_for_shared_user() {
        formService.shareForm(formId, SHARED_EMAIL);

        login(SHARED_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("QR Code"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(_get(Image.class).isVisible()).isTrue();
    }
}
