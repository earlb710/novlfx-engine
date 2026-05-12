package com.eb.javafx.audio;

/** Policy applied to lower-priority channels when a higher-priority channel starts playing. */
public enum DuckingPolicy {
    NONE,
    REDUCE_TO_PERCENT,
    PAUSE
}
