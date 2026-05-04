package com.eb.javafx.display;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DisplayDefinitionJsonLoaderTest {
    @Test
    void loadsGenericDisplayDefinitionsFromJson() {
        ImageDisplayRegistry registry = new ImageDisplayRegistry();
        registry.registerBaseDisplayContent();

        DisplayDefinitionJsonLoader.loadInto("""
                {
                  "transforms": [
                    {"id": "portrait", "fitWidth": 320, "fitHeight": 480, "opacity": 0.75, "xAlign": 0.5, "yAlign": 1.0}
                  ],
                  "images": [
                    {"id": "speaker.neutral", "sourcePath": "characters/speaker.png", "transformId": "portrait", "layer": "CHARACTER"}
                  ],
                  "layeredCharacters": [
                    {"id": "speaker", "drawOrder": ["speaker.neutral"], "defaultTransformId": "portrait", "metadata": {"kind": "demo"}}
                  ]
                }
                """, "test-display.json", registry);

        registry.validateDisplayContent();

        assertEquals(320, registry.transform("portrait").fitWidth());
        assertEquals(DisplayLayer.CHARACTER, registry.image("speaker.neutral").layer());
        assertEquals(List.of("speaker.neutral"), registry.layeredCharacter("speaker").drawOrder());
        assertEquals("demo", registry.layeredCharacter("speaker").metadata().get("kind"));
    }

    @Test
    void rejectsFractionalIntegerFields() {
        ImageDisplayRegistry registry = new ImageDisplayRegistry();

        assertThrows(IllegalArgumentException.class, () -> DisplayDefinitionJsonLoader.loadInto("""
                {
                  "transforms": [
                    {"id": "portrait", "fitWidth": 320.5, "fitHeight": 480}
                  ]
                }
                """, "test-display.json", registry));
    }
}
