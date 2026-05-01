package com.eb.javafx.util;

import java.util.Optional;

/** Lightweight success/failure value for operations that should not always throw. */
public final class Result<T> {
    private final T value;
    private final String failureMessage;
    private final Throwable cause;

    private Result(T value, String failureMessage, Throwable cause) {
        this.value = value;
        this.failureMessage = failureMessage;
        this.cause = cause;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null, null);
    }

    public static <T> Result<T> failure(String message) {
        return failure(message, null);
    }

    public static <T> Result<T> failure(String message, Throwable cause) {
        return new Result<>(null, Validation.requireNonBlank(message, "Failure message is required."), cause);
    }

    public boolean succeeded() {
        return failureMessage == null;
    }

    public boolean failed() {
        return !succeeded();
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public Optional<String> failureMessage() {
        return Optional.ofNullable(failureMessage);
    }

    public Optional<Throwable> cause() {
        return Optional.ofNullable(cause);
    }

    public T orElse(T fallback) {
        return succeeded() ? value : fallback;
    }

    public T orElseThrow() {
        if (succeeded()) {
            return value;
        }
        if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException(failureMessage, cause);
    }
}
