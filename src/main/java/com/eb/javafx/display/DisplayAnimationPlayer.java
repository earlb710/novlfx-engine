package com.eb.javafx.display;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Rectangle2D;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JavaFX timelines from reusable ATL-style animation profiles.
 *
 * <p>The player converts validated display animation steps into JavaFX key frames.
 * Reduced-motion callers receive a zero-duration timeline after the node is moved
 * directly to the final state.</p>
 */
public final class DisplayAnimationPlayer {

    /**
     * Builds a JavaFX timeline for the supplied node and animation profile.
     *
     * @param node node whose opacity, scale, translation, rotation, clip, viewport, and effect properties are animated
     * @param animation validated non-empty animation profile
     * @param reducedMotion when true, skips interpolation and applies the final step immediately
     * @return timeline configured with repeat count and auto-reverse behavior
     */
    public Timeline buildTimeline(Node node, DisplayAnimation animation, boolean reducedMotion) {
        if (reducedMotion) {
            applyFinalState(node, animation.steps().get(animation.steps().size() - 1));
            return new Timeline(new KeyFrame(Duration.ZERO));
        }

        List<KeyFrame> keyFrames = new ArrayList<>();
        Duration elapsed = Duration.ZERO;
        Rectangle clipRectangle = configureClip(node, animation);
        ViewportAnimation viewportAnimation = configureViewport(node, animation);
        EffectAnimation effectAnimation = configureEffects(node, animation);
        List<KeyValue> initialValues = baseKeyValues(node);
        addInitialClipValues(initialValues, clipRectangle);
        addInitialViewportValues(initialValues, viewportAnimation);
        addInitialEffectValues(initialValues, effectAnimation);
        keyFrames.add(new KeyFrame(Duration.ZERO, initialValues.toArray(KeyValue[]::new)));

        for (DisplayAnimationStep step : animation.steps()) {
            elapsed = elapsed.add(Duration.millis(step.pauseBeforeMillis()));
            elapsed = elapsed.add(Duration.millis(step.durationMillis()));
            List<KeyValue> values = stepKeyValues(node, step);
            addClipValues(values, clipRectangle, step);
            addViewportValues(values, viewportAnimation, step);
            addEffectValues(values, effectAnimation, step);
            keyFrames.add(new KeyFrame(elapsed, values.toArray(KeyValue[]::new)));
        }

        Timeline timeline = new Timeline(keyFrames.toArray(KeyFrame[]::new));
        timeline.setCycleCount(animation.repeatCount());
        timeline.setAutoReverse(animation.autoReverse());
        return timeline;
    }

    private void applyFinalState(Node node, DisplayAnimationStep step) {
        node.setOpacity(step.targetOpacity());
        node.setScaleX(step.targetScaleX());
        node.setScaleY(step.targetScaleY());
        node.setTranslateX(step.targetTranslateX());
        node.setTranslateY(step.targetTranslateY());
        node.setRotate(step.targetRotate());
        if (step.targetClipBounds() != null) {
            node.setClip(rectangle(step.targetClipBounds()));
        }
        if (step.targetViewportBounds() != null && node instanceof ImageView imageView) {
            imageView.setViewport(rectangle2D(step.targetViewportBounds()));
        }
        if (step.targetEffects().hasAnyEffect()) {
            EffectAnimation effects = new EffectAnimation(new GaussianBlur(), new DropShadow(), new ColorAdjust());
            chainEffects(node, effects);
            applyEffects(effects, step.targetEffects());
        }
    }

    private List<KeyValue> baseKeyValues(Node node) {
        List<KeyValue> values = new ArrayList<>();
        values.add(new KeyValue(node.opacityProperty(), node.getOpacity()));
        values.add(new KeyValue(node.scaleXProperty(), node.getScaleX()));
        values.add(new KeyValue(node.scaleYProperty(), node.getScaleY()));
        values.add(new KeyValue(node.translateXProperty(), node.getTranslateX()));
        values.add(new KeyValue(node.translateYProperty(), node.getTranslateY()));
        values.add(new KeyValue(node.rotateProperty(), node.getRotate()));
        return values;
    }

    private List<KeyValue> stepKeyValues(Node node, DisplayAnimationStep step) {
        List<KeyValue> values = new ArrayList<>();
        values.add(new KeyValue(node.opacityProperty(), step.targetOpacity(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(node.scaleXProperty(), step.targetScaleX(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(node.scaleYProperty(), step.targetScaleY(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(node.translateXProperty(), step.targetTranslateX(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(node.translateYProperty(), step.targetTranslateY(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(node.rotateProperty(), step.targetRotate(), step.interpolation().toJavaFxInterpolator()));
        return values;
    }

    private Rectangle configureClip(Node node, DisplayAnimation animation) {
        DisplayRectangleBounds firstTarget = firstClipTarget(animation);
        if (firstTarget == null) {
            return null;
        }
        if (node.getClip() instanceof Rectangle rectangle) {
            return rectangle;
        }
        Rectangle rectangle = rectangle(firstTarget);
        node.setClip(rectangle);
        return rectangle;
    }

    private DisplayRectangleBounds firstClipTarget(DisplayAnimation animation) {
        for (DisplayAnimationStep step : animation.steps()) {
            if (step.targetClipBounds() != null) {
                return step.targetClipBounds();
            }
        }
        return null;
    }

    private void addInitialClipValues(List<KeyValue> values, Rectangle clipRectangle) {
        if (clipRectangle == null) {
            return;
        }
        values.add(new KeyValue(clipRectangle.xProperty(), clipRectangle.getX()));
        values.add(new KeyValue(clipRectangle.yProperty(), clipRectangle.getY()));
        values.add(new KeyValue(clipRectangle.widthProperty(), clipRectangle.getWidth()));
        values.add(new KeyValue(clipRectangle.heightProperty(), clipRectangle.getHeight()));
    }

    private void addClipValues(List<KeyValue> values, Rectangle clipRectangle, DisplayAnimationStep step) {
        if (clipRectangle == null || step.targetClipBounds() == null) {
            return;
        }
        DisplayRectangleBounds bounds = step.targetClipBounds();
        values.add(new KeyValue(clipRectangle.xProperty(), bounds.x(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(clipRectangle.yProperty(), bounds.y(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(clipRectangle.widthProperty(), bounds.width(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(clipRectangle.heightProperty(), bounds.height(), step.interpolation().toJavaFxInterpolator()));
    }

    private ViewportAnimation configureViewport(Node node, DisplayAnimation animation) {
        if (!(node instanceof ImageView imageView)) {
            return null;
        }
        DisplayRectangleBounds firstTarget = firstViewportTarget(animation);
        if (firstTarget == null) {
            return null;
        }
        Rectangle2D current = imageView.getViewport();
        Rectangle proxy = current == null
                ? rectangle(firstTarget)
                : new Rectangle(current.getMinX(), current.getMinY(), current.getWidth(), current.getHeight());
        proxy.xProperty().addListener((observable, oldValue, newValue) -> applyViewport(imageView, proxy));
        proxy.yProperty().addListener((observable, oldValue, newValue) -> applyViewport(imageView, proxy));
        proxy.widthProperty().addListener((observable, oldValue, newValue) -> applyViewport(imageView, proxy));
        proxy.heightProperty().addListener((observable, oldValue, newValue) -> applyViewport(imageView, proxy));
        applyViewport(imageView, proxy);
        return new ViewportAnimation(proxy);
    }

    private DisplayRectangleBounds firstViewportTarget(DisplayAnimation animation) {
        for (DisplayAnimationStep step : animation.steps()) {
            if (step.targetViewportBounds() != null) {
                return step.targetViewportBounds();
            }
        }
        return null;
    }

    private void addInitialViewportValues(List<KeyValue> values, ViewportAnimation viewportAnimation) {
        if (viewportAnimation == null) {
            return;
        }
        addInitialClipValues(values, viewportAnimation.proxy());
    }

    private void addViewportValues(List<KeyValue> values, ViewportAnimation viewportAnimation, DisplayAnimationStep step) {
        if (viewportAnimation == null || step.targetViewportBounds() == null) {
            return;
        }
        DisplayRectangleBounds bounds = step.targetViewportBounds();
        Rectangle proxy = viewportAnimation.proxy();
        values.add(new KeyValue(proxy.xProperty(), bounds.x(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(proxy.yProperty(), bounds.y(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(proxy.widthProperty(), bounds.width(), step.interpolation().toJavaFxInterpolator()));
        values.add(new KeyValue(proxy.heightProperty(), bounds.height(), step.interpolation().toJavaFxInterpolator()));
    }

    private EffectAnimation configureEffects(Node node, DisplayAnimation animation) {
        if (!hasEffects(animation)) {
            return null;
        }
        EffectAnimation effects = new EffectAnimation(new GaussianBlur(), new DropShadow(), new ColorAdjust());
        chainEffects(node, effects);
        return effects;
    }

    private boolean hasEffects(DisplayAnimation animation) {
        for (DisplayAnimationStep step : animation.steps()) {
            if (step.targetEffects().hasAnyEffect()) {
                return true;
            }
        }
        return false;
    }

    private void chainEffects(Node node, EffectAnimation effects) {
        effects.dropShadow().setInput(effects.colorAdjust());
        effects.blur().setInput(effects.dropShadow());
        node.setEffect(effects.blur());
    }

    private void addInitialEffectValues(List<KeyValue> values, EffectAnimation effects) {
        if (effects == null) {
            return;
        }
        values.add(new KeyValue(effects.blur().radiusProperty(), effects.blur().getRadius()));
        values.add(new KeyValue(effects.dropShadow().radiusProperty(), effects.dropShadow().getRadius()));
        values.add(new KeyValue(effects.dropShadow().offsetXProperty(), effects.dropShadow().getOffsetX()));
        values.add(new KeyValue(effects.dropShadow().offsetYProperty(), effects.dropShadow().getOffsetY()));
        values.add(new KeyValue(effects.colorAdjust().hueProperty(), effects.colorAdjust().getHue()));
        values.add(new KeyValue(effects.colorAdjust().saturationProperty(), effects.colorAdjust().getSaturation()));
        values.add(new KeyValue(effects.colorAdjust().brightnessProperty(), effects.colorAdjust().getBrightness()));
        values.add(new KeyValue(effects.colorAdjust().contrastProperty(), effects.colorAdjust().getContrast()));
    }

    private void addEffectValues(List<KeyValue> values, EffectAnimation effects, DisplayAnimationStep step) {
        if (effects == null || !step.targetEffects().hasAnyEffect()) {
            return;
        }
        DisplayEffectTargets targets = step.targetEffects();
        if (targets.blurEnabled()) {
            values.add(new KeyValue(effects.blur().radiusProperty(), targets.blurRadius(), step.interpolation().toJavaFxInterpolator()));
        }
        if (targets.dropShadowEnabled()) {
            values.add(new KeyValue(effects.dropShadow().radiusProperty(), targets.dropShadowRadius(), step.interpolation().toJavaFxInterpolator()));
            values.add(new KeyValue(effects.dropShadow().offsetXProperty(), targets.dropShadowOffsetX(), step.interpolation().toJavaFxInterpolator()));
            values.add(new KeyValue(effects.dropShadow().offsetYProperty(), targets.dropShadowOffsetY(), step.interpolation().toJavaFxInterpolator()));
        }
        if (targets.colorAdjustEnabled()) {
            values.add(new KeyValue(effects.colorAdjust().hueProperty(), targets.colorAdjustHue(), step.interpolation().toJavaFxInterpolator()));
            values.add(new KeyValue(effects.colorAdjust().saturationProperty(), targets.colorAdjustSaturation(), step.interpolation().toJavaFxInterpolator()));
            values.add(new KeyValue(effects.colorAdjust().brightnessProperty(), targets.colorAdjustBrightness(), step.interpolation().toJavaFxInterpolator()));
            values.add(new KeyValue(effects.colorAdjust().contrastProperty(), targets.colorAdjustContrast(), step.interpolation().toJavaFxInterpolator()));
        }
    }

    private void applyEffects(EffectAnimation effects, DisplayEffectTargets targets) {
        if (targets.blurEnabled()) {
            effects.blur().setRadius(targets.blurRadius());
        }
        if (targets.dropShadowEnabled()) {
            effects.dropShadow().setRadius(targets.dropShadowRadius());
            effects.dropShadow().setOffsetX(targets.dropShadowOffsetX());
            effects.dropShadow().setOffsetY(targets.dropShadowOffsetY());
        }
        if (targets.colorAdjustEnabled()) {
            effects.colorAdjust().setHue(targets.colorAdjustHue());
            effects.colorAdjust().setSaturation(targets.colorAdjustSaturation());
            effects.colorAdjust().setBrightness(targets.colorAdjustBrightness());
            effects.colorAdjust().setContrast(targets.colorAdjustContrast());
        }
    }

    private void applyViewport(ImageView imageView, Rectangle proxy) {
        imageView.setViewport(new Rectangle2D(proxy.getX(), proxy.getY(), proxy.getWidth(), proxy.getHeight()));
    }

    private Rectangle rectangle(DisplayRectangleBounds bounds) {
        return new Rectangle(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private Rectangle2D rectangle2D(DisplayRectangleBounds bounds) {
        return new Rectangle2D(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private record ViewportAnimation(Rectangle proxy) {
    }

    private record EffectAnimation(GaussianBlur blur, DropShadow dropShadow, ColorAdjust colorAdjust) {
    }
}
