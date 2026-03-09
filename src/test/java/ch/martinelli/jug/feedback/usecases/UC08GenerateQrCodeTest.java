package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.service.FormService;
import ch.martinelli.jug.feedback.views.DashboardView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

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
        var form = formService.createFormFromTemplate("QR Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.getId();
        publicToken = form.getPublicToken();
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
    void qr_code_button_opens_dialog_with_image_and_url() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("QR Code"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();

        // QR code image should be displayed
        assertThat(_get(Image.class).isVisible()).isTrue();

        // URL should contain the public token
        var urlSpan = _find(Span.class).stream()
                .filter(s -> s.getText().contains(publicToken))
                .findFirst();
        assertThat(urlSpan).isPresent();

        // Copy URL button should be available
        assertThat(_get(Button.class, spec -> spec.withText("Copy URL")).isVisible()).isTrue();
    }

    @Test
    void qr_code_available_for_draft_form() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);

        // Form is DRAFT by default - QR Code button should still be available
        _click(findActionButton("QR Code"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
    }

    @Test
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
