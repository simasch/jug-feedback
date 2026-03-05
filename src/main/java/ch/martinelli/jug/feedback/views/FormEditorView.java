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
import org.springframework.security.core.context.SecurityContextHolder;

@Route("editor")
@PermitAll
public class FormEditorView extends VerticalLayout implements HasUrlParameter<Long>, HasDynamicTitle {

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
    public String getPageTitle() {
        return getTranslation("editor.page-title");
    }

    @Override
    public void setParameter(BeforeEvent event, Long formId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!formService.hasAccess(formId, email)) {
            event.forwardTo(DashboardView.class);
            return;
        }
        formService.getFormById(formId).ifPresent(form -> {
            currentForm = form;
            buildView();
        });
    }

    private void buildView() {
        removeAll();

        Button backButton = new Button(getTranslation("editor.back"),
            e -> UI.getCurrent().navigate(DashboardView.class));

        H2 title = new H2(getTranslation("editor.title", currentForm.getTitle()));

        titleField = new TextField(getTranslation("editor.form-title"));
        titleField.setValue(currentForm.getTitle() != null ? currentForm.getTitle() : "");
        titleField.setWidth("300px");

        speakerField = new TextField(getTranslation("editor.speaker"));
        speakerField.setValue(currentForm.getSpeakerName() != null ? currentForm.getSpeakerName() : "");
        speakerField.setWidth("300px");

        topicField = new TextField(getTranslation("editor.topic"));
        topicField.setValue(currentForm.getTopic() != null ? currentForm.getTopic() : "");
        topicField.setWidth("300px");

        Button saveButton = new Button(getTranslation("editor.save"), e -> saveFormDetails());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout formFields = new HorizontalLayout(titleField, speakerField, topicField, saveButton);
        formFields.setAlignItems(Alignment.BASELINE);
        formFields.setWidthFull();

        questionGrid = new Grid<>(FeedbackQuestion.class, false);
        questionGrid.addColumn(FeedbackQuestion::getOrderIndex).setHeader(getTranslation("editor.column.order")).setWidth("60px");
        questionGrid.addColumn(FeedbackQuestion::getQuestionText).setHeader(getTranslation("editor.column.question")).setAutoWidth(true);
        questionGrid.addColumn(q -> q.getQuestionType().name()).setHeader(getTranslation("editor.column.type")).setWidth("100px");
        questionGrid.setItems(currentForm.getQuestions());
        questionGrid.setHeight("400px");

        TextField newQuestionText = new TextField(getTranslation("editor.new-question"));
        newQuestionText.setWidth("400px");

        ComboBox<QuestionType> newQuestionType = new ComboBox<>(getTranslation("editor.new-question.type"));
        newQuestionType.setItems(QuestionType.values());
        newQuestionType.setValue(QuestionType.RATING);

        Button addQuestionBtn = new Button(getTranslation("editor.add-question"), e -> {
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
        Notification.show(getTranslation("editor.saved"), 2000, Notification.Position.BOTTOM_START);
    }
}
