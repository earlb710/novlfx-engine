package com.eb.javafx.gallery;

import com.eb.javafx.routing.RouteContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Generic gallery screen that renders a grid of image entries, locked behind
 * {@code ProgressTracker} unlocks. Locked entries show a placeholder; unlocked entries
 * show the resolved image reference. Pass game-specific registry and gallery ID from
 * the route registration.
 */
public final class GalleryScreen {
    private static final String LOCKED_LABEL = "Not yet unlocked";

    private GalleryScreen() {}

    /**
     * Creates a gallery scene for the given registry and gallery ID.
     *
     * @param context   route context providing game state and theming
     * @param registry  registry containing the gallery definition
     * @param galleryId ID of the gallery to display
     */
    public static Scene createScene(RouteContext context, GalleryRegistry registry, String galleryId) {
        GalleryService service = new GalleryService(registry, context.gameState().progress());
        List<GalleryEntryViewModel> entries = service.viewModels(galleryId);

        TilePane grid = new TilePane(12, 12);
        grid.setPadding(new Insets(16));
        grid.setPrefColumns(3);
        entries.forEach(entry -> grid.getChildren().add(entryTile(entry)));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);

        BorderPane root = new BorderPane(scroll);
        return context.themedScene(root);
    }

    private static VBox entryTile(GalleryEntryViewModel entry) {
        VBox tile = new VBox(6);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(8));
        tile.getStyleClass().add("screen-panel");
        tile.setPrefWidth(200);
        tile.setPrefHeight(160);

        if (entry.unlocked() && entry.imageRef().isPresent()) {
            Label imgLabel = new Label("[" + entry.imageRef().get() + "]");
            imgLabel.setWrapText(true);
            imgLabel.setStyle("-fx-text-fill: #90ee90;");
            tile.getChildren().add(imgLabel);
        } else {
            Label locked = new Label(LOCKED_LABEL);
            locked.setWrapText(true);
            locked.setStyle("-fx-text-fill: #888;");
            tile.getChildren().add(locked);
        }

        Label caption = new Label(entry.captionTextKey());
        caption.setWrapText(true);
        tile.getChildren().add(caption);

        return tile;
    }
}
