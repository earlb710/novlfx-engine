package com.eb.javafx.scene;

import java.util.List;

/** Evaluated hotspot map ready for UI rendering, with resolved background image and evaluated option states. */
public final class HotspotMapViewModel {
    private final String backgroundImageRef;
    private final List<HotspotOptionViewModel> options;

    public HotspotMapViewModel(String backgroundImageRef, List<HotspotOptionViewModel> options) {
        this.backgroundImageRef = backgroundImageRef;
        this.options = List.copyOf(options);
    }

    public String backgroundImageRef() { return backgroundImageRef; }
    public List<HotspotOptionViewModel> options() { return options; }
}
