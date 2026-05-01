package com.eb.javafx.audio;

/** Adapter boundary for app-owned JavaFX media playback of validated audio commands. */
public interface AudioPlaybackAdapter {
    /** Plays or replaces media on the command's channel using already validated volume and loop settings. */
    void play(AudioPlaybackCommand command);

    /** Stops any media currently owned by the named channel. */
    void stopChannel(String channelId);
}
