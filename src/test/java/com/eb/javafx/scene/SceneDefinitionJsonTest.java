package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SceneDefinitionJsonTest {
    @Test
    void importsSimpleSceneDefinitionsFromJson() {
        List<SceneDefinition> scenes = SceneDefinitionJson.fromJson("""
                {
                  "scenes": [
                    {
                      "id": "intro",
                      "metadata": {"chapter": "one"},
                      "steps": [
                        {"id": "line", "type": "DIALOGUE", "speakerId": "narrator", "textDefinition": "intro.line"},
                        {"id": "choice", "type": "CHOICE", "choices": [
                          {"id": "continue", "textDefinition": "intro.continue", "transition": {"type": "COMPLETE"}}
                        ]}
                      ]
                    }
                  ]
                }
                """, "test-scenes.json");

        assertEquals(1, scenes.size());
        SceneDefinition scene = scenes.get(0);
        assertEquals("intro", scene.id());
        assertEquals("one", scene.metadata().get("chapter"));
        assertEquals(SceneStepType.DIALOGUE, scene.steps().get(0).type());
        assertEquals("continue", scene.steps().get(1).choices().get(0).id());
        assertEquals(SceneTransitionType.COMPLETE, scene.steps().get(1).choices().get(0).transition().type());
    }

    @Test
    void exportsAndImportsSceneDefinitions() {
        SceneDefinition scene = new SceneDefinition("intro", List.of(), List.of(
                SceneStep.create("line", SceneStepType.NARRATION, null, "intro.line", null,
                        List.of(), List.of(), SceneTransition.complete(), Map.of("tone", "plain"))), Map.of());

        List<SceneDefinition> roundTrip = SceneDefinitionJson.fromJson(SceneDefinitionJson.toJson(scene), "round-trip");

        assertEquals("intro", roundTrip.get(0).id());
        assertEquals("plain", roundTrip.get(0).steps().get(0).metadata().get("tone"));
        assertEquals(SceneTransitionType.COMPLETE, roundTrip.get(0).steps().get(0).transition().type());
    }

    @Test
    void serializesSceneFlowStateSnapshots() {
        SceneFlowState state = new SceneFlowState(
                "intro",
                2,
                List.of(new SceneReturnPoint("caller", 4)),
                List.of("continue"),
                "overlay");

        SceneFlowState roundTrip = SceneFlowStateJson.fromJson(SceneFlowStateJson.toJson(state), "flow");

        assertEquals("intro", roundTrip.activeSceneId());
        assertEquals(2, roundTrip.stepIndex());
        assertEquals("caller", roundTrip.callStack().get(0).sceneId());
        assertEquals(List.of("continue"), roundTrip.selectedChoiceIds());
        assertEquals("overlay", roundTrip.pendingUiInterruption());
    }
}
