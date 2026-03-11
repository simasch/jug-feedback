package ch.martinelli.feedback.form.ui;

import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.FormTemplate;
import ch.martinelli.feedback.ui.MaterialIcon;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;

@Route("templates")
@PermitAll
public class TemplatesView extends VerticalLayout implements HasDynamicTitle {

    private final transient FormService formService;
    private final Grid<FormTemplate> grid;
    private final Span emptyState;

    public TemplatesView(FormService formService) {
        this.formService = formService;

        setSizeFull();
        setPadding(true);

        var title = new H2(getTranslation("templates.title"));
        add(title);

        emptyState = new Span(getTranslation("templates.empty"));
        emptyState.setVisible(false);
        add(emptyState);

        grid = new Grid<>(FormTemplate.class, false);
        grid.addColumn(FormTemplate::name).setHeader(getTranslation("templates.column.name")).setAutoWidth(true);
        grid.addColumn(t -> t.questions().size()).setHeader(getTranslation("templates.column.questions")).setAutoWidth(true);
        grid.addColumn(t -> t.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setHeader(getTranslation("templates.column.created")).setAutoWidth(true);
        grid.addComponentColumn(this::createActionButtons).setKey("actions").setHeader("").setAutoWidth(true);
        grid.setSizeFull();

        add(grid);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("templates.page-title");
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private void refreshGrid() {
        var templates = formService.getTemplatesForUser(getCurrentUserEmail());
        grid.setItems(templates);
        var empty = templates.isEmpty();
        emptyState.setVisible(empty);
        grid.setVisible(!empty);
    }

    private HorizontalLayout createActionButtons(FormTemplate template) {
        var buttons = new HorizontalLayout();

        var renameButton = new Button(getTranslation("templates.action.rename"), e -> showRenameDialog(template));
        renameButton.setIcon(MaterialIcon.create("edit"));
        renameButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        var deleteButton = new Button(getTranslation("templates.action.delete"), e -> {
            formService.deleteTemplate(template.id());
            refreshGrid();
            Notification.show(getTranslation("templates.delete.success"), 3000, Notification.Position.BOTTOM_START);
        });
        deleteButton.setIcon(MaterialIcon.create("delete"));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);

        buttons.add(renameButton, deleteButton);
        return buttons;
    }

    private void showRenameDialog(FormTemplate template) {
        var dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("templates.rename.title"));

        var nameField = new TextField(getTranslation("templates.rename.name"));
        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setValue(template.name());

        var saveButton = new Button(getTranslation("templates.rename.save"), e -> {
            if (nameField.getValue().trim().isEmpty()) {
                nameField.setInvalid(true);
                nameField.setErrorMessage(getTranslation("templates.rename.error.name-required"));
                return;
            }
            formService.renameTemplate(template.id(), nameField.getValue().trim());
            dialog.close();
            refreshGrid();
            Notification.show(getTranslation("templates.rename.success"), 3000, Notification.Position.BOTTOM_START);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelButton = new Button(getTranslation("templates.rename.cancel"), e -> dialog.close());

        var content = new VerticalLayout(nameField);
        content.setPadding(false);

        var footer = new HorizontalLayout(saveButton, cancelButton);

        dialog.add(content);
        dialog.getFooter().add(footer);
        dialog.open();
        nameField.focus();
    }
}
