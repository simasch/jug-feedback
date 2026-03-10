package ch.martinelli.feedback.form.ui;

import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.FormStatus;
import ch.martinelli.feedback.form.domain.FormTemplate;
import ch.martinelli.feedback.form.domain.QrCodeService;
import ch.martinelli.feedback.response.ui.ResultsView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;

@Route("")
@PermitAll
public class DashboardView extends VerticalLayout implements HasDynamicTitle {

    private static final String CANCEL_KEY = "dashboard.create.cancel";

    private final transient FormService formService;
    private final transient QrCodeService qrCodeService;
    private final Grid<FeedbackForm> grid;

    public DashboardView(FormService formService, QrCodeService qrCodeService) {
        this.formService = formService;
        this.qrCodeService = qrCodeService;

        setSizeFull();
        setPadding(true);

        var title = new H2(getTranslation("dashboard.title"));
        var createButton = new Button(getTranslation("dashboard.create-new"), e -> showCreateDialog());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var createFromTemplateButton = new Button(getTranslation("dashboard.create-from-template"), e -> showCreateFromTemplateDialog());

        var header = new HorizontalLayout(title, createButton, createFromTemplateButton);
        header.setAlignItems(Alignment.BASELINE);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        grid = new Grid<>(FeedbackForm.class, false);
        grid.addColumn(FeedbackForm::title).setHeader(getTranslation("dashboard.column.title")).setAutoWidth(true);
        grid.addColumn(FeedbackForm::speakerName).setHeader(getTranslation("dashboard.column.speaker")).setAutoWidth(true);
        grid.addColumn(FeedbackForm::eventDate).setHeader(getTranslation("dashboard.column.date")).setAutoWidth(true);
        grid.addColumn(FeedbackForm::location).setHeader(getTranslation("dashboard.column.location")).setAutoWidth(true);
        grid.addColumn(form -> form.status().name()).setHeader(getTranslation("dashboard.column.status")).setAutoWidth(true);
        grid.addColumn(form -> {
            var currentUser = getCurrentUserEmail();
            if (currentUser.equals(form.ownerEmail())) {
                return getTranslation("dashboard.access.owner");
            }
            return getTranslation("dashboard.access.shared");
        }).setHeader(getTranslation("dashboard.column.access")).setAutoWidth(true);
        grid.addColumn(form -> getTranslation("dashboard.responses", formService.getResponseCount(form.id())))
                .setHeader(getTranslation("dashboard.column.responses")).setAutoWidth(true);
        grid.addComponentColumn(this::createActionButtons).setKey("actions").setHeader("").setAutoWidth(true);
        grid.setSizeFull();

        add(header, grid);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("dashboard.page-title");
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private void refreshGrid() {
        grid.setItems(formService.getFormsForUser(getCurrentUserEmail()));
    }

    private HorizontalLayout createActionButtons(FeedbackForm form) {
        var buttons = new HorizontalLayout();
        var currentUser = getCurrentUserEmail();
        var isOwner = currentUser.equals(form.ownerEmail());

        var resultsButton = new Button(getTranslation("dashboard.action.results"), e -> UI.getCurrent().navigate(ResultsView.class, form.id()));
        resultsButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        var qrButton = new Button(getTranslation("dashboard.action.qr-code"), e -> showQrDialog(form));
        qrButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        if (!isOwner) {
            buttons.add(qrButton, resultsButton);
            return buttons;
        }

        var shareButton = new Button(getTranslation("dashboard.action.share"), e -> showShareDialog(form));
        shareButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        var templateButton = new Button(getTranslation("dashboard.action.save-template"), e -> showSaveAsTemplateDialog(form));
        templateButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        if (form.status() == FormStatus.DRAFT) {
            var editButton = new Button(getTranslation("dashboard.action.edit"), e -> UI.getCurrent().navigate(FormEditorView.class, form.id()));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

            var publishButton = new Button(getTranslation("dashboard.action.publish"), e -> {
                formService.publishForm(form.id());
                refreshGrid();
            });
            publishButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            buttons.add(editButton, publishButton, qrButton, resultsButton, shareButton, templateButton);
        } else if (form.status() == FormStatus.PUBLIC) {
            var closeButton = new Button(getTranslation("dashboard.action.close"), e -> {
                formService.closeForm(form.id());
                refreshGrid();
            });
            closeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);

            if (formService.getResponseCount(form.id()) == 0) {
                var unpublishButton = new Button(getTranslation("dashboard.action.unpublish"), e -> {
                    formService.unpublishForm(form.id());
                    refreshGrid();
                });
                unpublishButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
                buttons.add(unpublishButton);
            }

            buttons.add(closeButton, qrButton, resultsButton, shareButton, templateButton);
        } else {
            var reopenButton = new Button(getTranslation("dashboard.action.reopen"), e -> {
                formService.reopenForm(form.id());
                refreshGrid();
            });
            reopenButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

            var deleteButton = new Button(getTranslation("dashboard.action.delete"), e -> {
                formService.deleteForm(form.id());
                refreshGrid();
            });
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            buttons.add(reopenButton, qrButton, resultsButton, shareButton, templateButton, deleteButton);
        }

        return buttons;
    }

    private void showShareDialog(FeedbackForm form) {
        var dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("dashboard.share.title", form.title()));

        var content = new VerticalLayout();
        content.setPadding(false);

        var shares = formService.getShares(form.id());
        var shareList = new VerticalLayout();
        shareList.setPadding(false);
        shareList.setSpacing(false);

        for (var share : shares) {
            var row = new HorizontalLayout();
            row.setAlignItems(Alignment.CENTER);
            var emailSpan = new Span(share.sharedWithEmail());
            var removeBtn = new Button(getTranslation("dashboard.share.remove"), e -> {
                formService.unshareForm(form.id(), share.sharedWithEmail());
                dialog.close();
                showShareDialog(form);
            });
            removeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            row.add(emailSpan, removeBtn);
            shareList.add(row);
        }

        if (shares.isEmpty()) {
            shareList.add(new Span(getTranslation("dashboard.share.empty")));
        }

        var emailField = new EmailField(getTranslation("dashboard.share.email"));
        emailField.setWidthFull();

        var addButton = new Button(getTranslation("dashboard.share.button"), e -> {
            var email = emailField.getValue().trim();
            if (email.isEmpty() || emailField.isInvalid()) {
                Notification.show(getTranslation("dashboard.share.error.invalid-email"), 3000, Notification.Position.MIDDLE);
                return;
            }
            if (email.equals(form.ownerEmail())) {
                Notification.show(getTranslation("dashboard.share.error.self"), 3000, Notification.Position.MIDDLE);
                return;
            }
            formService.shareForm(form.id(), email);
            Notification.show(getTranslation("dashboard.share.success", email), 3000, Notification.Position.BOTTOM_START);
            dialog.close();
            showShareDialog(form);
        });
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var addRow = new HorizontalLayout(emailField, addButton);
        addRow.setAlignItems(Alignment.BASELINE);
        addRow.setWidthFull();

        content.add(shareList, addRow);
        dialog.add(content);
        dialog.setCloseOnOutsideClick(true);
        dialog.open();
    }

    private void showQrDialog(FeedbackForm form) {
        var dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("dashboard.qr.title", form.title()));

        var request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        var formUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
                + "/form/" + form.publicToken();
        var qrBytes = qrCodeService.generateQrCode(formUrl, 300, 300);

        var qrImage = new Image(
                DownloadHandler.fromInputStream(event ->
                        new DownloadResponse(new ByteArrayInputStream(qrBytes), "qr.png", "image/png", qrBytes.length)),
                "QR Code");
        qrImage.setWidth("300px");
        qrImage.setHeight("300px");

        var urlAnchor = new Anchor(formUrl, formUrl);
        urlAnchor.setTarget("_blank");
        urlAnchor.getStyle().set("word-break", "break-all");

        var copyButton = new Button(getTranslation("dashboard.qr.copy-url"));
        copyButton.getElement().executeJs("""
                this.addEventListener('click', function() {
                    var url = $0;
                    if (navigator.clipboard && navigator.clipboard.writeText) {
                        navigator.clipboard.writeText(url);
                    } else {
                        var ta = document.createElement('textarea');
                        ta.value = url;
                        ta.style.position = 'fixed';
                        ta.style.left = '-9999px';
                        document.body.appendChild(ta);
                        ta.select();
                        document.execCommand('copy');
                        document.body.removeChild(ta);
                    }
                })""", formUrl);
        copyButton.addClickListener(e ->
                Notification.show(getTranslation("dashboard.qr.copied"), 2000, Notification.Position.BOTTOM_START));

        var dialogContent = new VerticalLayout(qrImage, urlAnchor, copyButton);
        dialogContent.setAlignItems(Alignment.CENTER);

        dialog.add(dialogContent);
        dialog.setCloseOnOutsideClick(true);
        dialog.open();
    }

    private void showSaveAsTemplateDialog(FeedbackForm form) {
        var dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("dashboard.template.title"));

        var nameField = new TextField(getTranslation("dashboard.template.name"));
        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setValue(form.title());

        var saveButton = new Button(getTranslation("dashboard.template.save"), e -> {
            if (nameField.getValue().trim().isEmpty()) {
                nameField.setInvalid(true);
                nameField.setErrorMessage(getTranslation("dashboard.template.error.name-required"));
                return;
            }
            formService.saveFormAsTemplate(form.id(), nameField.getValue().trim());
            dialog.close();
            Notification.show(getTranslation("dashboard.template.success"), 3000, Notification.Position.BOTTOM_START);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelButton = new Button(getTranslation(CANCEL_KEY), e -> dialog.close());

        var content = new VerticalLayout(nameField);
        content.setPadding(false);

        var footer = new HorizontalLayout(saveButton, cancelButton);

        dialog.add(content);
        dialog.getFooter().add(footer);
        dialog.open();
        nameField.focus();
    }

    private void showCreateFromTemplateDialog() {
        var dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("dashboard.from-template.title"));

        var templates = formService.getTemplatesForUser(getCurrentUserEmail());

        var templateSelector = new ComboBox<FormTemplate>(getTranslation("dashboard.from-template.template"));
        templateSelector.setItems(templates);
        templateSelector.setItemLabelGenerator(FormTemplate::name);
        templateSelector.setWidthFull();
        templateSelector.setRequired(true);

        var titleField = new TextField(getTranslation("dashboard.create.form-title"));
        titleField.setWidthFull();
        titleField.setRequired(true);

        var speakerField = new TextField(getTranslation("dashboard.create.speaker"));
        speakerField.setWidthFull();

        var dateField = new DatePicker(getTranslation("dashboard.create.date"));
        dateField.setWidthFull();

        var locationField = new TextField(getTranslation("dashboard.create.location"));
        locationField.setWidthFull();

        var createButton = new Button(getTranslation("dashboard.create.button"), e -> {
            if (templateSelector.getValue() == null) {
                templateSelector.setInvalid(true);
                templateSelector.setErrorMessage(getTranslation("dashboard.from-template.error.template-required"));
                return;
            }
            if (titleField.getValue().trim().isEmpty()) {
                titleField.setInvalid(true);
                return;
            }
            formService.createFormFromTemplate(
                    templateSelector.getValue(),
                    titleField.getValue().trim(),
                    speakerField.getValue().trim(),
                    dateField.getValue(),
                    locationField.getValue().trim(),
                    getCurrentUserEmail()
            );
            dialog.close();
            refreshGrid();
            Notification.show(getTranslation("dashboard.create.success"), 3000, Notification.Position.BOTTOM_START);
        });
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelButton = new Button(getTranslation(CANCEL_KEY), e -> dialog.close());

        var content = new VerticalLayout(templateSelector, titleField, speakerField, dateField, locationField);
        content.setPadding(false);

        var footer = new HorizontalLayout(createButton, cancelButton);

        dialog.add(content);
        dialog.getFooter().add(footer);
        dialog.open();
        templateSelector.focus();
    }

    private void showCreateDialog() {
        var dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("dashboard.create.title"));

        var titleField = new TextField(getTranslation("dashboard.create.form-title"));
        titleField.setWidthFull();
        titleField.setRequired(true);

        var speakerField = new TextField(getTranslation("dashboard.create.speaker"));
        speakerField.setWidthFull();

        var dateField = new DatePicker(getTranslation("dashboard.create.date"));
        dateField.setWidthFull();

        var locationField = new TextField(getTranslation("dashboard.create.location"));
        locationField.setWidthFull();

        var createButton = new Button(getTranslation("dashboard.create.button"), e -> {
            if (titleField.getValue().trim().isEmpty()) {
                titleField.setInvalid(true);
                return;
            }
            formService.createForm(
                    titleField.getValue().trim(),
                    speakerField.getValue().trim(),
                    dateField.getValue(),
                    locationField.getValue().trim(),
                    getCurrentUserEmail()
            );
            dialog.close();
            refreshGrid();
            Notification.show(getTranslation("dashboard.create.success"), 3000, Notification.Position.BOTTOM_START);
        });
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelButton = new Button(getTranslation(CANCEL_KEY), e -> dialog.close());

        var content = new VerticalLayout(titleField, speakerField, dateField, locationField);
        content.setPadding(false);

        var footer = new HorizontalLayout(createButton, cancelButton);

        dialog.add(content);
        dialog.getFooter().add(footer);
        dialog.open();
        titleField.focus();
    }
}
