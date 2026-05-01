package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class StartupFailureExceptionTest {

    @Test
    void exceptionRetainsStartupFailureCategory() {
        RuntimeException cause = new RuntimeException("cause");

        StartupFailureException exception = new StartupFailureException(
                StartupFailureCategory.INVALID_CONTENT,
                "invalid content",
                cause);

        assertSame(StartupFailureCategory.INVALID_CONTENT, exception.category());
        assertEquals("invalid content", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("Invalid content", exception.category().displayName());
    }
}
