package ch.martinelli.jug.feedback.views;

import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.entity.FeedbackQuestion;
import ch.martinelli.jug.feedback.entity.QuestionType;
import ch.martinelli.jug.feedback.service.FormService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;

@Route("editor")
@PageTitle("Form Editor - JUG Feedback")
@PermitAll
public class FormEditorView extends VerticalLayout implements HasUrlParameter<Long> {

    private final FormService formService;
    private FeedbackForm currentForm;
    private Grid<FeedbackQuestion> questionGrid;
    private TextField titleField;
    private TextField speakerField;
    private TextField topicField;

    public FormEditorView(FormService formService) {
        this.formService = formService;
        setSizeFull();
        setPadding(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long formId) {
        formService.getFormById(formId).ifPresent(form -> {
            currentForm = form;
            buildView();
        });
    }

    private void buildView() {
        removeAll();

        Button backButton = new Button("← Back to Dashboard",
            e -> UI.getCurrent().navigate(DashboardView.class));

        H2 title = new H2("Edit Form: " + currentForm.getTitle());

        titleField = new TextField("Form Title");
        titleField.setValue(currentForm.getTitle() != null ? currentForm.getTitle() : "");
        titleField.setWidth("300px");

        speakerField = new TextField("Speaker Name");
        speakerField.setValue(currentForm.getSpeakerName() != null ? currentForm.getSpeakerName() : "");
        speakerField.setWidth("300px");

        topicField = new TextField("Topic");
        topicField.setValue(currentForm.getTopic() != null ? currentForm.getTopic() : "");
        topicField.setWidth("300px");

        Button saveButton = new Button("Save Form Details", e -> saveFormDetails());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout formFields = new HorizontalLayout(titleField, speakerField, topicField, saveButton);
        formFields.setAlignItems(Alignment.BASELINE);
        formFields.setWidthFull();

        questionGrid = new Grid<>(FeedbackQuestion.class, false);
        questionGrid.addColumn(FeedbackQuestion::getOrderIndex).setHeader("#").setWidth("60px");
        questionGrid.addColumn(FeedbackQuestion::getQuestionText).setHeader("Question").setAutoWidth(true);
        questionGrid.addColumn(q -> q.getQuestionType().name()).setHeader("Type").setWidth("100px");
        questionGrid.setItems(currentForm.getQuestions());
        questionGrid.setHeight("400px");

        TextField newQuestionText = new TextField("New Question");
        newQuestionText.setWidth("400px");

        ComboBox<QuestionType> newQuestionType = new ComboBox<>("Type");
        newQuestionType.setItems(QuestionType.values());
        newQuestionType.setValue(QuestionType.RATING);

        Button addQuestionBtn = new Button("Add Question", e -> {
            if (!newQuestionText.getValue().trim().isEmpty()) {
                FeedbackQuestion q = new FeedbackQuestion();
                q.setForm(currentForm);
                q.setQuestionText(newQuestionText.getValue().trim());
                q.setQuestionType(newQuestionType.getValue());
                q.setOrderIndex(currentForm.getQuestions().size() + 1);
                currentForm.getQuestions().add(q);
                formService.saveForm(currentForm);
                currentForm = formService.getFormById(currentForm.getId()).orElseThrow();
                questionGrid.setItems(currentForm.getQuestions());
                newQuestionText.clear();
            }
        });
        addQuestionBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout addQuestionLayout = new HorizontalLayout(newQuestionText, newQuestionType, addQuestionBtn);
        addQuestionLayout.setAlignItems(Alignment.BASELINE);

        add(backButton, title, formFields, questionGrid, addQuestionLayout);
    }

    private void saveFormDetails() {
        currentForm.setTitle(titleField.getValue().trim());
        currentForm.setSpeakerName(speakerField.getValue().trim());
        currentForm.setTopic(topicField.getValue().trim());
        formService.saveForm(currentForm);
        Notification.show("Form saved", 2000, Notification.Position.BOTTOM_START);
    }
}
