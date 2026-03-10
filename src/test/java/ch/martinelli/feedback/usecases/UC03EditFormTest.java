package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.QuestionType;
import ch.martinelli.feedback.form.ui.FormEditorView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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

class UC03EditFormTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc03-edit@example.com";
    private static final String OTHER_EMAIL = "uc03-other@example.com";

    @Autowired
    private FormService formService;

    private Long formId;

    @BeforeEach
    void createTestForm() {
        var form = formService.createForm("Original Title", "Original Speaker", LocalDate.of(2026, 1, 1), "Original Location", OWNER_EMAIL);
        formId = form.id();
    }

    @Test
    @UseCase(id = "UC-03")
    void owner_can_open_form_editor() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(FormEditorView.class, formId);

        assertThat(_get(TextField.class, spec -> spec.withLabel("Form Title")).getValue()).isEqualTo("Original Title");
        assertThat(_get(TextField.class, spec -> spec.withLabel("Speaker Name")).getValue()).isEqualTo("Original Speaker");
        assertThat(_get(TextField.class, spec -> spec.withLabel("Location")).getValue()).isEqualTo("Original Location");
    }

    @Test
    @UseCase(id = "UC-03")
    void owner_can_save_form_details() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(FormEditorView.class, formId);

        _setValue(_get(TextField.class, spec -> spec.withLabel("Form Title")), "Updated Title");
        _setValue(_get(TextField.class, spec -> spec.withLabel("Speaker Name")), "Updated Speaker");
        _setValue(_get(DatePicker.class, spec -> spec.withLabel("Date")), LocalDate.of(2026, 6, 15));
        _setValue(_get(TextField.class, spec -> spec.withLabel("Location")), "Updated Location");

        _click(_get(Button.class, spec -> spec.withText("Save Form Details")));

        expectNotifications("Form saved");

        var updatedForm = formService.getFormById(formId).orElseThrow();
        assertThat(updatedForm.title()).isEqualTo("Updated Title");
        assertThat(updatedForm.speakerName()).isEqualTo("Updated Speaker");
        assertThat(updatedForm.location()).isEqualTo("Updated Location");
    }

    @Test
    @UseCase(id = "UC-03")
    void editor_shows_empty_question_grid_for_new_form() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(FormEditorView.class, formId);

        @SuppressWarnings("unchecked")
        var grid = _get(Grid.class);
        assertThat(_size(grid)).isZero();
    }

    @Test
    @UseCase(id = "UC-03", scenario = "A2")
    @SuppressWarnings("unchecked")
    void owner_can_add_new_question() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(FormEditorView.class, formId);

        _setValue(_get(TextField.class, spec -> spec.withLabel("New Question")), "How was the food?");
        _setValue(_get(ComboBox.class, spec -> spec.withLabel("Type")), QuestionType.TEXT);
        _click(_get(Button.class, spec -> spec.withText("Add Question")));

        var grid = _get(Grid.class);
        assertThat(_size(grid)).isEqualTo(1);
    }

    @Test
    @UseCase(id = "UC-03", scenario = "A1")
    void non_owner_is_redirected_to_dashboard() {
        login(OTHER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(FormEditorView.class, formId);

        // Should be redirected to dashboard
        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-03", scenario = "A1")
    void shared_user_is_redirected_to_dashboard() {
        formService.shareForm(formId, OTHER_EMAIL);
        login(OTHER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(FormEditorView.class, formId);

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-03", scenario = "Precondition")
    void public_form_owner_is_redirected_to_dashboard() {
        formService.publishForm(formId);
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(FormEditorView.class, formId);

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-03", scenario = "Precondition")
    void closed_form_owner_is_redirected_to_dashboard() {
        formService.publishForm(formId);
        formService.closeForm(formId);
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(FormEditorView.class, formId);

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-03")
    void back_button_navigates_to_dashboard() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(FormEditorView.class, formId);

        _click(_get(Button.class, spec -> spec.withText("Back to Dashboard")));

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }
}
