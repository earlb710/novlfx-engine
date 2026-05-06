package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

/** Rectangle bounds used by authored animation clip and viewport targets. */
public record DisplayRectangleBounds(double x, double y, double width, double height) {
    public DisplayRectangleBounds {
        width = Validation.requireBetween(width, 0.0, Double.MAX_VALUE, "Animation rectangle width must be zero or positive.");
        height = Validation.requireBetween(height, 0.0, Double.MAX_VALUE, "Animation rectangle height must be zero or positive.");
    }
}
