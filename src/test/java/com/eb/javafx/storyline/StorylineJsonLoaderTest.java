package com.eb.javafx.storyline;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StorylineJsonLoaderTest {

    // ----- helpers -----

    private static StorylineJsonLoader loader() {
        return new StorylineJsonLoader();
    }

    // ----- basic event loading -----

    @Test
    void loadsMinimalEvent() {
        String json = """
                { "events": [
                  { "id": "intro.wake", "textKey": "text.intro.wake" }
                ]}""";
        Storyline s = loader().load(json);
        assertEquals(1, s.events().size());
        StorylineEvent e = s.events().get(0);
        assertEquals("intro.wake", e.id());
        assertEquals("text.intro.wake", e.textKey());
        assertFalse(e.repeatable());
        assertSame(EventTrigger.none(), e.trigger());
        assertTrue(e.requirements().isEmpty());
        assertEquals(java.util.Set.of(EventStatus.COMPLETED), e.allowedStatuses());
    }

    @Test
    void loadsDescription() {
        String json = """
                { "events": [
                  { "id": "ev", "textKey": "t", "description": "Marsh arrives at the lab for the first time." }
                ]}""";
        StorylineEvent e = loader().load(json).events().get(0);
        assertTrue(e.description().isPresent());
        assertEquals("Marsh arrives at the lab for the first time.", e.description().get());
    }

    @Test
    void descriptionIsEmptyWhenOmitted() {
        assertFalse(loader().load("""
                { "events": [{ "id": "ev", "textKey": "t" }] }""")
                .events().get(0).description().isPresent());
    }

    @Test
    void loadsRepeatableFlag() {
        String json = """
                { "events": [
                  { "id": "amb.dog", "textKey": "t", "repeatable": true }
                ]}""";
        assertTrue(loader().load(json).events().get(0).repeatable());
    }

    @Test
    void loadsAllowedStatuses() {
        String json = """
                { "events": [
                  { "id": "ev", "textKey": "t", "allowedStatuses": ["accept", "refuse"] }
                ]}""";
        StorylineEvent e = loader().load(json).events().get(0);
        assertTrue(e.allowedStatuses().contains("accept"));
        assertTrue(e.allowedStatuses().contains("refuse"));
        assertEquals(2, e.allowedStatuses().size());
    }

    // ----- trigger types -----

    @Test
    void loadsTriggerNone() {
        String json = """
                { "events": [
                  { "id": "ev", "textKey": "t", "trigger": { "type": "none" } }
                ]}""";
        assertSame(EventTrigger.none(), loader().load(json).events().get(0).trigger());
    }

    @Test
    void loadsTriggerScene() {
        String json = """
                { "events": [
                  { "id": "ev", "textKey": "t",
                    "trigger": { "type": "scene", "sceneId": "scene.lab.enter" } }
                ]}""";
        EventTrigger t = loader().load(json).events().get(0).trigger();
        assertInstanceOf(EventTrigger.Scene.class, t);
        assertEquals("scene.lab.enter", ((EventTrigger.Scene) t).sceneId());
    }

    @Test
    void loadsTriggerDialogChain() {
        String json = """
                { "events": [
                  { "id": "ev", "textKey": "t",
                    "trigger": { "type": "dialogChain", "dialogChainId": "dialog.marsh.intro" } }
                ]}""";
        EventTrigger t = loader().load(json).events().get(0).trigger();
        assertInstanceOf(EventTrigger.DialogChain.class, t);
        assertEquals("dialog.marsh.intro", ((EventTrigger.DialogChain) t).dialogChainId());
    }

    // ----- requirement types -----

    @Test
    void loadsLocationRequirement() {
        String json = req("{ \"type\": \"location\", \"locationId\": \"rejuv-lab\" }");
        EventRequirement r = singleReq(json);
        assertInstanceOf(EventRequirement.Location.class, r);
        assertEquals("rejuv-lab", ((EventRequirement.Location) r).locationId());
    }

    @Test
    void loadsCharacterRequirement() {
        String json = req("{ \"type\": \"character\", \"characterId\": \"marsh\" }");
        EventRequirement r = singleReq(json);
        assertInstanceOf(EventRequirement.Character.class, r);
        assertEquals("marsh", ((EventRequirement.Character) r).characterId());
    }

    @Test
    void loadsTimeOfDayRequirement() {
        String json = req("{ \"type\": \"timeOfDay\", \"timeBucket\": \"morning\" }");
        EventRequirement r = singleReq(json);
        assertInstanceOf(EventRequirement.TimeOfDay.class, r);
        assertEquals("morning", ((EventRequirement.TimeOfDay) r).timeBucket());
    }

    @Test
    void loadsEventCompletedRequirement() {
        String json = req("{ \"type\": \"eventCompleted\", \"eventId\": \"intro.wake\" }");
        EventRequirement r = singleReq(json);
        assertInstanceOf(EventRequirement.EventCompleted.class, r);
        assertEquals("intro.wake", ((EventRequirement.EventCompleted) r).eventId());
    }

    @Test
    void loadsEventStatusRequirement() {
        String json = req("{ \"type\": \"eventStatus\", \"eventId\": \"root.ev\", \"status\": \"accept\" }");
        EventRequirement r = singleReq(json);
        assertInstanceOf(EventRequirement.EventStatus.class, r);
        EventRequirement.EventStatus es = (EventRequirement.EventStatus) r;
        assertEquals("root.ev", es.eventId());
        assertEquals("accept", es.requiredStatus());
    }

    @Test
    void loadsFlagRequirement() {
        String json = req("{ \"type\": \"flag\", \"flag\": \"phone-unlocked\" }");
        EventRequirement r = singleReq(json);
        assertInstanceOf(EventRequirement.Flag.class, r);
        assertEquals("phone-unlocked", ((EventRequirement.Flag) r).flag());
    }

    @Test
    void loadsAllOfRequirement() {
        String json = req("""
                { "type": "allOf", "requirements": [
                  { "type": "location", "locationId": "lab" },
                  { "type": "timeOfDay", "timeBucket": "morning" }
                ]}""");
        EventRequirement r = singleReq(json);
        assertInstanceOf(EventRequirement.AllOf.class, r);
        List<EventRequirement> children = ((EventRequirement.AllOf) r).children();
        assertEquals(2, children.size());
        assertInstanceOf(EventRequirement.Location.class, children.get(0));
        assertInstanceOf(EventRequirement.TimeOfDay.class, children.get(1));
    }

    @Test
    void loadsAnyOfRequirement() {
        String json = req("""
                { "type": "anyOf", "requirements": [
                  { "type": "flag", "flag": "a" },
                  { "type": "flag", "flag": "b" }
                ]}""");
        EventRequirement r = singleReq(json);
        assertInstanceOf(EventRequirement.AnyOf.class, r);
        assertEquals(2, ((EventRequirement.AnyOf) r).children().size());
    }

    @Test
    void loadsNotRequirement() {
        String json = req("""
                { "type": "not", "requirement":
                  { "type": "location", "locationId": "lab" }
                }""");
        EventRequirement r = singleReq(json);
        assertInstanceOf(EventRequirement.Not.class, r);
        assertInstanceOf(EventRequirement.Location.class, ((EventRequirement.Not) r).inner());
    }

    @Test
    void loadsCustomRequirementViaRegistry() {
        EventRequirement stub = EventRequirement.flagSet("stub-flag");
        CustomRequirementRegistry registry = name -> {
            if ("my.custom".equals(name)) return stub;
            throw new IllegalArgumentException("Unknown: " + name);
        };
        String json = req("{ \"type\": \"custom\", \"name\": \"my.custom\" }");
        EventRequirement r = new StorylineJsonLoader(registry).load(json).events().get(0).requirements().get(0);
        assertSame(stub, r);
    }

    // ----- round-trip eligibility -----

    @Test
    void loadedStorylineIsEligibleUnderMatchingRuntime() {
        String json = """
                { "events": [
                  { "id": "root", "textKey": "t" },
                  { "id": "child", "textKey": "t",
                    "requirements": [
                      { "type": "eventStatus", "eventId": "root", "status": "COMPLETED" }
                    ]}
                ]}""";
        Storyline s = loader().load(json);
        StorylineRuntime runtime = new StorylineRuntime();
        StorylineDirector director = new StorylineDirector(s, runtime);

        List<StorylineEvent> eligible = director.eligibleEvents(StorylineContext.empty(), null);
        assertEquals(1, eligible.size());
        assertEquals("root", eligible.get(0).id());

        director.recordCompleted("root");
        eligible = director.eligibleEvents(StorylineContext.empty(), null);
        assertEquals(1, eligible.size());
        assertEquals("child", eligible.get(0).id());
    }

    // ----- error cases -----

    @Test
    void rejectsUnknownTriggerType() {
        String json = """
                { "events": [
                  { "id": "ev", "textKey": "t", "trigger": { "type": "explode" } }
                ]}""";
        assertThrows(IllegalArgumentException.class, () -> loader().load(json));
    }

    @Test
    void rejectsUnknownRequirementType() {
        String json = req("{ \"type\": \"teleport\" }");
        assertThrows(IllegalArgumentException.class, () -> loader().load(json));
    }

    @Test
    void rejectsCustomRequirementWithoutRegistry() {
        String json = req("{ \"type\": \"custom\", \"name\": \"any.name\" }");
        assertThrows(IllegalArgumentException.class, () -> loader().load(json));
    }

    @Test
    void rejectsMissingId() {
        String json = "{ \"events\": [{ \"textKey\": \"t\" }] }";
        assertThrows(IllegalArgumentException.class, () -> loader().load(json));
    }

    @Test
    void rejectsMissingTextKey() {
        String json = "{ \"events\": [{ \"id\": \"ev\" }] }";
        assertThrows(IllegalArgumentException.class, () -> loader().load(json));
    }

    @Test
    void rejectsDuplicateEventIds() {
        String json = """
                { "events": [
                  { "id": "same", "textKey": "t" },
                  { "id": "same", "textKey": "t" }
                ]}""";
        assertThrows(IllegalArgumentException.class, () -> loader().load(json));
    }

    // ----- helpers -----

    private static String req(String requirementJson) {
        return "{ \"events\": [{ \"id\": \"ev\", \"textKey\": \"t\", \"requirements\": [" + requirementJson + "] }] }";
    }

    private static EventRequirement singleReq(String json) {
        List<EventRequirement> reqs = loader().load(json).events().get(0).requirements();
        assertEquals(1, reqs.size(), "Expected exactly one requirement.");
        return reqs.get(0);
    }
}
