package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.service.FormService;
import ch.martinelli.jug.feedback.views.DashboardView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.GridKt._size;
import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.assertj.core.api.Assertions.assertThat;

class UC02CreateFormTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc02-create@example.com";

    @Autowired
    private FormService formService;

    @BeforeEach
    void loginAndNavigate() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(DashboardView.class);
    }

    @Test
    void create_new_form_button_opens_dialog() {
        _click(_get(Button.class, spec -> spec.withText("Create New Form")));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(_get(TextField.class, spec -> spec.withLabel("Form Title")).isVisible()).isTrue();
        assertThat(_get(TextField.class, spec -> spec.withLabel("Speaker Name")).isVisible()).isTrue();
        assertThat(_get(DatePicker.class, spec -> spec.withLabel("Date")).isVisible()).isTrue();
        assertThat(_get(TextField.class, spec -> spec.withLabel("Location")).isVisible()).isTrue();
    }

    @Test
    void create_form_with_title_creates_draft_form() {
        _click(_get(Button.class, spec -> spec.withText("Create New Form")));

        _setValue(_get(TextField.class, spec -> spec.withLabel("Form Title")), "Test Presentation");
        _setValue(_get(TextField.class, spec -> spec.withLabel("Speaker Name")), "John Doe");
        _setValue(_get(DatePicker.class, spec -> spec.withLabel("Date")), LocalDate.of(2026, 3, 15));
        _setValue(_get(TextField.class, spec -> spec.withLabel("Location")), "Zurich");

        _click(_get(Button.class, spec -> spec.withText("Create")));

        expectNotifications("Form created successfully");

        // Verify form appears in grid
        @SuppressWarnings("unchecked")
        var grid = _get(Grid.class);
        assertThat(_size(grid)).isGreaterThan(0);

        // Verify form was created with correct data
        var forms = formService.getFormsForUser(OWNER_EMAIL);
        var createdForm = forms.stream()
                .filter(f -> "Test Presentation".equals(f.getTitle()))
                .findFirst()
                .orElseThrow();
        assertThat(createdForm.getStatus().name()).isEqualTo("DRAFT");
        assertThat(createdForm.getSpeakerName()).isEqualTo("John Doe");
        assertThat(createdForm.getQuestions()).hasSize(13);
    }

    @Test
    void create_form_without_title_shows_validation_error() {
        _click(_get(Button.class, spec -> spec.withText("Create New Form")));

        // Leave title empty, click Create
        _click(_get(Button.class, spec -> spec.withText("Create")));

        // Dialog should still be open (title is required)
        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(_get(TextField.class, spec -> spec.withLabel("Form Title")).isInvalid()).isTrue();
    }

    @Test
    void created_form_has_13_template_questions() {
        formService.createFormFromTemplate("Template Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);

        var forms = formService.getFormsForUser(OWNER_EMAIL);
        var form = forms.stream()
                .filter(f -> "Template Test".equals(f.getTitle()))
                .findFirst()
                .orElseThrow();

        assertThat(form.getQuestions()).hasSize(13);
        var ratingCount = form.getQuestions().stream()
                .filter(q -> q.getQuestionType().name().equals("RATING"))
                .count();
        var textCount = form.getQuestions().stream()
                .filter(q -> q.getQuestionType().name().equals("TEXT"))
                .count();
        assertThat(ratingCount).isEqualTo(9);
        assertThat(textCount).isEqualTo(4);
    }

    @Test
    void cancel_dialog_does_not_create_form() {
        long countBefore = formService.getFormsForUser(OWNER_EMAIL).size();

        _click(_get(Button.class, spec -> spec.withText("Create New Form")));
        _click(_get(Button.class, spec -> spec.withText("Cancel")));

        long countAfter = formService.getFormsForUser(OWNER_EMAIL).size();
        assertThat(countAfter).isEqualTo(countBefore);
    }
}
