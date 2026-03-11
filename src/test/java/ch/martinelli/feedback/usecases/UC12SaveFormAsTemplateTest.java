package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FeedbackQuestion;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.FormTemplateRepository;
import ch.martinelli.feedback.form.domain.QuestionType;
import ch.martinelli.feedback.form.ui.DashboardView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
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

class UC12SaveFormAsTemplateTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc12-template@example.com";

    @Autowired
    private FormService formService;

    @Autowired
    private FormTemplateRepository formTemplateRepository;

    private Long formId;

    @BeforeEach
    void createFormWithQuestions() {
        formTemplateRepository.deleteByOwnerEmail(OWNER_EMAIL);

        var form = formService.createForm("Template Test Form", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.id();

        var questions = List.of(
                new FeedbackQuestion(null, form.id(), "How was the talk?", QuestionType.RATING, 1),
                new FeedbackQuestion(null, form.id(), "Any comments?", QuestionType.TEXT, 2)
        );
        formService.saveForm(form.withQuestions(questions));

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
    @UseCase(id = "UC-12", businessRules = {"BR-024", "BR-026"})
    void save_form_as_template_with_default_name() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Save as Template"));

        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();

        // Template name should be pre-filled with form title
        var nameField = _get(TextField.class, spec -> spec.withLabel("Template Name"));
        assertThat(nameField.getValue()).isEqualTo("Template Test Form");

        // Save with default name
        _click(_get(Button.class, spec -> spec.withText("Save")));

        expectNotifications("Template saved successfully");

        // Verify template was created with questions
        var templates = formTemplateRepository.findByOwnerEmail(OWNER_EMAIL);
        assertThat(templates).hasSize(1);
        assertThat(templates.getFirst().name()).isEqualTo("Template Test Form");
    }

    @Test
    @UseCase(id = "UC-12", scenario = "Custom Name")
    void save_form_as_template_with_custom_name() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Save as Template"));

        var nameField = _get(TextField.class, spec -> spec.withLabel("Template Name"));
        _setValue(nameField, "My Custom Template");

        _click(_get(Button.class, spec -> spec.withText("Save")));

        expectNotifications("Template saved successfully");

        var templates = formTemplateRepository.findByOwnerEmail(OWNER_EMAIL);
        assertThat(templates).hasSize(1);
        assertThat(templates.getFirst().name()).isEqualTo("My Custom Template");
    }

    @Test
    @UseCase(id = "UC-12", scenario = "A1")
    void save_template_with_empty_name_shows_validation_error() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Save as Template"));

        var nameField = _get(TextField.class, spec -> spec.withLabel("Template Name"));
        _setValue(nameField, "");

        _click(_get(Button.class, spec -> spec.withText("Save")));

        // Dialog should still be open
        var dialog = _get(Dialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(nameField.isInvalid()).isTrue();

        // No template should be created
        var templates = formTemplateRepository.findByOwnerEmail(OWNER_EMAIL);
        assertThat(templates).isEmpty();
    }

    @Test
    @UseCase(id = "UC-12", scenario = "A2")
    void cancel_dialog_does_not_create_template() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Save as Template"));

        _click(_get(Button.class, spec -> spec.withText("Cancel")));

        var templates = formTemplateRepository.findByOwnerEmail(OWNER_EMAIL);
        assertThat(templates).isEmpty();
    }

    @Test
    @UseCase(id = "UC-12", businessRules = "BR-025")
    void save_template_from_public_form() {
        formService.publishForm(formId);

        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Save as Template"));

        _click(_get(Button.class, spec -> spec.withText("Save")));

        expectNotifications("Template saved successfully");

        var templates = formTemplateRepository.findByOwnerEmail(OWNER_EMAIL);
        assertThat(templates).hasSize(1);
    }

    @Test
    @UseCase(id = "UC-12", businessRules = "BR-025")
    void save_template_from_closed_form() {
        formService.publishForm(formId);
        formService.closeForm(formId);

        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButton("Save as Template"));

        _click(_get(Button.class, spec -> spec.withText("Save")));

        expectNotifications("Template saved successfully");

        var templates = formTemplateRepository.findByOwnerEmail(OWNER_EMAIL);
        assertThat(templates).hasSize(1);
    }
}
