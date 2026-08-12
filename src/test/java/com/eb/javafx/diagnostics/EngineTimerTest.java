package com.eb.javafx.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The debug stopwatch — measurement, freeze-on-stop, the one-shot wrappers, and the global mute.
 *  Deliberately avoids asserting exact durations (that would be flaky). */
class EngineTimerTest {

    @Test
    void measuresNonNegativeElapsedAndFreezesOnStop() {
        EngineTimer t = EngineTimer.start("probe");
        assertTrue(t.elapsedMillis() >= 0);
        assertTrue(t.elapsedNanos() >= 0);
        long stopped = t.stop();
        assertTrue(stopped >= 0);
        // Frozen: the elapsed no longer advances after stop, so a second read equals the first.
        assertEquals(stopped, t.elapsedMillis());
    }

    @Test
    void stopIsIdempotent() {
        EngineTimer t = EngineTimer.start("probe");
        long first = t.stop();
        assertEquals(first, t.stop(), "a second stop returns the same frozen elapsed");
        t.close();   // close() == stop(), still fine
        assertEquals(first, t.elapsedMillis());
    }

    @Test
    void oneShotWrappersRunAndReturn() {
        boolean[] ran = {false};
        EngineTimer.time("run", () -> ran[0] = true);
        assertTrue(ran[0]);
        assertEquals("value", EngineTimer.time("supply", () -> "value"));
    }

    @Test
    void globalMuteKeepsMeasuringAndChaining() {
        boolean previous = EngineTimer.isEnabled();
        try {
            EngineTimer.setEnabled(false);
            assertFalse(EngineTimer.isEnabled());
            EngineTimer t = EngineTimer.start("probe");
            assertSame(t, t.warnOver(100));   // chainable
            assertSame(t, t.lap("phase"));    // chainable, no output when muted
            assertTrue(t.stop() >= 0);        // still measures while muted
        } finally {
            EngineTimer.setEnabled(previous);
        }
    }
}
