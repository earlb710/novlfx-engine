package com.eb.javafx.audio;

/** Adapter-level lifecycle actions that concrete media implementations should support. */
public enum AudioPlaybackLifecycleEvent {
    PRELOAD,
    PLAY,
    STOP,
    FADE,
    CROSSFADE
}
