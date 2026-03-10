package ch.martinelli.feedback.response.ui;

import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.QuestionType;
import ch.martinelli.feedback.form.ui.DashboardView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.util.Map;

@Route("results")
@PermitAll
public class ResultsView extends VerticalLayout implements HasUrlParameter<Long>, HasDynamicTitle {

    private final transient FormService formService;
    private final transient PdfExportService pdfExportService;

    public ResultsView(FormService formService, PdfExportService pdfExportService) {
        this.formService = formService;
        this.pdfExportService = pdfExportService;
        setSizeFull();
        setPadding(true);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("results.page-title");
    }

    @Override
    public void setParameter(BeforeEvent event, Long formId) {
        var email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!formService.hasAccess(formId, email)) {
            event.forwardTo(DashboardView.class);
            return;
        }
        formService.getFormById(formId).ifPresent(this::buildView);
    }

    private void buildView(FeedbackForm form) {
        removeAll();

        var backButton = new Button(getTranslation("results.back"),
                e -> UI.getCurrent().navigate(DashboardView.class));

        String fileName = form.title().replaceAll("[^a-zA-Z0-9\\-]", "_") + "_results.pdf";
        var exportPdfLink = new Anchor(
                DownloadHandler.fromInputStream(_ -> {
                    byte[] pdfBytes = pdfExportService.generateResultsPdf(form, formService);
                    return new DownloadResponse(new ByteArrayInputStream(pdfBytes), fileName,
                            "application/pdf", pdfBytes.length);
                }),
                getTranslation("results.export-pdf"));
        exportPdfLink.getElement().getStyle().set("display", "inline-flex");
        exportPdfLink.getElement().getStyle().set("align-items", "center");
        exportPdfLink.getElement().setAttribute("theme", "badge primary");

        var buttonBar = new HorizontalLayout(backButton, exportPdfLink);
        buttonBar.setAlignItems(Alignment.CENTER);
        var title = new H2(getTranslation("results.title", form.title()));
        add(buttonBar, title);

        if (form.speakerName() != null && !form.speakerName().isEmpty()) {
            add(new Span(getTranslation("results.speaker", form.speakerName())));
        }

        var responseCount = formService.getResponseCount(form.id());
        add(new Paragraph(getTranslation("results.total-responses", responseCount)));

        if (responseCount == 0) {
            add(new Paragraph(getTranslation("results.no-responses")));
            return;
        }

        for (var question : form.questions()) {
            add(new H3(question.orderIndex() + ". " + question.questionText()));

            if (question.questionType() == QuestionType.RATING) {
                addRatingResult(question.id());
            } else {
                addTextResults(question.id());
            }
        }
    }

    private void addRatingResult(Long questionId) {
        var distribution = formService.getRatingDistribution(questionId);
        long totalRatings = distribution.values().stream().mapToLong(Long::longValue).sum();
        long maxCount = distribution.values().stream().mapToLong(Long::longValue).max().orElse(1);

        var chartContainer = new Div();
        chartContainer.addClassName("rating-distribution");
        chartContainer.getStyle()
                .set("max-width", "500px")
                .set("margin", "8px 0 12px 0");

        for (Map.Entry<Integer, Long> entry : distribution.entrySet()) {
            int rating = entry.getKey();
            long count = entry.getValue();
            double percentage = totalRatings > 0 ? (count * 100.0) / totalRatings : 0;
            double barWidth = maxCount > 0 ? (count * 100.0) / maxCount : 0;

            var row = new Div();
            row.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("gap", "8px")
                    .set("margin-bottom", "4px");

            var label = new Span(String.valueOf(rating));
            label.getStyle()
                    .set("min-width", "16px")
                    .set("text-align", "right")
                    .set("font-weight", "bold");

            var barBackground = new Div();
            barBackground.getStyle()
                    .set("flex", "1")
                    .set("background-color", "var(--lumo-contrast-10pct)")
                    .set("border-radius", "4px")
                    .set("height", "20px")
                    .set("overflow", "hidden");

            var bar = new Div();
            bar.getStyle()
                    .set("width", String.format("%.1f%%", barWidth))
                    .set("height", "100%")
                    .set("background-color", "var(--lumo-primary-color)")
                    .set("border-radius", "4px")
                    .set("transition", "width 0.3s ease");

            barBackground.add(bar);

            var countLabel = new Span(String.format("%d (%.0f%%)", count, percentage));
            countLabel.getStyle()
                    .set("min-width", "80px")
                    .set("font-size", "var(--lumo-font-size-s)");

            row.add(label, barBackground, countLabel);
            chartContainer.add(row);
        }

        add(chartContainer);

        var avg = formService.getAverageRating(questionId);
        if (avg != null) {
            add(new Paragraph(getTranslation("results.average-rating-with-count",
                    String.format("%.2f", avg), totalRatings)));
        } else {
            add(new Paragraph(getTranslation("results.average-rating-na")));
        }
    }

    private void addTextResults(Long questionId) {
        var textAnswers = formService.getTextAnswers(questionId);
        for (var answer : textAnswers) {
            if (answer.textValue() != null && !answer.textValue().trim().isEmpty()) {
                var p = new Paragraph("\u2022 " + answer.textValue());
                p.getStyle().set("margin-left", "20px");
                add(p);
            }
        }
    }
}
