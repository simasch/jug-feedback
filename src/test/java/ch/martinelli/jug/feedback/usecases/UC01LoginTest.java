package ch.martinelli.jug.feedback.usecases;

import ch.martinelli.jug.feedback.KaribuTest;
import ch.martinelli.jug.feedback.views.LoginView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.assertj.core.api.Assertions.assertThat;

class UC01LoginTest extends KaribuTest {

    @Test
    void login_view_displays_email_field_and_send_code_button() {
        UI.getCurrent().navigate(LoginView.class);

        assertThat(_get(EmailField.class, spec -> spec.withLabel("Email")).isVisible()).isTrue();
        assertThat(_get(Button.class, spec -> spec.withText("Send Login Code")).isVisible()).isTrue();

        // Code field and login button should not be visible
        assertThat(_find(TextField.class).stream().noneMatch(tf -> tf.isVisible() && "Login Code".equals(tf.getLabel()))).isTrue();
        assertThat(_find(Button.class).stream().noneMatch(b -> b.isVisible() && "Login".equals(b.getText()))).isTrue();
    }

    @Test
    void send_code_with_empty_email_shows_error() {
        UI.getCurrent().navigate(LoginView.class);

        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        expectNotifications("Please enter a valid email address");
    }

    @Test
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
    void login_with_empty_code_shows_error() {
        UI.getCurrent().navigate(LoginView.class);

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), "test@example.com");
        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        _click(_get(Button.class, spec -> spec.withText("Login")));

        expectNotifications("Login code sent! Check your inbox.", "Please enter the login code");
    }

    @Test
    void login_with_invalid_code_shows_error() {
        UI.getCurrent().navigate(LoginView.class);

        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), "invalid@example.com");
        _click(_get(Button.class, spec -> spec.withText("Send Login Code")));

        _setValue(_get(TextField.class, spec -> spec.withLabel("Login Code")), "00000000");
        _click(_get(Button.class, spec -> spec.withText("Login")));

        expectNotifications("Login code sent! Check your inbox.", "Invalid or expired code");
    }

    @Test
    void authenticated_user_is_redirected_to_dashboard() {
        login("test@example.com", List.of("USER"));

        UI.getCurrent().navigate(LoginView.class);

        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPath()).isEmpty();
    }
}
