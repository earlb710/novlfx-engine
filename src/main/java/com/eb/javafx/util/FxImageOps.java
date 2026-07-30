package com.eb.javafx.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Game-agnostic pixel algorithms over JavaFX {@link Image}s: separable box blur, alpha
 * compositing, nearest-neighbour resample and linear blend.
 *
 * <p>These operate purely on {@code javafx.scene.image} types and carry no game state, so any
 * surface that composites or resamples in-memory maps (character-texture layering, icon tinting,
 * fabric baking, …) can share one implementation instead of re-deriving the same
 * {@code getArgb}/{@code setArgb} loops.</p>
 */
public final class FxImageOps {

    private FxImageOps() {
    }

    /**
     * In-place separable box blur of a single float channel laid out row-major as {@code w×h}.
     * Edges are clamped (nearest-pixel extension).
     *
     * @param v      channel data, {@code length == w*h}; mutated in place
     * @param w      width in pixels
     * @param h      height in pixels
     * @param radius blur radius (window size is {@code radius*2 + 1})
     */
    public static void boxBlur(float[] v, int w, int h, int radius) {
        float[] tmp = new float[v.length];
        int window = radius * 2 + 1;
        for (int y = 0; y < h; y++) {          // horizontal
            float sum = 0;
            for (int x = -radius; x <= radius; x++) sum += v[y * w + Math.min(w - 1, Math.max(0, x))];
            for (int x = 0; x < w; x++) {
                tmp[y * w + x] = sum / window;
                sum -= v[y * w + Math.min(w - 1, Math.max(0, x - radius))];
                sum += v[y * w + Math.min(w - 1, Math.max(0, x + radius + 1))];
            }
        }
        for (int x = 0; x < w; x++) {          // vertical
            float sum = 0;
            for (int y = -radius; y <= radius; y++) sum += tmp[Math.min(h - 1, Math.max(0, y)) * w + x];
            for (int y = 0; y < h; y++) {
                v[y * w + x] = sum / window;
                sum -= tmp[Math.min(h - 1, Math.max(0, y - radius)) * w + x];
                sum += tmp[Math.min(h - 1, Math.max(0, y + radius + 1)) * w + x];
            }
        }
    }

    /**
     * Composites an already-loaded alpha {@code overlay} over {@code base}, matching the overlay's
     * size first. Returns {@code base} unchanged when {@code overlay} is {@code null} or the
     * composite fails.
     *
     * @param base           opaque background map
     * @param overlay        alpha overlay (its own alpha × {@code opacityPercent} drives the blend)
     * @param opacityPercent overlay strength, 0–100
     * @return the composited image, or {@code base} on failure
     */
    public static Image composeImage(Image base, Image overlay, double opacityPercent) {
        if (overlay == null) return base;
        int od = (int) overlay.getWidth();
        Image baseSized = ((int) base.getWidth() == od && (int) base.getHeight() == od)
                ? base : scaleTo(base, od);
        Image composed = overlayAlpha(baseSized, overlay, opacityPercent / 100.0);
        return composed == null ? base : composed;
    }

    /**
     * Composites {@code over} onto {@code base} using {@code over}'s own alpha × {@code strength}
     * ({@code out = base*(1−a) + over*a}, {@code a = overlayAlpha·strength}). Same-size maps only;
     * the result is opaque.
     *
     * @return the composited image, or {@code null} if the maps are missing, errored or mismatched
     */
    public static Image overlayAlpha(Image base, Image over, double strength) {
        if (base == null || over == null || base.isError() || over.isError()) return null;
        int w = (int) base.getWidth(), h = (int) base.getHeight();
        if (w <= 0 || h <= 0 || (int) over.getWidth() != w || (int) over.getHeight() != h) return null;
        PixelReader rb = base.getPixelReader(), ro = over.getPixelReader();
        if (rb == null || ro == null) return null;
        double s = Math.max(0.0, Math.min(1.0, strength));
        WritableImage out = new WritableImage(w, h);
        PixelWriter wr = out.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int cb = rb.getArgb(x, y), co = ro.getArgb(x, y);
                double a = ((co >>> 24) & 0xff) / 255.0 * s;
                if (a <= 0.0) { wr.setArgb(x, y, 0xff000000 | (cb & 0xffffff)); continue; }
                int r = (int) (((cb >> 16) & 0xff) * (1 - a) + ((co >> 16) & 0xff) * a);
                int g = (int) (((cb >> 8) & 0xff) * (1 - a) + ((co >> 8) & 0xff) * a);
                int bl = (int) ((cb & 0xff) * (1 - a) + (co & 0xff) * a);
                wr.setArgb(x, y, 0xff000000 | (r << 16) | (g << 8) | bl);
            }
        }
        return out;
    }

    /**
     * Nearest-neighbour resample of {@code src} to {@code dim}×{@code dim} (square). Returns
     * {@code src} unchanged when it is already that size or cannot be read.
     */
    public static Image scaleTo(Image src, int dim) {
        int sw = (int) src.getWidth(), sh = (int) src.getHeight();
        if (sw == dim && sh == dim) return src;
        PixelReader r = src.getPixelReader();
        if (r == null || sw <= 0 || sh <= 0) return src;
        WritableImage out = new WritableImage(dim, dim);
        PixelWriter w = out.getPixelWriter();
        for (int y = 0; y < dim; y++) {
            int sy = Math.min(sh - 1, y * sh / dim);
            for (int x = 0; x < dim; x++) {
                w.setArgb(x, y, r.getArgb(Math.min(sw - 1, x * sw / dim), sy));
            }
        }
        return out;
    }

    /**
     * Per-pixel linear blend {@code a*(1−t) + b*t} (RGB; alpha forced opaque). Same-size maps only.
     *
     * @return the blended image, or {@code null} if the maps are missing, errored or mismatched
     */
    public static Image blendImages(Image a, Image b, double t) {
        if (a == null || b == null || a.isError() || b.isError()) return null;
        int w = (int) a.getWidth(), h = (int) a.getHeight();
        if (w <= 0 || h <= 0 || (int) b.getWidth() != w || (int) b.getHeight() != h) return null;
        PixelReader ra = a.getPixelReader(), rb = b.getPixelReader();
        if (ra == null || rb == null) return null;
        double s = Math.max(0.0, Math.min(1.0, t));
        WritableImage out = new WritableImage(w, h);
        PixelWriter wr = out.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int ca = ra.getArgb(x, y), cb = rb.getArgb(x, y);
                int r = (int) (((ca >> 16) & 0xff) * (1 - s) + ((cb >> 16) & 0xff) * s);
                int g = (int) (((ca >> 8) & 0xff) * (1 - s) + ((cb >> 8) & 0xff) * s);
                int bl = (int) ((ca & 0xff) * (1 - s) + (cb & 0xff) * s);
                wr.setArgb(x, y, 0xff000000 | (r << 16) | (g << 8) | bl);
            }
        }
        return out;
    }
}
