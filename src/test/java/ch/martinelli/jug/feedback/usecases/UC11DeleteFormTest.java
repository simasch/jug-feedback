package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.service.FormService;
import ch.martinelli.jug.feedback.views.DashboardView;
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

    private Long formId;

    @BeforeEach
    void createClosedForm() {
        var form = formService.createFormFromTemplate("Delete Test", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formId = form.getId();
        formService.publishForm(formId);
        formService.closeForm(formId);
        login(OWNER_EMAIL, List.of("USER"));
    }

    @SuppressWarnings("unchecked")
    private int findFormRowIndex(Long targetFormId) {
        Grid<FeedbackForm> grid = _get(Grid.class);
        var items = _findAll(grid);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(targetFormId)) {
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
    void closed_form_shows_delete_button() {
        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButtonForForm(formId, "Delete")).isNotNull();
    }

    @Test
    void delete_removes_form() {
        UI.getCurrent().navigate(DashboardView.class);

        _click(findActionButtonForForm(formId, "Delete"));

        // Form should no longer exist
        assertThat(formService.getFormById(formId)).isEmpty();

        // Form should no longer appear in grid
        assertThat(findFormRowIndex(formId)).isEqualTo(-1);
    }

    @Test
    void delete_also_removes_shares() {
        formService.shareForm(formId, "shared@example.com");
        assertThat(formService.getShares(formId)).hasSize(1);

        UI.getCurrent().navigate(DashboardView.class);
        _click(findActionButtonForForm(formId, "Delete"));

        assertThat(formService.getFormById(formId)).isEmpty();
    }

    @Test
    void draft_form_does_not_show_delete_button() {
        var draftForm = formService.createFormFromTemplate("Draft Form", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);

        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButtonForForm(draftForm.getId(), "Delete")).isNull();
    }

    @Test
    void public_form_does_not_show_delete_button() {
        var publicForm = formService.createFormFromTemplate("Public Form", "Speaker", LocalDate.now(), "Location", OWNER_EMAIL);
        formService.publishForm(publicForm.getId());

        UI.getCurrent().navigate(DashboardView.class);

        assertThat(findActionButtonForForm(publicForm.getId(), "Delete")).isNull();
    }
}
