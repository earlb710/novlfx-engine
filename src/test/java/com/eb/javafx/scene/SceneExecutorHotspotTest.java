package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SceneExecutorHotspotTest {

    private static ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots", List.of(
                new CodeDefinition("first", "First", 10, List.of()),
                new CodeDefinition("second", "Second", 20, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    private static HotspotDefinition hotspot(String id, String targetSceneId) {
        return new HotspotDefinition(id, "loc." + id, 0.1, 0.1, 0.2, 0.2, null, targetSceneId);
    }

    @Test
    void advanceUntilPausePausesOnHotspotMap() {
        HotspotMapDefinition map = new HotspotMapDefinition("world_map", "bg_world.png",
            List.of(hotspot("town", "scene_town"), hotspot("forest", "scene_forest")));
        HotspotMapRegistry mapRegistry = new HotspotMapRegistry();
        mapRegistry.register(map);

        SceneRegistry sceneRegistry = new SceneRegistry();
        sceneRegistry.register(SceneDefinition.of("start", List.of(
            SceneStep.hotspotMap("choose", "world_map"))));
        sceneRegistry.register(SceneDefinition.of("scene_town", List.of(
            SceneStep.narration("town-line", "town.arrival"))));
        sceneRegistry.register(SceneDefinition.of("scene_forest", List.of(
            SceneStep.narration("forest-line", "forest.arrival"))));

        SceneExecutor executor = new SceneExecutor(sceneRegistry, null, mapRegistry);
        ActionContext ctx = actionContext();

        SceneExecutionResult result = executor.advanceUntilPause(ctx, executor.start("start"));

        assertEquals(SceneExecutionStatus.WAITING_FOR_HOTSPOT, result.status());
        assertTrue(result.hotspotMapViewModel().isPresent());
        HotspotMapViewModel vm = result.hotspotMapViewModel().get();
        assertEquals("bg_world.png", vm.backgroundImageRef());
        assertEquals(2, vm.options().size());
        assertEquals("town", vm.options().get(0).id());
        assertTrue(vm.options().get(0).enabled());
    }

    @Test
    void selectHotspotJumpsToTargetScene() {
        HotspotMapDefinition map = new HotspotMapDefinition("world_map", "bg_world.png",
            List.of(hotspot("town", "scene_town")));
        HotspotMapRegistry mapRegistry = new HotspotMapRegistry();
        mapRegistry.register(map);

        SceneRegistry sceneRegistry = new SceneRegistry();
        sceneRegistry.register(SceneDefinition.of("start", List.of(
            SceneStep.hotspotMap("choose", "world_map"))));
        sceneRegistry.register(SceneDefinition.of("scene_town", List.of(
            SceneStep.narration("town-line", "town.arrival"))));

        SceneExecutor executor = new SceneExecutor(sceneRegistry, null, mapRegistry);
        ActionContext ctx = actionContext();

        SceneFlowState hotspotState = executor.advanceUntilPause(ctx, executor.start("start")).state();
        SceneExecutionResult result = executor.selectHotspot(ctx, hotspotState, "town");

        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, result.status());
        assertEquals("town.arrival", result.step().textDefinition());
    }

    @Test
    void selectHotspotThrowsWhenNotWaitingForHotspot() {
        SceneRegistry sceneRegistry = new SceneRegistry();
        sceneRegistry.register(SceneDefinition.of("start", List.of(
            SceneStep.narration("line", "start.line"))));

        SceneExecutor executor = new SceneExecutor(sceneRegistry, null, new HotspotMapRegistry());
        ActionContext ctx = actionContext();

        SceneFlowState state = executor.advanceUntilPause(ctx, executor.start("start")).state();
        assertThrows(IllegalStateException.class, () -> executor.selectHotspot(ctx, state, "town"));
    }
}
