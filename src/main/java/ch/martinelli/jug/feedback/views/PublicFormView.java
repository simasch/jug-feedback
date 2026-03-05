package ch.martinelli.jug.feedback.views;

import ch.martinelli.jug.feedback.entity.FeedbackAnswer;
import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.entity.FeedbackQuestion;
import ch.martinelli.jug.feedback.entity.FormStatus;
import ch.martinelli.jug.feedback.entity.QuestionType;
import ch.martinelli.jug.feedback.service.FormService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route("form")
@PageTitle("Feedback Form - JUG Feedback")
@AnonymousAllowed
public class PublicFormView extends VerticalLayout implements HasUrlParameter<String> {

    private final FormService formService;
    private final Map<Long, RadioButtonGroup<Integer>> ratingGroups = new HashMap<>();
    private final Map<Long, TextArea> textAreas = new HashMap<>();
    private FeedbackForm currentForm;

    public PublicFormView(FormService formService) {
        this.formService = formService;
        setPadding(true);
        setMaxWidth("800px");
        getStyle().set("margin", "0 auto");
    }

    @Override
    public void setParameter(BeforeEvent event, String token) {
        Optional<FeedbackForm> optForm = formService.getFormByPublicToken(token);

        if (optForm.isEmpty()) {
            add(new H2("Form not found"),
                    new Paragraph("The requested form does not exist."));
            return;
        }

        FeedbackForm form = optForm.get();

        if (form.getStatus() != FormStatus.PUBLIC) {
            add(new H2("Form not available"),
                    new Paragraph("This form is not currently accepting responses."));
            return;
        }

        currentForm = form;
        buildForm(form);
    }

    private void buildForm(FeedbackForm form) {
        H2 title = new H2(form.getTitle());
        add(title);

        if (form.getSpeakerName() != null && !form.getSpeakerName().isEmpty()) {
            add(new Paragraph("Speaker: " + form.getSpeakerName()));
        }
        if (form.getTopic() != null && !form.getTopic().isEmpty()) {
            add(new Paragraph("Topic: " + form.getTopic()));
        }

        add(new Paragraph("Please rate the following aspects from 1 (poor) to 5 (excellent):"));

        for (FeedbackQuestion question : form.getQuestions()) {
            add(new H3(question.getOrderIndex() + ". " + question.getQuestionText()));

            if (question.getQuestionType() == QuestionType.RATING) {
                RadioButtonGroup<Integer> ratingGroup = new RadioButtonGroup<>();
                ratingGroup.setItems(1, 2, 3, 4, 5);
                ratingGroup.setItemLabelGenerator(i -> switch (i) {
                    case 1 -> "1 - Poor";
                    case 2 -> "2 - Below average";
                    case 3 -> "3 - Average";
                    case 4 -> "4 - Good";
                    case 5 -> "5 - Excellent";
                    default -> i.toString();
                });
                add(ratingGroup);
                ratingGroups.put(question.getId(), ratingGroup);
            } else {
                TextArea textArea = new TextArea();
                textArea.setPlaceholder("Your answer...");
                textArea.setWidthFull();
                textArea.setMinHeight("100px");
                add(textArea);
                textAreas.put(question.getId(), textArea);
            }
        }

        Button submitButton = new Button("Submit Feedback", e -> submitFeedback());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submitButton.setWidthFull();
        add(submitButton);
    }

    private void submitFeedback() {
        List<FeedbackAnswer> answers = new ArrayList<>();

        for (FeedbackQuestion question : currentForm.getQuestions()) {
            FeedbackAnswer answer = new FeedbackAnswer();
            answer.setQuestion(question);

            if (question.getQuestionType() == QuestionType.RATING) {
                RadioButtonGroup<Integer> group = ratingGroups.get(question.getId());
                if (group != null && group.getValue() != null) {
                    answer.setRatingValue(group.getValue());
                }
            } else {
                TextArea textArea = textAreas.get(question.getId());
                if (textArea != null && !textArea.getValue().trim().isEmpty()) {
                    answer.setTextValue(textArea.getValue().trim());
                }
            }

            answers.add(answer);
        }

        formService.submitResponse(currentForm.getId(), answers);

        removeAll();
        add(new H2("Thank you!"),
                new Paragraph("Your feedback has been submitted successfully."));
    }
}
