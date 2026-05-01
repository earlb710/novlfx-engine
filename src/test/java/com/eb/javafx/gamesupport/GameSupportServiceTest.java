package com.eb.javafx.gamesupport;

import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
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
        assertTrue(service.locationRegistry().isEmpty());
        assertEquals(1, service.gameClock().currentTime().day());
        assertEquals("default", service.gameClock().currentTime().timeSlotId());
    }

    @Test
    void clockAdvancesConfiguredSlotsAndRollsDayAfterFinalSlot() {
        GameClock clock = new GameClock(timeSlots(
                new CodeDefinition("early", "Early", 10, List.of()),
                new CodeDefinition("late", "Late", 20, List.of()),
                new CodeDefinition("after-hours", "After Hours", 30, List.of())));

        assertEquals(new GameDateTime(1, "late").toString(), clock.advanceSlot().toString());
        assertEquals(new GameDateTime(1, "after-hours").toString(), clock.advanceSlot().toString());
        assertEquals(new GameDateTime(2, "early").toString(), clock.advanceSlot().toString());
    }

    @Test
    void codeTablesRepresentGenericSectionTwoCategories() {
        CodeTableDefinition roles = codeTable("roles",
                new CodeDefinition("manager", "Manager", 20, List.of("work")),
                new CodeDefinition("founder", "Founder", 10, List.of("work")));

        assertEquals(List.of("founder", "manager"), roles.codes().stream().map(CodeDefinition::id).toList());
        assertTrue(roles.contains("manager"));
        assertThrows(UnsupportedOperationException.class, () ->
                roles.codes().add(new CodeDefinition("other", "Other", 30, List.of())));
    }

    @Test
    void categoryCodeTablesLoadTranslatedDefinitionsFromJson() throws URISyntaxException {
        CategoryCodeTableDefinition categories = CategoryCodeTableDefinition.load(testResource(
                "category-code-tables.en.json"));

        assertEquals("en", categories.language());
        assertTrue(categories.containsTable("roles"));
        assertTrue(categories.containsTable("postures"));
        assertTrue(categories.containsTable("goals"));

        CodeTableDefinition duties = categories.table("duties").orElseThrow();
        assertEquals("Duties", duties.title());
        assertEquals(List.of("closer", "opener"), duties.codes().stream().map(CodeDefinition::id).toList());
        assertEquals(List.of("work", "night"), duties.code("closer").orElseThrow().tags());
    }

    @Test
    void categoryCodeTablesRejectMissingLanguage() {
        assertThrows(IllegalArgumentException.class, () ->
                CategoryCodeTableDefinition.fromJson("{\"tables\":[]}", "missing-language.json"));
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
        assertEquals("default", clock.currentTime().timeSlotId());
        assertEquals(2, clock.currentTime().day());
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
        assertEquals("default", clock.currentTime().timeSlotId());
        assertEquals(1, clock.currentTime().day());
    }

    private static CodeTableDefinition timeSlots(CodeDefinition... codes) {
        return new CodeTableDefinition("time-slots", "Time Slots", List.of(codes));
    }

    private static CodeTableDefinition codeTable(String id, CodeDefinition... codes) {
        return new CodeTableDefinition(id, id, List.of(codes));
    }

    private static Path testResource(String name) throws URISyntaxException {
        URL resource = GameSupportServiceTest.class.getResource("/com/eb/javafx/gamesupport/" + name);
        if (resource == null) {
            throw new IllegalArgumentException("Missing test resource: " + name);
        }
        return Path.of(resource.toURI());
    }
}
