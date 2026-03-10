package ch.martinelli.feedback.usecases;

import ch.martinelli.feedback.KaribuTest;
import ch.martinelli.feedback.UseCase;
import ch.martinelli.feedback.auth.ui.LoginView;
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

import static ch.martinelli.feedback.db.Tables.ACCESS_TOKEN;
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

    @Test
    @UseCase(id = "UC-01", businessRules = "BR-004")
    void new_code_request_deletes_previous_tokens() {
        var email = "uc01-token-cleanup@example.com";
        UI.getCurrent().navigate(LoginView.class);

        // Request first code
        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), email);
        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        var firstCode = dsl.select(ACCESS_TOKEN.TOKEN)
                .from(ACCESS_TOKEN)
                .where(ACCESS_TOKEN.EMAIL.eq(email))
                .fetchOne(ACCESS_TOKEN.TOKEN);
        assertThat(firstCode).isNotNull();

        // Navigate back to login and request a second code
        UI.getCurrent().navigate(LoginView.class);
        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), email);
        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        // The first code must no longer exist
        var firstCodeStillExists = dsl.fetchExists(
                dsl.selectOne().from(ACCESS_TOKEN)
                        .where(ACCESS_TOKEN.EMAIL.eq(email))
                        .and(ACCESS_TOKEN.TOKEN.eq(firstCode)));
        assertThat(firstCodeStillExists).isFalse();

        // Only one token for this email must exist
        var tokenCount = dsl.fetchCount(ACCESS_TOKEN, ACCESS_TOKEN.EMAIL.eq(email));
        assertThat(tokenCount).isEqualTo(1);
    }

    @Test
    @UseCase(id = "UC-01", businessRules = "BR-003")
    void used_token_cannot_be_used_again() {
        var email = "uc01-single-use@example.com";
        UI.getCurrent().navigate(LoginView.class);

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), email);
        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        var code = dsl.select(ACCESS_TOKEN.TOKEN)
                .from(ACCESS_TOKEN)
                .where(ACCESS_TOKEN.EMAIL.eq(email))
                .fetchOne(ACCESS_TOKEN.TOKEN);

        // Mark the token as used
        dsl.update(ACCESS_TOKEN)
                .set(ACCESS_TOKEN.USED, true)
                .where(ACCESS_TOKEN.EMAIL.eq(email))
                .execute();

        // Try to use the already-used token
        _setValue(_get(TextField.class, spec -> spec.withLabel("Login Code")), code);
        _click(_get(Button.class, spec -> spec.withText("Login")));

        expectNotifications("Login code sent! Check your inbox.", "Invalid or expired code");
    }
}
