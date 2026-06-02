package com.eb.javafx.ui;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Renders an {@link HBox} of one icon repeated at a set of per-icon opacities — a "N at opacity"
 * badge, e.g. a meter drawn as a row of hearts where the trailing icon's opacity shows a fractional
 * bucket. The resource path, opacity array, and sizing are all supplied by the caller; the icon
 * image itself is produced by an injected {@link IconRasteriser}, so this stays free of any
 * particular asset pipeline or domain stat model.
 *
 * <p>The badge rasterises at {@code sizePx × }{@link #HIDPI_FACTOR} and displays at {@code sizePx}
 * for crisp output on high-density displays. Threading: JavaFX application thread only.</p>
 */
public final class RepeatedIconBadge {

    /** Default per-icon edge length in CSS pixels. */
    public static final int DEFAULT_ICON_SIZE_PX = 18;

    /** Default horizontal gap between icons. */
    public static final int DEFAULT_HORIZONTAL_GAP_PX = 2;

    /** Hi-DPI multiplier applied to the rasterisation request: the icon is rasterised at
     *  {@code size × HIDPI_FACTOR} then scaled down by the {@link ImageView}. */
    public static final int HIDPI_FACTOR = 2;

    /** Produces the icon image for {@code resourcePath} at the requested raster size (px), or
     *  {@code null} when it can't be loaded. */
    @FunctionalInterface
    public interface IconRasteriser {
        Image rasterise(String resourcePath, int rasterPx);
    }

    private RepeatedIconBadge() {
    }

    /**
     * Builds a badge: one icon from {@code resourcePath} repeated once per entry in
     * {@code opacities}, each {@link ImageView} set to its entry's opacity. Returns an empty
     * {@link HBox} when {@code resourcePath} is null, {@code opacities} is empty, or the rasteriser
     * returns null. A negative {@code gapPx} intentionally overlaps adjacent icons.
     */
    public static HBox build(String resourcePath, double[] opacities, int iconSizePx, int gapPx,
            IconRasteriser rasteriser) {
        HBox box = new HBox(gapPx);
        box.setAlignment(Pos.CENTER_LEFT);
        if (resourcePath == null || opacities == null || opacities.length == 0) {
            return box;
        }
        Image image = rasteriser.rasterise(resourcePath, iconSizePx * HIDPI_FACTOR);
        if (image == null) {
            return box;
        }
        for (double opacity : opacities) {
            ImageView view = buildImageView(image, iconSizePx);
            view.setOpacity(opacity);
            box.getChildren().add(view);
        }
        return box;
    }

    /** Returns a single icon as an {@link ImageView}, or {@code null} when {@code resourcePath} is
     *  null or the rasteriser returns null. */
    public static ImageView singleIcon(String resourcePath, int iconSizePx, IconRasteriser rasteriser) {
        if (resourcePath == null) {
            return null;
        }
        Image image = rasteriser.rasterise(resourcePath, iconSizePx * HIDPI_FACTOR);
        return image == null ? null : buildImageView(image, iconSizePx);
    }

    private static ImageView buildImageView(Image image, int iconSizePx) {
        ImageView view = new ImageView(image);
        view.setFitWidth(iconSizePx);
        view.setFitHeight(iconSizePx);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }
}
