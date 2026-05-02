package com.eb.javafx.save;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SaveSnapshotCodecTest {
    private final SaveSnapshotCodec<String> codec = new SaveSnapshotCodec<>() {
        @Override
        public String sectionId() {
            return "testSection";
        }

        @Override
        public int schemaVersion() {
            return 2;
        }

        @Override
        public String toJson(String snapshot) {
            return "{\"value\":\"" + snapshot + "\"}";
        }

        @Override
        public String fromJson(String json, String sourceName) {
            return sourceName + ":" + json;
        }
    };

    @Test
    void sectionRequiresIdentityVersionAndPayload() {
        SaveSnapshotSection section = new SaveSnapshotSection("section", 1, "{}");

        assertEquals("section", section.sectionId());
        assertEquals(1, section.schemaVersion());
        assertEquals("{}", section.payloadJson());
        assertThrows(IllegalArgumentException.class, () -> new SaveSnapshotSection("", 1, "{}"));
        assertThrows(IllegalArgumentException.class, () -> new SaveSnapshotSection("section", 0, "{}"));
        assertThrows(IllegalArgumentException.class, () -> new SaveSnapshotSection("section", 1, " "));
    }

    @Test
    void codecWrapsSnapshotAsVersionedSection() {
        SaveSnapshotSection section = codec.toSection("saved");

        assertEquals("testSection", section.sectionId());
        assertEquals(2, section.schemaVersion());
        assertEquals("{\"value\":\"saved\"}", section.payloadJson());
    }

    @Test
    void codecValidatesSectionMetadataBeforeLoadingPayload() {
        SaveSnapshotSection section = new SaveSnapshotSection("testSection", 2, "{}");

        assertEquals("testSection:{}", codec.fromSection(section));
        assertThrows(IllegalArgumentException.class, () -> codec.fromSection(null));
        assertThrows(IllegalArgumentException.class, () -> codec.fromSection(
                new SaveSnapshotSection("otherSection", 2, "{}")));
        assertThrows(IllegalArgumentException.class, () -> codec.fromSection(
                new SaveSnapshotSection("testSection", 3, "{}")));
    }
}
