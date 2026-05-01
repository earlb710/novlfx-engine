package com.eb.javafx.gamesupport;

import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GameSupportServiceTest {

    @Test
    void serviceExposesNoContentRegistriesOnlyAfterInitialization() {
        GameSupportService service = new GameSupportService();

        assertFalse(service.isInitialized());
        assertThrows(IllegalStateException.class, service::actionRegistry);

        service.initialize();

        assertTrue(service.isInitialized());
        assertTrue(service.actionRegistry().isEmpty());
        assertEquals(1, service.gameClock().currentTime().day());
        assertEquals(TimeSlot.MORNING, service.gameClock().currentTime().timeSlot());
    }

    @Test
    void clockAdvancesSlotsAndRollsDayAfterNight() {
        GameClock clock = new GameClock();

        assertEquals(new GameDateTime(1, TimeSlot.AFTERNOON).toString(), clock.advanceSlot().toString());
        assertEquals(new GameDateTime(1, TimeSlot.EVENING).toString(), clock.advanceSlot().toString());
        assertEquals(new GameDateTime(1, TimeSlot.NIGHT).toString(), clock.advanceSlot().toString());
        assertEquals(new GameDateTime(2, TimeSlot.MORNING).toString(), clock.advanceSlot().toString());
    }

    @Test
    void actionRegistryRunsGenericActionsWithRequirementsAndEffects() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        GameClock clock = new GameClock();
        ActionContext context = new ActionContext(new GameState("main-menu"), randomService, clock);
        GameAction action = new GameAction(
                "advance-time",
                "Advance Time",
                "time",
                List.of(requirementContext -> RequirementResult.allowed()),
                List.of(effectContext -> {
                    effectContext.gameClock().advanceSlot();
                    return ActionResult.success("Advanced one slot.");
                }));
        ActionRegistry registry = new ActionRegistry();

        registry.register(action);
        ActionResult result = registry.action("advance-time").orElseThrow().execute(context);

        assertTrue(result.success());
        assertTrue(result.stateChanged());
        assertEquals("Advanced one slot.", result.message());
        assertEquals(TimeSlot.AFTERNOON, clock.currentTime().timeSlot());
        assertEquals(1, registry.actionsByCategory().get("time").size());
    }

    @Test
    void blockedRequirementPreventsEffects() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        GameClock clock = new GameClock();
        ActionContext context = new ActionContext(new GameState("main-menu"), randomService, clock);
        GameAction action = new GameAction(
                "blocked",
                "Blocked",
                "test",
                List.of(requirementContext -> RequirementResult.blocked("Not available.")),
                List.of(effectContext -> {
                    effectContext.gameClock().advanceSlot();
                    return ActionResult.success("Should not run.");
                }));

        ActionResult result = action.execute(context);

        assertFalse(result.success());
        assertEquals("Not available.", result.message());
        assertEquals(TimeSlot.MORNING, clock.currentTime().timeSlot());
    }
}
