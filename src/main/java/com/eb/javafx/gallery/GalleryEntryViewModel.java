package com.eb.javafx.gallery;

import java.util.Optional;

/** Read-only gallery entry view model: exposes image ref only when unlocked. */
public final class GalleryEntryViewModel {
    private final String id;
    private final String captionTextKey;
    private final boolean unlocked;
    private final String imageRef;

    GalleryEntryViewModel(String id, String captionTextKey, boolean unlocked, String imageRef) {
        this.id = id;
        this.captionTextKey = captionTextKey;
        this.unlocked = unlocked;
        this.imageRef = imageRef;
    }

    public String id() { return id; }
    public String captionTextKey() { return captionTextKey; }
    public boolean unlocked() { return unlocked; }
    public Optional<String> imageRef() { return Optional.ofNullable(unlocked ? imageRef : null); }
}
