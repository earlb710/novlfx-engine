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

    @Test
    void registryComposesMigratesAndRoundTripsSnapshotSections() {
        SaveSnapshotRegistry registry = new SaveSnapshotRegistry();
        registry.registerRequired("scene", 2, section ->
                new SaveSnapshotSection(section.sectionId(), 2, section.payloadJson() + "-migrated"));
        registry.registerOptional("prefs", 1);

        SaveSnapshotDocument document = registry.compose(java.util.List.of(
                new SaveSnapshotSection("scene", 1, "{\"scene\":\"intro\"}"),
                new SaveSnapshotSection("prefs", 1, "{\"font\":\"Serif\"}")));

        assertEquals(2, document.sections().size());
        assertEquals(2, document.section("scene").orElseThrow().schemaVersion());
        assertEquals("{\"scene\":\"intro\"}-migrated", document.section("scene").orElseThrow().payloadJson());

        SaveSnapshotDocument reparsed = SaveSnapshotDocument.fromJson(document.toJson(), "snapshot");
        assertEquals(document.sections().size(), reparsed.sections().size());
        assertEquals(document.section("prefs").orElseThrow().payloadJson(), reparsed.section("prefs").orElseThrow().payloadJson());
        assertEquals(2, registry.decompose(reparsed).size());
    }

    @Test
    void registryRejectsMissingRequiredDuplicateAndUnsupportedSections() {
        SaveSnapshotRegistry registry = new SaveSnapshotRegistry();
        registry.registerRequired("scene", 2);

        assertThrows(IllegalArgumentException.class, () -> registry.compose(java.util.List.of()));
        assertThrows(IllegalArgumentException.class, () -> registry.compose(java.util.List.of(
                new SaveSnapshotSection("scene", 2, "{}"),
                new SaveSnapshotSection("scene", 2, "{\"other\":true}"))));
        assertThrows(IllegalArgumentException.class, () -> registry.compose(java.util.List.of(
                new SaveSnapshotSection("scene", 1, "{}"))));
    }
}
