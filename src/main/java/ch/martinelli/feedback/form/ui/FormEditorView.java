package ch.martinelli.feedback.form.ui;

import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FeedbackQuestion;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.FormStatus;
import ch.martinelli.feedback.form.domain.QuestionType;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;

@Route("editor")
@PermitAll
public class FormEditorView extends VerticalLayout implements HasUrlParameter<Long>, HasDynamicTitle {

    private static final String FIELD_WIDTH = "300px";

    private final transient FormService formService;
    private transient FeedbackForm currentForm;
    private TextField titleField;
    private TextField speakerField;
    private DatePicker dateField;
    private TextField locationField;

    public FormEditorView(FormService formService) {
        this.formService = formService;
        setSizeFull();
        setPadding(true);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("editor.page-title");
    }

    @Override
    public void setParameter(BeforeEvent event, Long formId) {
        var email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!formService.isOwner(formId, email)) {
            event.forwardTo("");
            return;
        }
        var form = formService.getFormById(formId).orElse(null);
        if (form == null || form.status() != FormStatus.DRAFT) {
            event.forwardTo("");
            return;
        }
        currentForm = form;
        buildView();
    }

    private void buildView() {
        removeAll();

        var backButton = new Button(getTranslation("editor.back"),
            e -> UI.getCurrent().navigate(""));

        var title = new H2(getTranslation("editor.title", currentForm.title()));

        titleField = new TextField(getTranslation("editor.form-title"));
        titleField.setValue(currentForm.title() != null ? currentForm.title() : "");
        titleField.setWidth(FIELD_WIDTH);

        speakerField = new TextField(getTranslation("editor.speaker"));
        speakerField.setValue(currentForm.speakerName() != null ? currentForm.speakerName() : "");
        speakerField.setWidth(FIELD_WIDTH);

        dateField = new DatePicker(getTranslation("editor.date"));
        dateField.setValue(currentForm.eventDate());
        dateField.setWidth(FIELD_WIDTH);

        locationField = new TextField(getTranslation("editor.location"));
        locationField.setValue(currentForm.location() != null ? currentForm.location() : "");
        locationField.setWidth(FIELD_WIDTH);

        var saveButton = new Button(getTranslation("editor.save"), e -> saveFormDetails());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var formFields = new HorizontalLayout(titleField, speakerField, dateField, locationField, saveButton);
        formFields.setAlignItems(Alignment.BASELINE);
        formFields.setWidthFull();

        var questionGrid = new Grid<>(FeedbackQuestion.class, false);
        questionGrid.addColumn(FeedbackQuestion::orderIndex).setHeader(getTranslation("editor.column.order")).setWidth("60px");
        questionGrid.addColumn(FeedbackQuestion::questionText).setHeader(getTranslation("editor.column.question")).setAutoWidth(true);
        questionGrid.addColumn(q -> q.questionType().name()).setHeader(getTranslation("editor.column.type")).setWidth("100px");
        questionGrid.setItems(currentForm.questions());
        questionGrid.setHeight("400px");

        var newQuestionText = new TextField(getTranslation("editor.new-question"));
        newQuestionText.setWidth("400px");

        var newQuestionType = new ComboBox<QuestionType>(getTranslation("editor.new-question.type"));
        newQuestionType.setItems(QuestionType.values());
        newQuestionType.setValue(QuestionType.RATING);

        var addQuestionBtn = new Button(getTranslation("editor.add-question"), e -> {
            if (!newQuestionText.getValue().trim().isEmpty()) {
                var q = new FeedbackQuestion(null, currentForm.id(),
                        newQuestionText.getValue().trim(), newQuestionType.getValue(),
                        currentForm.questions().size() + 1);
                var updatedQuestions = new ArrayList<>(currentForm.questions());
                updatedQuestions.add(q);
                currentForm = currentForm.withQuestions(updatedQuestions);
                formService.saveForm(currentForm);
                currentForm = formService.getFormById(currentForm.id()).orElseThrow();
                questionGrid.setItems(currentForm.questions());
                newQuestionText.clear();
            }
        });
        addQuestionBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var addQuestionLayout = new HorizontalLayout(newQuestionText, newQuestionType, addQuestionBtn);
        addQuestionLayout.setAlignItems(Alignment.BASELINE);

        add(backButton, title, formFields, questionGrid, addQuestionLayout);
    }

    private void saveFormDetails() {
        currentForm = currentForm.withDetails(
                titleField.getValue().trim(),
                speakerField.getValue().trim(),
                dateField.getValue(),
                locationField.getValue().trim());
        formService.saveForm(currentForm);
        Notification.show(getTranslation("editor.saved"), 2000, Notification.Position.BOTTOM_START);
    }
}
