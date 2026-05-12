package com.eb.javafx.scene;

/** Evaluated hotspot option ready for UI rendering, with enabled state reflecting condition evaluation. */
public final class HotspotOptionViewModel {
    private final String id;
    private final String labelTextKey;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final boolean enabled;

    public HotspotOptionViewModel(String id, String labelTextKey, double x, double y,
                                   double width, double height, boolean enabled) {
        this.id = id;
        this.labelTextKey = labelTextKey;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.enabled = enabled;
    }

    public String id() { return id; }
    public String labelTextKey() { return labelTextKey; }
    public double x() { return x; }
    public double y() { return y; }
    public double width() { return width; }
    public double height() { return height; }
    public boolean enabled() { return enabled; }
}
