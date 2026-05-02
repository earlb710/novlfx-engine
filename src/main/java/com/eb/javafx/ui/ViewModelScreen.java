package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Renders a reusable screen from a UI-neutral view model.
 *
 * <p>The adapter turns model lines into labels and actions into navigation buttons, then wraps them in the
 * shared screen shell and themed scene supplied by the active route context.</p>
 */
public final class ViewModelScreen {
    private ViewModelScreen() {
    }

    public static Scene createScene(RouteContext context, ScreenViewModel viewModel) {
        VBox content = new VBox(8);
        for (String line : viewModel.lines()) {
            content.getChildren().add(new Label(line));
        }
        for (ScreenActionViewModel action : viewModel.actions()) {
            Button button = ScreenNavigation.button(context, action.label(), action.routeId());
            button.setDisable(!action.enabled());
            content.getChildren().add(button);
        }
        return context.themedScene(ScreenShell.titled(viewModel.title(), content));
    }
}
