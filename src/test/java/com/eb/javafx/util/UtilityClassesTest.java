package com.eb.javafx.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UtilityClassesTest {
    @Test
    void validationRejectsBlankAndOutOfRangeValues() {
        assertEquals("id", Validation.requireNonBlank("id", "required"));
        assertEquals(0.5, Validation.requireUnitInterval(0.5, "range"));
        assertEquals(1, Validation.requireSlot(1));

        assertThrows(IllegalArgumentException.class, () -> Validation.requireNonBlank(" ", "required"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireUnitInterval(1.5, "range"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireSlot(1000));
    }

    @Test
    void initializationGuardTracksLifecycle() {
        InitializationGuard guard = new InitializationGuard("service used before initialization");

        assertFalse(guard.isInitialized());
        assertThrows(IllegalStateException.class, guard::requireInitialized);

        guard.markInitialized();

        assertTrue(guard.isInitialized());
        guard.requireInitialized();
    }

    @Test
    void pathUtilsNormalizeAndProtectResolvedChildren() {
        Path root = Path.of("game");

        assertEquals("images/cg.png", PathUtils.normalizeSeparators("images\\cg.png"));
        assertEquals(Path.of("game", "images", "cg.png"), PathUtils.resolveChild(root, "images/cg.png"));
        assertEquals(".png", PathUtils.extensionLowercase(Path.of("CG.PNG")));
        assertEquals("cg.png", PathUtils.fileNameLowercase("images/CG.PNG"));
        assertThrows(IllegalArgumentException.class, () -> PathUtils.resolveChild(root, "../outside.png"));
    }

    @Test
    void jsonStringsQuoteNullableAndParseEscapes() {
        String quoted = JsonStrings.quote("Line\n\"quoted\"");

        assertEquals("\"Line\\n\\\"quoted\\\"\"", quoted);
        assertEquals("null", JsonStrings.nullableQuote(null));

        JsonStrings.ParsedString parsed = JsonStrings.parse("\"Line\\n\\u0041\" rest", 0);

        assertEquals("Line\nA", parsed.value());
        assertEquals(14, parsed.endIndex());
    }

    @Test
    void immutableCollectionsCopyDefensively() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("first", "1");
        Map<String, String> copy = ImmutableCollections.copyMap(source);
        source.put("second", "2");

        assertEquals(List.of("first"), List.copyOf(copy.keySet()));
        assertThrows(UnsupportedOperationException.class, () -> copy.put("third", "3"));
    }

    @Test
    void timeFormattingFormatsInstantsAndElapsedTime() {
        Instant start = Instant.parse("2026-05-01T15:00:00Z");
        Instant end = Instant.parse("2026-05-01T15:00:01.250Z");

        assertEquals("2026-05-01T15:00:00Z", TimeFormatting.formatInstant(start));
        assertEquals(start, TimeFormatting.parseInstant("2026-05-01T15:00:00Z"));
        assertEquals(1250, TimeFormatting.elapsedMillis(start, end));
        assertEquals("1250 ms", TimeFormatting.formatElapsedMillis(start, end));
    }

    @Test
    void resultRepresentsSuccessAndFailure() {
        Result<String> success = Result.success("ok");
        Result<String> failure = Result.failure("not ok");

        assertTrue(success.succeeded());
        assertEquals("ok", success.orElseThrow());
        assertTrue(failure.failed());
        assertEquals("fallback", failure.orElse("fallback"));
        assertThrows(IllegalStateException.class, failure::orElseThrow);
    }
}
