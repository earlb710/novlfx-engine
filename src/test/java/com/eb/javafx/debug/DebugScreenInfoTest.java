package com.eb.javafx.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DebugScreenInfoTest {
    @Test
    void normalizesNullFieldsToEmptyStrings() {
        DebugScreenInfo info = new DebugScreenInfo(null, null, null);

        assertEquals("", info.routeId());
        assertEquals("", info.screenClass());
        assertEquals("", info.jsonFilePath());
    }

    @Test
    void emptyFactoryProducesAllBlankFields() {
        DebugScreenInfo info = DebugScreenInfo.empty();

        assertEquals("", info.routeId());
        assertEquals("", info.screenClass());
        assertEquals("", info.jsonFilePath());
    }

    @Test
    void forRouteRetainsRouteIdAndLeavesOtherFieldsBlank() {
        DebugScreenInfo info = DebugScreenInfo.forRoute("main-menu");

        assertEquals("main-menu", info.routeId());
        assertEquals("", info.screenClass());
        assertEquals("", info.jsonFilePath());
    }

    @Test
    void withScreenClassReplacesOnlyScreenClass() {
        DebugScreenInfo info = DebugScreenInfo.forRoute("preferences").withScreenClass("com.eb.Foo");

        assertEquals("preferences", info.routeId());
        assertEquals("com.eb.Foo", info.screenClass());
        assertEquals("", info.jsonFilePath());
    }

    @Test
    void withJsonFilePathReplacesOnlyJsonFilePath() {
        DebugScreenInfo info = DebugScreenInfo.forRoute("save-load")
                .withScreenClass("com.eb.Bar")
                .withJsonFilePath("/path/to/screen.json");

        assertEquals("save-load", info.routeId());
        assertEquals("com.eb.Bar", info.screenClass());
        assertEquals("/path/to/screen.json", info.jsonFilePath());
    }
}
