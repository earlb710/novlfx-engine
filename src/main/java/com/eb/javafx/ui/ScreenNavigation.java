package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import javafx.scene.control.Button;

/** Shared navigation controls for reusable routed screens. */
final class ScreenNavigation {
    private ScreenNavigation() {
    }

    static Button button(RouteContext context, String text, String routeId) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> context.navigateTo(routeId));
        return button;
    }
}
