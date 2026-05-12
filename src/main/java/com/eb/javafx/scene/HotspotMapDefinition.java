package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;
import java.util.List;

/** Named background map with a list of clickable hotspot regions. */
public final class HotspotMapDefinition {
    private final String id;
    private final String backgroundImageRef;
    private final List<HotspotDefinition> hotspots;

    public HotspotMapDefinition(String id, String backgroundImageRef, List<HotspotDefinition> hotspots) {
        this.id = Validation.requireNonBlank(id, "HotspotMap id is required.");
        this.backgroundImageRef = Validation.requireNonBlank(backgroundImageRef, "HotspotMap backgroundImageRef is required.");
        this.hotspots = List.copyOf(hotspots);
    }

    public String id() { return id; }
    public String backgroundImageRef() { return backgroundImageRef; }
    public List<HotspotDefinition> hotspots() { return hotspots; }
}
