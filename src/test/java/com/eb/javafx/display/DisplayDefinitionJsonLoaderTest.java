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
    void loadsStructuredAndScriptedAnimationsFromJson() {
        ImageDisplayRegistry registry = new ImageDisplayRegistry();

        DisplayDefinitionJsonLoader.loadInto("""
                {
                  "animations": [
                    {
                      "id": "speaker.fade",
                      "repeatCount": 2,
                      "autoReverse": true,
                      "script": [
                        "pause 100",
                        "fade 200 opacity 0.5 ease_out"
                      ]
                    },
                    {
                      "id": "speaker.step",
                      "steps": [
                        {
                          "durationMillis": 300,
                          "pauseBeforeMillis": 25,
                          "targetOpacity": 0.8,
                          "targetScaleX": 1.1,
                          "targetScaleY": 1.2,
                          "targetTranslateX": 4,
                          "targetTranslateY": -2,
                          "targetRotate": 30,
                          "interpolation": "ease_both"
                        }
                      ]
                    }
                  ],
                  "animationScripts": [
                    "animation speaker.slide\\nmove 150 translateX 8 translateY 0 linear\\nend"
                  ]
                }
                """, "test-display.json", registry);

        assertEquals(3, registry.animations().size());
        assertEquals(2, registry.animation("speaker.fade").repeatCount());
        assertEquals(true, registry.animation("speaker.fade").autoReverse());
        assertEquals(100, registry.animation("speaker.fade").steps().get(0).pauseBeforeMillis());
        assertEquals(0.5, registry.animation("speaker.fade").steps().get(1).targetOpacity());
        assertEquals(25, registry.animation("speaker.step").steps().get(0).pauseBeforeMillis());
        assertEquals(30.0, registry.animation("speaker.step").steps().get(0).targetRotate());
        assertEquals(DisplayInterpolation.EASE_BOTH, registry.animation("speaker.step").steps().get(0).interpolation());
        assertEquals(8.0, registry.animation("speaker.slide").steps().get(0).targetTranslateX());
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

    @Test
    void rejectsDuplicateAnimationIdsFromJson() {
        ImageDisplayRegistry registry = new ImageDisplayRegistry();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                DisplayDefinitionJsonLoader.loadInto("""
                        {
                          "animations": [
                            {"id": "duplicate", "script": ["pause 1"]},
                            {"id": "duplicate", "script": ["pause 2"]}
                          ]
                        }
                        """, "test-display.json", registry));

        assertEquals("Duplicate display animation id: duplicate", exception.getMessage());
    }
}
