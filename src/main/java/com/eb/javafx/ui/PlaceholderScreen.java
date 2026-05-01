package com.eb.javafx.ui;

import com.eb.javafx.routing.SceneRouter;

import java.util.List;

/** Builds simple route-backed screen models for reusable informational screens. */
public final class PlaceholderScreen {
    private PlaceholderScreen() {
    }

    public static ScreenViewModel viewModel(String title, String bodyText) {
        return new ScreenViewModel(
                title,
                List.of(bodyText),
                List.of(new ScreenActionViewModel("Back to main menu", SceneRouter.MAIN_MENU_ROUTE, true)));
    }
}
