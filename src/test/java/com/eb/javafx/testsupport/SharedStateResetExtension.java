package com.eb.javafx.testsupport;

import com.eb.javafx.text.DialogHistory;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Resets process-global engine state before each test so the suite is order-independent.
 *
 * <p>The conversation {@link DialogHistory} is installed as a first-wins shared singleton (by
 * {@code GameState} construction), so without a reset it accumulates across tests in the same JVM —
 * causing counts to drift (e.g. "expected 10 but was 20") and "a dialog history entry is already
 * open" when a prior test left one open. Reverting the shared instance to {@code null} before each
 * test lets the next {@code GameState} install a fresh history — the reset the engine's own docs say
 * tests should do in {@code @BeforeEach}, applied here once for the whole suite.</p>
 *
 * <p>Auto-registered via {@code junit-platform.properties}
 * ({@code junit.jupiter.extensions.autodetection.enabled=true}) +
 * {@code META-INF/services/org.junit.jupiter.api.extension.Extension}.</p>
 */
public final class SharedStateResetExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        DialogHistory.installShared(null);
    }
}
