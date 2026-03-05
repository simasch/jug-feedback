package ch.martinelli.jug.feedback.views;

import ch.martinelli.jug.feedback.entity.FeedbackAnswer;
import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.entity.FeedbackQuestion;
import ch.martinelli.jug.feedback.entity.QuestionType;
import ch.martinelli.jug.feedback.service.FormService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route("results")
@PageTitle("Results - JUG Feedback")
@PermitAll
public class ResultsView extends VerticalLayout implements HasUrlParameter<Long> {

    private final FormService formService;

    public ResultsView(FormService formService) {
        this.formService = formService;
        setSizeFull();
        setPadding(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long formId) {
        formService.getFormById(formId).ifPresent(this::buildView);
    }

    private void buildView(FeedbackForm form) {
        removeAll();

        Button backButton = new Button("← Back to Dashboard",
            e -> UI.getCurrent().navigate(DashboardView.class));

        H2 title = new H2("Results: " + form.getTitle());
        add(backButton, title);

        if (form.getSpeakerName() != null && !form.getSpeakerName().isEmpty()) {
            add(new Span("Speaker: " + form.getSpeakerName()));
        }

        long responseCount = formService.getResponseCount(form.getId());
        add(new Paragraph("Total responses: " + responseCount));

        if (responseCount == 0) {
            add(new Paragraph("No responses yet."));
            return;
        }

        for (FeedbackQuestion question : form.getQuestions()) {
            add(new H3(question.getOrderIndex() + ". " + question.getQuestionText()));

            if (question.getQuestionType() == QuestionType.RATING) {
                Double avg = formService.getAverageRating(question.getId());
                if (avg != null) {
                    add(new Paragraph("Average rating: " + String.format("%.2f", avg) + " / 5"));
                }
            } else {
                List<FeedbackAnswer> textAnswers = formService.getTextAnswers(question.getId());
                for (FeedbackAnswer answer : textAnswers) {
                    if (answer.getTextValue() != null && !answer.getTextValue().trim().isEmpty()) {
                        Paragraph p = new Paragraph("• " + answer.getTextValue());
                        p.getStyle().set("margin-left", "20px");
                        add(p);
                    }
                }
            }
        }
    }
}
