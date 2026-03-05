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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route("form")
@AnonymousAllowed
public class PublicFormView extends VerticalLayout implements HasUrlParameter<String>, HasDynamicTitle {

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
    public String getPageTitle() {
        return getTranslation("form.page-title");
    }

    @Override
    public void setParameter(BeforeEvent event, String token) {
        Optional<FeedbackForm> optForm = formService.getFormByPublicToken(token);

        if (optForm.isEmpty()) {
            add(new H2(getTranslation("form.not-found")),
                    new Paragraph(getTranslation("form.not-found.message")));
            return;
        }

        FeedbackForm form = optForm.get();

        if (form.getStatus() != FormStatus.PUBLIC) {
            add(new H2(getTranslation("form.not-available")),
                    new Paragraph(getTranslation("form.not-available.message")));
            return;
        }

        currentForm = form;
        buildForm(form);
    }

    private void buildForm(FeedbackForm form) {
        H2 title = new H2(form.getTitle());
        add(title);

        if (form.getSpeakerName() != null && !form.getSpeakerName().isEmpty()) {
            add(new Paragraph(getTranslation("form.speaker", form.getSpeakerName())));
        }
        if (form.getTopic() != null && !form.getTopic().isEmpty()) {
            add(new Paragraph(getTranslation("form.topic", form.getTopic())));
        }

        add(new Paragraph(getTranslation("form.rating.instructions")));

        for (FeedbackQuestion question : form.getQuestions()) {
            add(new H3(question.getOrderIndex() + ". " + question.getQuestionText()));

            if (question.getQuestionType() == QuestionType.RATING) {
                RadioButtonGroup<Integer> ratingGroup = new RadioButtonGroup<>();
                ratingGroup.setItems(1, 2, 3, 4, 5);
                ratingGroup.setItemLabelGenerator(i -> getTranslation("form.rating." + i));
                add(ratingGroup);
                ratingGroups.put(question.getId(), ratingGroup);
            } else {
                TextArea textArea = new TextArea();
                textArea.setPlaceholder(getTranslation("form.text.placeholder"));
                textArea.setWidthFull();
                textArea.setMinHeight("100px");
                add(textArea);
                textAreas.put(question.getId(), textArea);
            }
        }

        Button submitButton = new Button(getTranslation("form.submit"), e -> submitFeedback());
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
        add(new H2(getTranslation("form.thank-you")),
                new Paragraph(getTranslation("form.thank-you.message")));
    }
}
