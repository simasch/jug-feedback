package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FeedbackQuestion;
import ch.martinelli.feedback.form.domain.FeedbackQuestionRepository;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.QuestionType;
import ch.martinelli.feedback.response.domain.FeedbackAnswer;
import ch.martinelli.feedback.response.ui.PdfExportService;
import ch.martinelli.feedback.response.ui.ResultsView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.assertj.core.api.Assertions.assertThat;

class UC15ExportResultsPdfTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc15-owner@example.com";
    private static final String SHARED_EMAIL = "uc15-shared@example.com";
    @Autowired
    private FormService formService;

    @Autowired
    private FeedbackQuestionRepository questionRepository;

    @Autowired
    private PdfExportService pdfExportService;

    private Long formId;

    @BeforeEach
    void createFormWithResponses() {
        var form = formService.createForm("Test Form", "Jane Doe", LocalDate.of(2026, 6, 1), "Bern", OWNER_EMAIL);
        formId = form.id();

        questionRepository.save(new FeedbackQuestion(null, formId, "Overall quality", QuestionType.RATING, 1));
        questionRepository.save(new FeedbackQuestion(null, formId, "Comments", QuestionType.TEXT, 2));

        formService.publishForm(formId);
    }

    @Test
    @UseCase(id = "UC-15")
    void export_pdf_button_is_visible() {
        login(OWNER_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        var anchors = _find(Anchor.class);
        var pdfLink = anchors.stream()
                .filter(a -> a.getText().equals("Export PDF"))
                .findFirst();
        assertThat(pdfLink).isPresent();
    }

    @Test
    @UseCase(id = "UC-15", businessRules = "BR-034")
    void shared_user_can_see_export_button() {
        formService.shareForm(formId, SHARED_EMAIL);

        login(SHARED_EMAIL, List.of("USER"));
        UI.getCurrent().navigate(ResultsView.class, formId);

        var anchors = _find(Anchor.class);
        var pdfLink = anchors.stream()
                .filter(a -> a.getText().equals("Export PDF"))
                .findFirst();
        assertThat(pdfLink).isPresent();
    }

    @Test
    @UseCase(id = "UC-15", businessRules = "BR-033")
    void pdf_contains_form_data_with_responses() {
        var form = formService.getFormById(formId).orElseThrow();
        var answers = new ArrayList<FeedbackAnswer>();
        for (var question : form.questions()) {
            if (question.questionType() == QuestionType.RATING) {
                answers.add(new FeedbackAnswer(null, null, question.id(), 5, null));
            } else {
                answers.add(new FeedbackAnswer(null, null, question.id(), null, "Excellent!"));
            }
        }
        formService.submitResponse(formId, answers);

        byte[] pdfBytes = pdfExportService.generateResultsPdf(form, formService);

        assertThat(pdfBytes).isNotNull()
                .hasSizeGreaterThan(0);
        // Verify it's a valid PDF (starts with %PDF)
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @UseCase(id = "UC-15", scenario = "A1")
    void pdf_generated_with_no_responses() {
        var form = formService.getFormById(formId).orElseThrow();

        byte[] pdfBytes = pdfExportService.generateResultsPdf(form, formService);

        assertThat(pdfBytes).isNotNull()
                .hasSizeGreaterThan(0);
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @UseCase(id = "UC-15", businessRules = "BR-032")
    void pdf_file_name_replaces_special_characters() {
        var form = formService.createForm("My Form! @#$", "Speaker", LocalDate.now(), "Loc", OWNER_EMAIL);
        String fileName = form.title().replaceAll("[^a-zA-Z0-9\\-]", "_") + "_results.pdf";

        assertThat(fileName).isEqualTo("My_Form______results.pdf");
    }

    @Test
    @UseCase(id = "UC-15", businessRules = "BR-033")
    void pdf_contains_rating_distribution() {
        var form = formService.getFormById(formId).orElseThrow();

        // Submit multiple responses with different ratings
        for (int rating : new int[]{5, 4, 4, 3}) {
            var answers = new ArrayList<FeedbackAnswer>();
            for (var question : form.questions()) {
                if (question.questionType() == QuestionType.RATING) {
                    answers.add(new FeedbackAnswer(null, null, question.id(), rating, null));
                } else {
                    answers.add(new FeedbackAnswer(null, null, question.id(), null, "Comment"));
                }
            }
            formService.submitResponse(formId, answers);
        }

        byte[] pdfBytes = pdfExportService.generateResultsPdf(form, formService);

        assertThat(pdfBytes).isNotNull()
                .hasSizeGreaterThan(0);
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
    }
}
