package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.UseCase;
import ch.martinelli.jug.feedback.views.LoginView;
import com.github.mvysny.fakeservlet.FakeRequest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinServletRequest;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Objects;

import static ch.martinelli.jug.feedback.jooq.Tables.ACCESS_TOKEN;
import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.assertj.core.api.Assertions.assertThat;

class UC01LoginTest extends KaribuTest {

    @Autowired
    private DSLContext dsl;

    @Test
    @UseCase(id = "UC-01")
    void login_view_displays_email_field_and_send_code_button() {
        UI.getCurrent().navigate(LoginView.class);

        assertThat(_get(EmailField.class, spec -> spec.withLabel("Email")).isVisible()).isTrue();
        assertThat(_get(Button.class, spec -> spec.withText("Send Login Code")).isVisible()).isTrue();

        // Code field and login button should not be visible
        assertThat(_find(TextField.class).stream().noneMatch(tf -> tf.isVisible() && "Login Code".equals(tf.getLabel()))).isTrue();
        assertThat(_find(Button.class).stream().noneMatch(b -> b.isVisible() && "Login".equals(b.getText()))).isTrue();
    }

    @Test
    @UseCase(id = "UC-01", scenario = "A1")
    void send_code_with_empty_email_shows_error() {
        UI.getCurrent().navigate(LoginView.class);

        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        expectNotifications("Please enter a valid email address");
    }

    @Test
    @UseCase(id = "UC-01", businessRules = {"BR-001", "BR-002"})
    void send_code_with_valid_email_shows_code_field() {
        UI.getCurrent().navigate(LoginView.class);

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), "test@example.com");
        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        expectNotifications("Login code sent! Check your inbox.");

        assertThat(_get(EmailField.class, spec -> spec.withLabel("Email")).isReadOnly()).isTrue();
        assertThat(_get(TextField.class, spec -> spec.withLabel("Login Code")).isVisible()).isTrue();
        assertThat(_get(Button.class, spec -> spec.withText("Login")).isVisible()).isTrue();

        // Send code button should no longer be visible
        assertThat(_find(Button.class).stream().noneMatch(b -> b.isVisible() && "Send Login Code".equals(b.getText()))).isTrue();
    }

    @Test
    @UseCase(id = "UC-01", scenario = "A1")
    void login_with_empty_code_shows_error() {
        UI.getCurrent().navigate(LoginView.class);

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), "test@example.com");
        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        _click(_get(Button.class, spec -> spec.withText("Login")));

        expectNotifications("Login code sent! Check your inbox.", "Please enter the login code");
    }

    @Test
    @UseCase(id = "UC-01", scenario = "A1", businessRules = "BR-001")
    void login_with_invalid_code_shows_error() {
        UI.getCurrent().navigate(LoginView.class);

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), "invalid@example.com");
        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        _setValue(_get(TextField.class, spec -> spec.withLabel("Login Code")), "00000000");
        _click(_get(Button.class, spec -> spec.withText("Login")));

        expectNotifications("Login code sent! Check your inbox.", "Invalid or expired code");
    }

    @Test
    @UseCase(id = "UC-01", businessRules = {"BR-001", "BR-002"})
    void successful_login_marks_token_as_used() {
        UI.getCurrent().navigate(LoginView.class);

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), "uc01-login-flow@example.com");
        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        // Read the generated code from DB
        var code = dsl.select(ACCESS_TOKEN.TOKEN)
                .from(ACCESS_TOKEN)
                .where(ACCESS_TOKEN.EMAIL.eq("uc01-login-flow@example.com"))
                .fetchOne(ACCESS_TOKEN.TOKEN);

        _setValue(_get(TextField.class, spec -> spec.withLabel("Login Code")), code);

        // Pre-configure FakeRequest principal so ViewAccessChecker allows navigation to DashboardView
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "uc01-login-flow@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var request = (FakeRequest) VaadinServletRequest.getCurrent().getRequest();
        request.setUserPrincipalInt(auth);
        request.setUserInRole((_, role) -> Objects.equals("USER", role));

        _click(_get(Button.class, spec -> spec.withText("Login")));

        // Assert redirected to dashboard
        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();

        // Assert token is marked as used (update branch was executed)
        var used = dsl.select(ACCESS_TOKEN.USED)
                .from(ACCESS_TOKEN)
                .where(ACCESS_TOKEN.EMAIL.eq("uc01-login-flow@example.com"))
                .fetchOne(ACCESS_TOKEN.USED);
        assertThat(used).isTrue();
    }

    @Test
    @UseCase(id = "UC-01", scenario = "Postcondition")
    void authenticated_user_is_redirected_to_dashboard() {
        login("test@example.com", List.of("USER"));

        UI.getCurrent().navigate(LoginView.class);

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }
}
