package ch.martinelli.jug.feedback.views;

import ch.martinelli.jug.feedback.entity.FeedbackForm;
import ch.martinelli.jug.feedback.entity.FormShare;
import ch.martinelli.jug.feedback.entity.FormStatus;
import ch.martinelli.jug.feedback.service.FormService;
import ch.martinelli.jug.feedback.service.QrCodeService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import java.util.List;

@Route("")
@PermitAll
public class DashboardView extends VerticalLayout implements HasDynamicTitle {

    private final FormService formService;
    private final QrCodeService qrCodeService;
    private Grid<FeedbackForm> grid;

    public DashboardView(FormService formService, QrCodeService qrCodeService) {
        this.formService = formService;
        this.qrCodeService = qrCodeService;

        setSizeFull();
        setPadding(true);

        H2 title = new H2(getTranslation("dashboard.title"));
        Button createButton = new Button(getTranslation("dashboard.create-new"), e -> showCreateDialog());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, createButton);
        header.setAlignItems(Alignment.BASELINE);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        grid = new Grid<>(FeedbackForm.class, false);
        grid.addColumn(FeedbackForm::getTitle).setHeader(getTranslation("dashboard.column.title")).setAutoWidth(true);
        grid.addColumn(FeedbackForm::getSpeakerName).setHeader(getTranslation("dashboard.column.speaker")).setAutoWidth(true);
        grid.addColumn(FeedbackForm::getTopic).setHeader(getTranslation("dashboard.column.topic")).setAutoWidth(true);
        grid.addColumn(form -> form.getStatus().name()).setHeader(getTranslation("dashboard.column.status")).setAutoWidth(true);
        grid.addColumn(form -> {
            String currentUser = getCurrentUserEmail();
            if (currentUser.equals(form.getOwnerEmail())) {
                return getTranslation("dashboard.access.owner");
            }
            return getTranslation("dashboard.access.shared");
        }).setHeader(getTranslation("dashboard.column.access")).setAutoWidth(true);
        grid.addColumn(form -> getTranslation("dashboard.responses", formService.getResponseCount(form.getId())))
                .setHeader(getTranslation("dashboard.column.responses")).setAutoWidth(true);
        grid.addComponentColumn(this::createActionButtons).setHeader("").setAutoWidth(true);
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
        HorizontalLayout buttons = new HorizontalLayout();
        String currentUser = getCurrentUserEmail();
        boolean isOwner = currentUser.equals(form.getOwnerEmail());

        Button resultsButton = new Button(getTranslation("dashboard.action.results"), e -> UI.getCurrent().navigate(ResultsView.class, form.getId()));
        resultsButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        Button qrButton = new Button(getTranslation("dashboard.action.qr-code"), e -> showQrDialog(form));
        qrButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        if (!isOwner) {
            buttons.add(qrButton, resultsButton);
            return buttons;
        }

        Button shareButton = new Button(getTranslation("dashboard.action.share"), e -> showShareDialog(form));
        shareButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        if (form.getStatus() == FormStatus.DRAFT) {
            Button editButton = new Button(getTranslation("dashboard.action.edit"), e -> UI.getCurrent().navigate(FormEditorView.class, form.getId()));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

            Button publishButton = new Button(getTranslation("dashboard.action.publish"), e -> {
                formService.publishForm(form.getId());
                refreshGrid();
            });
            publishButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            buttons.add(editButton, publishButton, qrButton, resultsButton, shareButton);
        } else if (form.getStatus() == FormStatus.PUBLIC) {
            Button closeButton = new Button(getTranslation("dashboard.action.close"), e -> {
                formService.closeForm(form.getId());
                refreshGrid();
            });
            closeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            buttons.add(closeButton, qrButton, resultsButton, shareButton);
        } else {
            Button reopenButton = new Button(getTranslation("dashboard.action.reopen"), e -> {
                formService.reopenForm(form.getId());
                refreshGrid();
            });
            reopenButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

            Button deleteButton = new Button(getTranslation("dashboard.action.delete"), e -> {
                formService.deleteForm(form.getId());
                refreshGrid();
            });
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            buttons.add(reopenButton, qrButton, resultsButton, shareButton, deleteButton);
        }

        return buttons;
    }

    private void showShareDialog(FeedbackForm form) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("dashboard.share.title", form.getTitle()));

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);

        List<FormShare> shares = formService.getShares(form.getId());
        VerticalLayout shareList = new VerticalLayout();
        shareList.setPadding(false);
        shareList.setSpacing(false);

        for (FormShare share : shares) {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(Alignment.CENTER);
            Span emailSpan = new Span(share.getSharedWithEmail());
            Button removeBtn = new Button(getTranslation("dashboard.share.remove"), e -> {
                formService.unshareForm(form.getId(), share.getSharedWithEmail());
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

        EmailField emailField = new EmailField(getTranslation("dashboard.share.email"));
        emailField.setWidthFull();

        Button addButton = new Button(getTranslation("dashboard.share.button"), e -> {
            String email = emailField.getValue().trim();
            if (email.isEmpty() || emailField.isInvalid()) {
                Notification.show(getTranslation("dashboard.share.error.invalid-email"), 3000, Notification.Position.MIDDLE);
                return;
            }
            if (email.equals(form.getOwnerEmail())) {
                Notification.show(getTranslation("dashboard.share.error.self"), 3000, Notification.Position.MIDDLE);
                return;
            }
            formService.shareForm(form.getId(), email);
            Notification.show(getTranslation("dashboard.share.success", email), 3000, Notification.Position.BOTTOM_START);
            dialog.close();
            showShareDialog(form);
        });
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout addRow = new HorizontalLayout(emailField, addButton);
        addRow.setAlignItems(Alignment.BASELINE);
        addRow.setWidthFull();

        content.add(shareList, addRow);
        dialog.add(content);
        dialog.setCloseOnOutsideClick(true);
        dialog.open();
    }

    private void showQrDialog(FeedbackForm form) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("dashboard.qr.title", form.getTitle()));

        var request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        String formUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
                + "/form/" + form.getPublicToken();
        byte[] qrBytes = qrCodeService.generateQrCode(formUrl, 300, 300);

        Image qrImage = new Image(
                DownloadHandler.fromInputStream(event ->
                        new DownloadResponse(new ByteArrayInputStream(qrBytes), "qr.png", "image/png", qrBytes.length)),
                "QR Code");
        qrImage.setWidth("300px");
        qrImage.setHeight("300px");

        Span urlSpan = new Span(formUrl);
        urlSpan.getStyle().set("word-break", "break-all");

        Button copyButton = new Button(getTranslation("dashboard.qr.copy-url"));
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

        VerticalLayout dialogContent = new VerticalLayout(qrImage, urlSpan, copyButton);
        dialogContent.setAlignItems(Alignment.CENTER);

        dialog.add(dialogContent);
        dialog.setCloseOnOutsideClick(true);
        dialog.open();
    }

    private void showCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("dashboard.create.title"));

        TextField titleField = new TextField(getTranslation("dashboard.create.form-title"));
        titleField.setWidthFull();
        titleField.setRequired(true);

        TextField speakerField = new TextField(getTranslation("dashboard.create.speaker"));
        speakerField.setWidthFull();

        TextField topicField = new TextField(getTranslation("dashboard.create.topic"));
        topicField.setWidthFull();

        Button createButton = new Button(getTranslation("dashboard.create.button"), e -> {
            if (titleField.getValue().trim().isEmpty()) {
                titleField.setInvalid(true);
                return;
            }
            formService.createFormFromTemplate(
                    titleField.getValue().trim(),
                    speakerField.getValue().trim(),
                    topicField.getValue().trim(),
                    getCurrentUserEmail()
            );
            dialog.close();
            refreshGrid();
            Notification.show(getTranslation("dashboard.create.success"), 3000, Notification.Position.BOTTOM_START);
        });
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button(getTranslation("dashboard.create.cancel"), e -> dialog.close());

        VerticalLayout content = new VerticalLayout(titleField, speakerField, topicField);
        content.setPadding(false);

        HorizontalLayout footer = new HorizontalLayout(createButton, cancelButton);

        dialog.add(content);
        dialog.getFooter().add(footer);
        dialog.open();
        titleField.focus();
    }
}
