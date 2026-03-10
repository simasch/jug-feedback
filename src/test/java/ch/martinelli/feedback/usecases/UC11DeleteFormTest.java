package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FeedbackQuestion;
import ch.martinelli.feedback.form.domain.FeedbackQuestionRepository;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.QuestionType;
import ch.martinelli.feedback.form.ui.DashboardView;
import ch.martinelli.feedback.response.domain.FeedbackAnswer;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.GridKt.*;
import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class UC11DeleteFormTest extends KaribuTest {

    private static final String OWNER_EMAIL = "uc11-delete@example.com";

    @Autowired
    private FormService formService;

    @Autowired
    private FeedbackQuestionRepository questionRepository;

    private Long formId;

    @BeforeEach
    void createClosedForm() {
        var form = formService.createForm("Delete Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.id();
        formService.publishForm(formId);
        formService.closeForm(formId);
        login(OWNER_EMAIL, List.of("USER"));
    }

    @SuppressWarnings("unchecked")
    private int findFormRowIndex(Long targetFormId) {
        Grid<FeedbackForm> grid = _get(Grid.class);
        var items = _findAll(grid);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id().equals(targetFormId)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private Button findActionButtonForForm(Long targetFormId, String text) {
        Grid<FeedbackForm> grid = _get(Grid.class);
        int rowIndex = findFormRowIndex(targetFormId);
        if (rowIndex < 0) return null;
        var actions = (HorizontalLayout) _getCellComponent(grid, rowIndex, "actions");
        return actions.getChildren()
                .filter(c -> c instanceof Button btn && text.equals(btn.getText()))
                .map(c -> (Button) c)
                .findFirst()
                .orElse(null);
    }

    @Test
    @UseCase(id = "UC-11", businessRules = "BR-021")
    void closed_form_shows_delete_button() {
        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButtonForForm(formId, "Delete")).isNotNull();
    }

    @Test
    @UseCase(id = "UC-11", businessRules = "BR-022")
    void delete_removes_form() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButtonForForm(formId, "Delete"));

        // Form should no longer exist
        assertThat(formService.getFormById(formId)).isEmpty();

        // Form should no longer appear in grid
        assertThat(findFormRowIndex(formId)).isEqualTo(-1);
    }

    @Test
    @UseCase(id = "UC-11", businessRules = "BR-023")
    void delete_also_removes_shares() {
        formService.shareForm(formId, "shared@example.com");
        assertThat(formService.getShares(formId)).hasSize(1);

        UI.getCurrent().navigate(DashboardView.class);
        _click(findActionButtonForForm(formId, "Delete"));

        assertThat(formService.getFormById(formId)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-11", scenario = "Precondition", businessRules = "BR-021")
    void draft_form_does_not_show_delete_button() {
        var draftForm = formService.createForm("Draft Form", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);

        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButtonForForm(draftForm.id(), "Delete")).isNull();
    }

    @Test
    @UseCase(id = "UC-11", scenario = "Precondition", businessRules = "BR-021")
    void public_form_does_not_show_delete_button() {
        var publicForm = formService.createForm("Public Form", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formService.publishForm(publicForm.id());

        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButtonForForm(publicForm.id(), "Delete")).isNull();
    }

    @Test
    @UseCase(id = "UC-11", businessRules = "BR-023")
    void delete_also_removes_responses_and_answers() {
        var question = questionRepository.save(new FeedbackQuestion(null, formId, "Rating", QuestionType.RATING, 1));
        formService.submitResponse(formId, List.of(new FeedbackAnswer(null, null, question.id(), 4, null)));
        assertThat(formService.getResponseCount(formId)).isEqualTo(1);
        assertThat(formService.getRatingDistribution(question.id())).isNotEmpty();

        UI.getCurrent().navigate(DashboardView.class);
        _click(findActionButtonForForm(formId, "Delete"));

        assertThat(formService.getFormById(formId)).isEmpty();
        assertThat(formService.getResponseCount(formId)).isZero();
        assertThat(formService.getRatingDistribution(question.id())).isEmpty();
    }
}
