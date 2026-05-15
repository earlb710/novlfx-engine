package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.nio.file.Path;

/**
 * JavaFX renderer for {@link MainAppLayoutPlan main app layouts}.
 *
 * <p>The renderer composes a {@link StackPane} with three layered groups:</p>
 * <ol>
 *   <li><b>Background layer</b> — optional image and/or solid colour painted behind everything.</li>
 *   <li><b>Main frame layer</b> — a {@link BorderPane} whose centre holds the story and (optional)
 *       dialog screens split by the plan's ratio and orientation, and whose bottom holds an
 *       optional footer (the standard {@link ScreenShell#footerBar()}).</li>
 *   <li><b>Overlay layer</b> — one {@link StackPane} child per HUD overlay, positioned by anchor,
 *       absolute pixels, or fractional coordinates as specified by each
 *       {@link MainAppLayoutOverlay}.</li>
 * </ol>
 *
 * <p>The renderer never decides what a screen looks like; it asks the supplied
 * {@link MainAppScreenResolver} for each story, dialog, and overlay screen by id. Resolvers that
 * return {@code null} leave the corresponding slot empty.</p>
 */
public final class MainAppLayoutRenderer {
    private static final double OVERLAY_PADDING = 0.0;

    private MainAppLayoutRenderer() {
    }

    public static StackPane render(MainAppLayoutPlan plan, MainAppScreenResolver resolver) {
        return render(plan, resolver, null);
    }

    /**
     * Builds the JavaFX node tree for a main app layout plan.
     *
     * @param plan parsed layout plan
     * @param resolver supplies a JavaFX node for each referenced screen id
     * @param resourceRoot directory used to resolve a relative background-image path; may be
     *        {@code null} when the path is absolute or classpath-based
     * @return root pane that lays the background, frame, and overlays in order
     */
    public static StackPane render(MainAppLayoutPlan plan, MainAppScreenResolver resolver, Path resourceRoot) {
        Validation.requireNonNull(plan, "Main app layout plan is required.");
        Validation.requireNonNull(resolver, "Main app screen resolver is required.");

        StackPane root = new StackPane();
        root.getStyleClass().add("layout-main-app");
        root.setMinSize(0, 0);
        root.setAlignment(Pos.TOP_LEFT);

        Region backgroundLayer = backgroundLayer(plan, resourceRoot);
        if (backgroundLayer != null) {
            root.getChildren().add(backgroundLayer);
        }

        // Build overlay wrappers up-front so they can be parented into the story area when the
        // central frame is constructed. Anchoring overlays inside the story area means BOTTOM_*
        // alignments resolve to the bottom of the story slot — not the bottom of the whole frame —
        // and the dialog/footer bands can never be overlapped.
        java.util.Map<String, StackPane> overlayWrappersById = new java.util.LinkedHashMap<>();
        java.util.Map<String, MainAppLayoutOverlay> overlayPlansById = new java.util.LinkedHashMap<>();
        for (MainAppLayoutOverlay overlay : plan.overlays()) {
            if (!overlay.visible()) {
                continue;
            }
            Node content = resolver.resolve(overlay.screenId());
            if (content == null) {
                continue;
            }
            StackPane wrapper = overlayWrapper(overlay, content);
            overlayWrappersById.put(overlay.id(), wrapper);
            overlayPlansById.put(overlay.id(), overlay);
        }

        BorderPane frame = mainFrame(plan, resolver, overlayWrappersById.values());
        root.getChildren().add(frame);

        // Second pass: wire RELATIVE overlays now that all sibling wrappers exist in the story area.
        for (MainAppLayoutOverlay overlay : overlayPlansById.values()) {
            if (overlay.mode() != MainAppLayoutPlacementMode.RELATIVE) {
                continue;
            }
            StackPane wrapper = overlayWrappersById.get(overlay.id());
            StackPane anchor = overlayWrappersById.get(overlay.anchorBlockId());
            if (anchor == null) {
                // Anchor block is missing or hidden; leave the overlay at its top-left fallback so
                // the design still renders without the layout exploding.
                continue;
            }
            bindRelativePlacement(wrapper, anchor, overlay.anchor(), overlay.offsetX(), overlay.offsetY());
        }
        return root;
    }

    private static Region backgroundLayer(MainAppLayoutPlan plan, Path resourceRoot) {
        boolean hasImage = plan.backgroundImage() != null && !plan.backgroundImage().isBlank();
        boolean hasColor = plan.backgroundColor() != null && !plan.backgroundColor().isBlank();
        if (!hasImage && !hasColor) {
            return null;
        }
        Color color = hasColor ? Color.web(plan.backgroundColor()) : Color.TRANSPARENT;
        Image image = hasImage ? ScreenLayoutRenderer.loadBackgroundImage(plan.backgroundImage(), resourceRoot) : null;
        BackgroundLayer layer = new BackgroundLayer(image, plan.backgroundFit(), plan.backgroundOpacity(), color);
        layer.getStyleClass().add("layout-main-app-background");
        return layer;
    }

    private static BorderPane mainFrame(
            MainAppLayoutPlan plan,
            MainAppScreenResolver resolver,
            java.util.Collection<StackPane> overlays) {
        BorderPane frame = new BorderPane();
        frame.getStyleClass().add("layout-main-app-frame");
        frame.setMinSize(0, 0);
        // Transparent so the background image/colour shows through where the centre/footer don't paint.
        frame.setBackground(javafx.scene.layout.Background.EMPTY);

        Region centre = centerArea(plan, resolver, overlays);
        frame.setCenter(centre);

        if (plan.showFooter()) {
            HBox footer = ScreenShell.footerBar();
            frame.setBottom(footer);
            ScreenShell.pinFooterToBottom(frame);
        }
        return frame;
    }

    private static Region centerArea(
            MainAppLayoutPlan plan,
            MainAppScreenResolver resolver,
            java.util.Collection<StackPane> overlays) {
        Node storyRaw = wrappedSlot(resolver.resolve(plan.storyScreenId()), "layout-main-app-story");
        applyInsetPadding(storyRaw, plan.storyInsets());
        // The story area scrolls when its content overflows; horizontal scrolling is disabled so
        // the slot grows downward (vertical) or rightward inside its share, not sideways.
        ScrollPane story = scrollableStorySlot(storyRaw);
        // Wrap the story scroll plus the overlay wrappers in a StackPane that occupies exactly the
        // story slot. Overlay placement coordinates (alignment, pixels, percent, relative) are then
        // resolved against this StackPane's bounds, so BOTTOM_* anchors and 100%-y land at the
        // bottom of the story slot — never under the dialog or footer.
        StackPane storyArea = buildStoryArea(story, overlays);

        Node dialog = plan.dialogScreenId() == null
                ? null
                : wrappedSlot(resolver.resolve(plan.dialogScreenId()), "layout-main-app-dialog");
        if (dialog != null) {
            applyInsetPadding(dialog, plan.dialogInsets());
        }

        if (dialog == null) {
            // Story takes the whole centre when no dialog area is configured; overlays still live
            // inside the storyArea StackPane.
            return storyArea;
        }

        double dialogShare = 1.0 - plan.storyDialogRatio();
        if (plan.orientation() == MainAppLayoutOrientation.HORIZONTAL) {
            // Horizontal: story on the left, dialog pinned to the right at a proportional width.
            BorderPane centre = new BorderPane();
            centre.setMinSize(0, 0);
            centre.setBackground(javafx.scene.layout.Background.EMPTY);
            centre.setCenter(storyArea);
            centre.setRight(dialog);
            BorderPane.setAlignment(dialog, Pos.CENTER_RIGHT);
            pinSlotSize(dialog, centre.widthProperty().multiply(dialogShare), true);
            return centre;
        }
        // Vertical: story area scrolls, dialog stays pinned below it. Footer is added one level up
        // in mainFrame(), so the visible bottom-to-top stack is: footer (outer BorderPane.bottom),
        // dialog (this BorderPane.bottom), story area (this BorderPane.center, containing overlays).
        BorderPane centre = new BorderPane();
        centre.setMinSize(0, 0);
        centre.setBackground(javafx.scene.layout.Background.EMPTY);
        centre.setCenter(storyArea);
        centre.setBottom(dialog);
        BorderPane.setAlignment(dialog, Pos.BOTTOM_CENTER);
        pinSlotSize(dialog, centre.heightProperty().multiply(dialogShare), false);
        return centre;
    }

    private static StackPane buildStoryArea(ScrollPane story, java.util.Collection<StackPane> overlays) {
        StackPane storyArea = new StackPane();
        storyArea.getStyleClass().add("layout-main-app-story-area");
        storyArea.setMinSize(0, 0);
        storyArea.setAlignment(Pos.TOP_LEFT);
        // Make the scroll fill the storyArea; the overlays then sit on top of (or beside) it.
        story.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        storyArea.getChildren().add(story);
        storyArea.getChildren().addAll(overlays);
        return storyArea;
    }

    /**
     * Binds a slot Region's preferred dimension to the supplied size expression and pins
     * min/max to the same value so BorderPane treats it as a fixed-size band.
     */
    private static void pinSlotSize(Node slot, javafx.beans.binding.DoubleBinding size, boolean horizontal) {
        if (!(slot instanceof Region region)) {
            return;
        }
        if (horizontal) {
            region.prefWidthProperty().bind(size);
            region.minWidthProperty().bind(size);
            region.maxWidthProperty().bind(size);
        } else {
            region.prefHeightProperty().bind(size);
            region.minHeightProperty().bind(size);
            region.maxHeightProperty().bind(size);
        }
    }

    private static void applyInsetPadding(Node slot, MainAppLayoutInsets insets) {
        if (insets == null || insets.isEmpty()) {
            return;
        }
        if (slot instanceof Region region) {
            region.setPadding(new Insets(insets.top(), insets.right(), insets.bottom(), insets.left()));
        }
    }

    private static ScrollPane scrollableStorySlot(Node storyContent) {
        ScrollPane scroll = new ScrollPane(storyContent);
        scroll.setFitToWidth(true);
        // fitToHeight ensures short content stretches to fill the viewport; if a story screen has a
        // larger natural pref height than the viewport, the screen's own min height keeps it from
        // squishing and the scroll bar appears.
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinSize(0, 0);
        scroll.getStyleClass().add("layout-main-app-story-scroll");
        // Transparent so the layered background image/colour remains visible behind the story slot.
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return scroll;
    }

    private static Node wrappedSlot(Node content, String styleClass) {
        if (content == null) {
            StackPane placeholder = new StackPane();
            placeholder.getStyleClass().add(styleClass);
            placeholder.setMinSize(0, 0);
            return placeholder;
        }
        if (content instanceof Region region) {
            if (!region.getStyleClass().contains(styleClass)) {
                region.getStyleClass().add(styleClass);
            }
            return region;
        }
        StackPane wrapper = new StackPane(content);
        wrapper.getStyleClass().add(styleClass);
        wrapper.setMinSize(0, 0);
        return wrapper;
    }

    private static StackPane overlayWrapper(MainAppLayoutOverlay overlay, Node content) {
        StackPane wrapper = new StackPane(content);
        wrapper.getStyleClass().add("layout-main-app-overlay");
        wrapper.setId("overlay." + overlay.id());
        wrapper.setOpacity(overlay.opacity());
        wrapper.setPickOnBounds(false);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        if (overlay.preferredWidth() != null) {
            wrapper.setPrefWidth(overlay.preferredWidth());
        }
        if (overlay.preferredHeight() != null) {
            wrapper.setPrefHeight(overlay.preferredHeight());
        }
        applyPlacement(wrapper, overlay);
        return wrapper;
    }

    private static void applyPlacement(StackPane wrapper, MainAppLayoutOverlay overlay) {
        switch (overlay.mode()) {
            case ALIGNMENT -> {
                StackPane.setAlignment(wrapper, overlay.anchor().toJavaFxPos());
                double offsetX = overlay.offsetX();
                double offsetY = overlay.offsetY();
                StackPane.setMargin(wrapper, marginForAnchor(overlay.anchor(), offsetX, offsetY));
            }
            case PIXELS -> {
                StackPane.setAlignment(wrapper, Pos.TOP_LEFT);
                StackPane.setMargin(wrapper, new Insets(overlay.offsetY(), 0, 0, overlay.offsetX()));
            }
            case PERCENT -> {
                // Bind margins to parent size so the overlay stays at the requested fraction.
                StackPane.setAlignment(wrapper, Pos.TOP_LEFT);
                wrapper.parentProperty().addListener((observable, oldParent, newParent) -> {
                    if (newParent instanceof Region parentRegion) {
                        bindPercentMargin(wrapper, parentRegion, overlay.offsetX(), overlay.offsetY());
                    }
                });
                if (wrapper.getParent() instanceof Region parentRegion) {
                    bindPercentMargin(wrapper, parentRegion, overlay.offsetX(), overlay.offsetY());
                }
            }
            case RELATIVE -> {
                // Initial fallback placement; the render-time second pass replaces this with a
                // binding to the referenced anchor block's boundsInParent.
                StackPane.setAlignment(wrapper, Pos.TOP_LEFT);
                StackPane.setMargin(wrapper, Insets.EMPTY);
            }
        }
    }

    private static void bindRelativePlacement(
            StackPane wrapper,
            javafx.scene.Node anchor,
            MainAppLayoutAnchor relativeAnchor,
            double offsetX,
            double offsetY) {
        StackPane.setAlignment(wrapper, Pos.TOP_LEFT);
        Runnable update = () -> {
            javafx.geometry.Bounds anchorBounds = anchor.getBoundsInParent();
            double anchorWidth = anchorBounds.getWidth();
            double anchorHeight = anchorBounds.getHeight();
            double width = wrapper.getWidth();
            double height = wrapper.getHeight();
            double left;
            double top;
            switch (relativeAnchor) {
                case ABOVE -> {
                    left = anchorBounds.getMinX() + (anchorWidth - width) / 2.0 + offsetX;
                    top = anchorBounds.getMinY() - height + offsetY;
                }
                case BELOW -> {
                    left = anchorBounds.getMinX() + (anchorWidth - width) / 2.0 + offsetX;
                    top = anchorBounds.getMaxY() + offsetY;
                }
                case LEFT -> {
                    left = anchorBounds.getMinX() - width + offsetX;
                    top = anchorBounds.getMinY() + (anchorHeight - height) / 2.0 + offsetY;
                }
                case RIGHT -> {
                    left = anchorBounds.getMaxX() + offsetX;
                    top = anchorBounds.getMinY() + (anchorHeight - height) / 2.0 + offsetY;
                }
                default -> {
                    // Non-relative anchor on a RELATIVE overlay was already rejected by the record,
                    // so this branch is defensive only.
                    return;
                }
            }
            StackPane.setMargin(wrapper, new Insets(Math.max(0.0, top), 0, 0, Math.max(0.0, left)));
        };
        update.run();
        anchor.boundsInParentProperty().addListener((obs, oldB, newB) -> update.run());
        wrapper.widthProperty().addListener((obs, oldW, newW) -> update.run());
        wrapper.heightProperty().addListener((obs, oldH, newH) -> update.run());
    }

    private static void bindPercentMargin(StackPane wrapper, Region parent, double percentX, double percentY) {
        Runnable update = () -> {
            double left = parent.getWidth() * percentX;
            double top = parent.getHeight() * percentY;
            StackPane.setMargin(wrapper, new Insets(top, 0, 0, left));
        };
        update.run();
        parent.widthProperty().addListener((obs, oldW, newW) -> update.run());
        parent.heightProperty().addListener((obs, oldH, newH) -> update.run());
    }

    private static Insets marginForAnchor(MainAppLayoutAnchor anchor, double offsetX, double offsetY) {
        // Margin sides depend on which corner the anchor pulls toward. Top/bottom and left/right
        // offsets are symmetric for horizontal-centre anchors so the overlay sits on its anchor row
        // even when the user supplies a non-zero offsetX.
        double top = anchor == MainAppLayoutAnchor.TOP_LEFT
                || anchor == MainAppLayoutAnchor.TOP_CENTER
                || anchor == MainAppLayoutAnchor.TOP_RIGHT ? offsetY : 0.0;
        double bottom = anchor == MainAppLayoutAnchor.BOTTOM_LEFT
                || anchor == MainAppLayoutAnchor.BOTTOM_CENTER
                || anchor == MainAppLayoutAnchor.BOTTOM_RIGHT ? offsetY : 0.0;
        double left = anchor == MainAppLayoutAnchor.TOP_LEFT
                || anchor == MainAppLayoutAnchor.CENTER_LEFT
                || anchor == MainAppLayoutAnchor.BOTTOM_LEFT ? offsetX : 0.0;
        double right = anchor == MainAppLayoutAnchor.TOP_RIGHT
                || anchor == MainAppLayoutAnchor.CENTER_RIGHT
                || anchor == MainAppLayoutAnchor.BOTTOM_RIGHT ? offsetX : 0.0;
        if (top == 0.0 && bottom == 0.0 && left == 0.0 && right == 0.0) {
            return new Insets(OVERLAY_PADDING);
        }
        return new Insets(top, right, bottom, left);
    }

    /** Background image + colour layer that resizes with its parent and respects the chosen fit mode. */
    private static final class BackgroundLayer extends Region {
        private final javafx.scene.image.ImageView imageView;
        private final ScreenBackgroundFit fit;

        private BackgroundLayer(Image image, ScreenBackgroundFit fit, double opacity, Color canvasColor) {
            this.fit = fit;
            setMinSize(0, 0);
            setMouseTransparent(true);
            setFocusTraversable(false);
            setBorder(Border.EMPTY);
            setBackground(canvasColor.equals(Color.TRANSPARENT)
                    ? javafx.scene.layout.Background.EMPTY
                    : new javafx.scene.layout.Background(new BackgroundFill(canvasColor, CornerRadii.EMPTY, Insets.EMPTY)));
            if (image == null) {
                imageView = null;
                return;
            }
            imageView = new javafx.scene.image.ImageView(image);
            imageView.setMouseTransparent(true);
            imageView.setFocusTraversable(false);
            imageView.setSmooth(true);
            imageView.setOpacity(opacity);
            imageView.setManaged(false);
            getChildren().add(imageView);
        }

        @Override
        protected void layoutChildren() {
            if (imageView == null) {
                return;
            }
            double width = getWidth();
            double height = getHeight();
            switch (fit) {
                case STRETCH -> {
                    imageView.setPreserveRatio(false);
                    imageView.setFitWidth(width);
                    imageView.setFitHeight(height);
                    imageView.relocate(0, 0);
                }
                case CROP_CENTER -> {
                    Image image = imageView.getImage();
                    double imageWidth = image.getWidth();
                    double imageHeight = Math.max(image.getHeight(), 1.0);
                    double imageRatio = imageWidth / imageHeight;
                    double regionRatio = width / Math.max(height, 1.0);
                    imageView.setPreserveRatio(false);
                    double drawnWidth;
                    double drawnHeight;
                    if (imageRatio > regionRatio) {
                        // Image is proportionally wider; fit height and crop horizontally.
                        drawnHeight = height;
                        drawnWidth = imageWidth * height / imageHeight;
                    } else {
                        // Image is proportionally taller; fit width and crop vertically.
                        drawnWidth = width;
                        drawnHeight = imageHeight * width / Math.max(imageWidth, 1.0);
                    }
                    imageView.setFitWidth(drawnWidth);
                    imageView.setFitHeight(drawnHeight);
                    imageView.relocate((width - drawnWidth) / 2.0, (height - drawnHeight) / 2.0);
                }
            }
        }

        @Override
        protected double computePrefWidth(double height) {
            return 0;
        }

        @Override
        protected double computePrefHeight(double width) {
            return 0;
        }
    }
}
