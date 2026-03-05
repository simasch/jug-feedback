package ch.martinelli.jug.feedback.views;

import ch.martinelli.jug.feedback.service.TokenService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.List;

@Route("login")
@PageTitle("Login - JUG Feedback")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final TokenService tokenService;

    private final EmailField emailField = new EmailField("Email");
    private final TextField codeField = new TextField("Login Code");
    private final Button sendCodeButton;
    private final Button loginButton;

    private String currentEmail;

    public LoginView(TokenService tokenService) {
        this.tokenService = tokenService;

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        H2 title = new H2("JUG Feedback Login");

        emailField.setPlaceholder("Enter your email address");
        emailField.setWidth("300px");

        sendCodeButton = new Button("Send Login Code", e -> sendCode());
        sendCodeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendCodeButton.setWidth("300px");

        codeField.setPlaceholder("Enter the 8-digit code");
        codeField.setWidth("300px");
        codeField.setVisible(false);

        loginButton = new Button("Login", e -> authenticate());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidth("300px");
        loginButton.setVisible(false);

        add(title, emailField, sendCodeButton, codeField, loginButton);
    }

    private void sendCode() {
        String email = emailField.getValue().trim();
        if (email.isEmpty() || emailField.isInvalid()) {
            Notification.show("Please enter a valid email address", 3000, Notification.Position.MIDDLE);
            return;
        }

        currentEmail = email;
        tokenService.sendLoginCode(email);

        emailField.setReadOnly(true);
        sendCodeButton.setVisible(false);
        codeField.setVisible(true);
        loginButton.setVisible(true);
        codeField.focus();

        Notification.show("Login code sent! Check your inbox.", 3000, Notification.Position.MIDDLE);
    }

    private void authenticate() {
        String code = codeField.getValue().trim();
        if (code.isEmpty()) {
            Notification.show("Please enter the login code", 3000, Notification.Position.MIDDLE);
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
            Notification.show("Invalid or expired code", 3000, Notification.Position.MIDDLE);
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
