package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

/** Signals which talking animation to play when a speaker delivers dialogue. */
public final class TalkingAnimationCue {
    private final String speakerId;
    private final String talkingAnimationId;

    public TalkingAnimationCue(String speakerId, String talkingAnimationId) {
        this.speakerId = Validation.requireNonBlank(speakerId, "TalkingAnimationCue speakerId is required.");
        this.talkingAnimationId = Validation.requireNonBlank(talkingAnimationId, "TalkingAnimationCue talkingAnimationId is required.");
    }

    public String speakerId() { return speakerId; }
    public String talkingAnimationId() { return talkingAnimationId; }
}
