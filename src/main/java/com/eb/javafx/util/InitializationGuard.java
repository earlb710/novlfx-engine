package com.eb.javafx.util;

/** Tracks and enforces initialization for services with explicit startup lifecycles. */
public final class InitializationGuard {
    private final String uninitializedMessage;
    private boolean initialized;

    public InitializationGuard(String uninitializedMessage) {
        this.uninitializedMessage = Validation.requireNonBlank(uninitializedMessage, "Initialization message is required.");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void markInitialized() {
        initialized = true;
    }

    public void markUninitialized() {
        initialized = false;
    }

    public void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException(uninitializedMessage);
        }
    }
}
