package com.eb.javafx.ui;

import com.eb.javafx.routing.SceneRouter;

import java.util.List;

/**
 * Factory methods for simple reusable informational screen models.
 *
 * <p>The helpers centralize fallback screen text for engine-owned routes such as startup errors and missing
 * content, while still returning the same neutral {@link ScreenViewModel} used by authored screens.</p>
 */
public final class InformationalScreenModels {
    private InformationalScreenModels() {
    }

    public static ScreenViewModel backToMainMenu(String title, String bodyText) {
        return new ScreenViewModel(
                title,
                List.of(bodyText),
                List.of(new ScreenActionViewModel("Back to main menu", SceneRouter.MAIN_MENU_ROUTE, true)));
    }
}
