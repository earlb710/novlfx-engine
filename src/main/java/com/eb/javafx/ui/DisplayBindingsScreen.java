package com.eb.javafx.ui;

import com.eb.javafx.display.DisplayAnimationPlayer;
import com.eb.javafx.display.ImageAssetDefinition;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.display.LayeredCharacterDefinition;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Reusable diagnostic route for display registry bindings and previews.
 *
 * <p>The screen summarizes parsed image aliases, transforms, layered characters, and animation profiles,
 * then renders a limited gallery of display nodes so route authors can verify asset wiring without custom UI.</p>
 */
public final class DisplayBindingsScreen {
    private DisplayBindingsScreen() {
    }

    public static Scene createScene(RouteContext context) {
        ImageDisplayRegistry imageDisplayRegistry = context.imageDisplayRegistry();
        VBox content = new VBox(12);
        content.getChildren().addAll(
                new Label(format("line.parsed-image-aliases", "count", imageDisplayRegistry.images().size())),
                new Label(format("line.parsed-transforms", "count", imageDisplayRegistry.transforms().size())),
                new Label(format("line.layered-character-models", "count", imageDisplayRegistry.layeredCharacters().size())),
                new Label(format("line.animation-profiles", "count", imageDisplayRegistry.animations().size())));

        FlowPane previews = new FlowPane(12, 12);
        previews.setPrefWrapLength(680);
        previews.setAlignment(Pos.TOP_LEFT);
        int previewCount = 0;
        for (ImageAssetDefinition definition : imageDisplayRegistry.images().values()) {
            if (previewCount++ >= 12) {
                break;
            }
            VBox tile = new VBox(6);
            tile.setPadding(new Insets(8));
            tile.getStyleClass().add("screen-panel");
            tile.getChildren().addAll(
                    new Label(definition.id() + " [" + definition.layer() + "]"),
                    imageDisplayRegistry.createDisplayNode(definition.id()),
                    new Label(definition.sourcePath()));

            if ("empty_token_small".equals(definition.id())) {
                DisplayAnimationPlayer animationPlayer = new DisplayAnimationPlayer();
                animationPlayer.buildTimeline(tile, imageDisplayRegistry.animation(ImageDisplayRegistry.DISPLAY_PREVIEW_PULSE_ANIMATION),
                        context.preferencesService().reducedMotion()).play();
            }
            previews.getChildren().add(tile);
        }

        VBox layeredCharacterSummary = new VBox(8);
        layeredCharacterSummary.getChildren().add(new Label(screenText("block.layered-display-composition.title")));
        for (LayeredCharacterDefinition definition : imageDisplayRegistry.layeredCharacters().values()) {
            layeredCharacterSummary.getChildren().add(new Label(ScreenTextResources.format(
                    ScreenTextResources.DISPLAY_BINDINGS,
                    "line.layered-character-summary",
                    Map.of(
                            "id", definition.id(),
                            "drawOrder", definition.drawOrder().toString(),
                            "metadata", definition.metadata().toString()))));
        }

        VBox animationSummary = new VBox(8);
        animationSummary.getChildren().add(new Label(screenText("block.animation-profiles.title")));
        imageDisplayRegistry.animations().values().forEach(animation ->
                animationSummary.getChildren().add(new Label(ScreenTextResources.format(
                        ScreenTextResources.DISPLAY_BINDINGS,
                        "line.animation-summary",
                        Map.of(
                                "id", animation.id(),
                                "steps", Integer.toString(animation.steps().size()),
                                "repeat", Integer.toString(animation.repeatCount()),
                                "autoReverse", Boolean.toString(animation.autoReverse()))))));

        HBox sections = new HBox(16, layeredCharacterSummary, animationSummary);
        sections.setAlignment(Pos.TOP_LEFT);

        content.getChildren().addAll(
                previews,
                sections,
                ScreenNavigation.button(context, screenText("item.back.label"), SceneRouter.MAIN_MENU_ROUTE));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return context.themedScene(ScreenShell.titled(
                ScreenTextResources.title(ScreenTextResources.DISPLAY_BINDINGS), scrollPane));
    }

    private static String format(String key, String bindingName, int value) {
        return ScreenTextResources.format(
                ScreenTextResources.DISPLAY_BINDINGS,
                key,
                Map.of(bindingName, Integer.toString(value)));
    }

    private static String screenText(String key) {
        return ScreenTextResources.text(ScreenTextResources.DISPLAY_BINDINGS, key);
    }
}
