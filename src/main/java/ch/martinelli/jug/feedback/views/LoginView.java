package ch.martinelli.jug.feedback.views;

import ch.martinelli.jug.feedback.service.TokenService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.List;

@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    private final TokenService tokenService;

    private final EmailField emailField = new EmailField();
    private final TextField codeField = new TextField();
    private final Button sendCodeButton;
    private final Button loginButton;
    private ShortcutRegistration sendCodeShortcut;

    private String currentEmail;

    public LoginView(TokenService tokenService, BuildProperties buildProperties) {
        this.tokenService = tokenService;

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        H2 title = new H2(getTranslation("login.title"));

        emailField.setLabel(getTranslation("login.email"));
        emailField.setPlaceholder(getTranslation("login.email.placeholder"));
        emailField.setWidth("300px");

        sendCodeButton = new Button(getTranslation("login.send-code"), e -> sendCode());
        sendCodeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendCodeShortcut = sendCodeButton.addClickShortcut(Key.ENTER);
        sendCodeButton.setWidth("300px");

        codeField.setLabel(getTranslation("login.code"));
        codeField.setPlaceholder(getTranslation("login.code.placeholder"));
        codeField.setWidth("300px");
        codeField.setVisible(false);

        loginButton = new Button(getTranslation("login.login"), e -> authenticate());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidth("300px");
        loginButton.setVisible(false);

        Span version = new Span(getTranslation("login.version", buildProperties.getVersion()));
        version.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        add(title, emailField, sendCodeButton, codeField, loginButton, version);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("login.page-title");
    }

    private void sendCode() {
        String email = emailField.getValue().trim();
        if (email.isEmpty() || emailField.isInvalid()) {
            Notification.show(getTranslation("login.error.invalid-email"), 3000, Notification.Position.MIDDLE);
            return;
        }

        currentEmail = email;
        tokenService.sendLoginCode(email);

        emailField.setReadOnly(true);
        sendCodeShortcut.remove();
        sendCodeButton.setVisible(false);
        codeField.setVisible(true);
        loginButton.setVisible(true);
        loginButton.addClickShortcut(Key.ENTER);
        codeField.focus();

        Notification.show(getTranslation("login.code-sent"), 3000, Notification.Position.MIDDLE);
    }

    private void authenticate() {
        String code = codeField.getValue().trim();
        if (code.isEmpty()) {
            Notification.show(getTranslation("login.error.empty-code"), 3000, Notification.Position.MIDDLE);
            return;
        }

        if (tokenService.validateCode(currentEmail, code)) {
            UsernamePasswordAuthenticationToken auth =
                UsernamePasswordAuthenticationToken.authenticated(
                    currentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            VaadinServletRequest vaadinRequest = (VaadinServletRequest) VaadinRequest.getCurrent();
            HttpSession session = vaadinRequest.getHttpServletRequest().getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            UI.getCurrent().navigate(DashboardView.class);
        } else {
            Notification.show(getTranslation("login.error.invalid-code"), 3000, Notification.Position.MIDDLE);
            codeField.clear();
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            event.forwardTo(DashboardView.class);
        }
    }
}
