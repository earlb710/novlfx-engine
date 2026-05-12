package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;
import java.util.Optional;

/** Named clickable region on a hotspot map with fractional bounds, optional condition, and scene target. */
public final class HotspotDefinition {
    private final String id;
    private final String labelTextKey;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final String conditionExpression;
    private final String targetSceneId;

    public HotspotDefinition(String id, String labelTextKey, double x, double y,
                              double width, double height, String conditionExpression, String targetSceneId) {
        this.id = Validation.requireNonBlank(id, "Hotspot id is required.");
        this.labelTextKey = Validation.requireNonBlank(labelTextKey, "Hotspot labelTextKey is required.");
        if (x < 0 || x > 1 || y < 0 || y > 1 || width <= 0 || width > 1 || height <= 0 || height > 1) {
            throw new IllegalArgumentException(
                "Hotspot bounds must be fractional (x,y: 0.0-1.0; width,height: >0 and <=1.0).");
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.conditionExpression = conditionExpression;
        this.targetSceneId = Validation.requireNonBlank(targetSceneId, "Hotspot targetSceneId is required.");
    }

    public String id() { return id; }
    public String labelTextKey() { return labelTextKey; }
    public double x() { return x; }
    public double y() { return y; }
    public double width() { return width; }
    public double height() { return height; }
    public Optional<String> conditionExpression() { return Optional.ofNullable(conditionExpression); }
    public String targetSceneId() { return targetSceneId; }
}
