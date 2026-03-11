package ch.martinelli.feedback.ui;

import com.vaadin.flow.component.html.Span;

/**
 * Creates a Material Symbols Outlined icon as a Span component.
 */
public final class MaterialIcon {

    private MaterialIcon() {
    }

    public static Span create(String iconName) {
        var icon = new Span(iconName);
        icon.addClassName("material-symbols-outlined");
        icon.getStyle()
                .set("font-size", "18px")
                .set("vertical-align", "middle");
        return icon;
    }
}
