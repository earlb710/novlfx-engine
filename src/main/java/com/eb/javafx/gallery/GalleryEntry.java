package com.eb.javafx.gallery;

import com.eb.javafx.util.Validation;

/** One viewable image in a gallery, gated behind a progress unlock. */
public final class GalleryEntry {
    private final String id;
    private final String imageRef;
    private final String captionTextKey;
    private final String requiredUnlockId;

    public GalleryEntry(String id, String imageRef, String captionTextKey, String requiredUnlockId) {
        this.id = Validation.requireNonBlank(id, "Gallery entry id is required.");
        this.imageRef = Validation.requireNonBlank(imageRef, "Gallery entry imageRef is required.");
        this.captionTextKey = Validation.requireNonBlank(captionTextKey, "Gallery entry captionTextKey is required.");
        this.requiredUnlockId = Validation.requireNonBlank(requiredUnlockId, "Gallery entry requiredUnlockId is required.");
    }

    public String id() { return id; }
    public String imageRef() { return imageRef; }
    public String captionTextKey() { return captionTextKey; }
    public String requiredUnlockId() { return requiredUnlockId; }
}
