package com.eb.javafx.storyline;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for the typed-requirement storyline system:
 * <ul>
 *   <li>Each {@link EventRequirement} variant gates eligibility correctly.</li>
 *   <li>An event with both {@link EventRequirement.Location} and
 *       {@link EventRequirement.TimeOfDay} requirements appears in BOTH category trees.</li>
 *   <li>Parent-child via {@link EventRequirement.EventStatus} requirement — a child sits dormant
 *       until the parent records the right status, then surfaces from
 *       {@link StorylineDirector#recordOutcome}.</li>
 *   <li>Once-off vs repeatable lifecycle.</li>
 *   <li>{@link StorylineRuntime} snapshot round-trip.</li>
 *   <li>{@link EventRequirement.Custom} escape hatch with declared category indexes.</li>
 * </ul>
 */
final class StorylineTest {

    @Test
    void multiCategoryEventAppearsInBothTrees() {
        StorylineEvent labMorningEvent = StorylineEvent.builder("c1.mirror", "text.c1.mirror")
                .requirement(EventRequirement.atLocation("rejuv-lab.recovery-room"))
                .requirement(EventRequirement.atTimeOfDay("morning"))
                .build();
        Storyline storyline = Storyline.builder().event(labMorningEvent).build();

        EventTree locationTree = storyline.treeForCategory(EventCategory.LOCATION);
        EventTree timeTree = storyline.treeForCategory(EventCategory.TIME);
        assertEquals(List.of(labMorningEvent),
                locationTree.events("rejuv-lab.recovery-room"),
                "Event should show up under its location bucket.");
        assertEquals(List.of(labMorningEvent),
                timeTree.events("morning"),
                "Same event should also show up under its time-of-day bucket.");
        // GENERIC tree stays empty because the event has category-yielding requirements.
        assertTrue(storyline.treeForCategory(EventCategory.GENERIC).allEvents().isEmpty());
    }

    @Test
    void eventWithoutCategoryRequirementsLandsInGenericTree() {
        StorylineEvent generic = StorylineEvent.builder("intro.wake", "text.intro.wake").build();
        Storyline storyline = Storyline.builder().event(generic).build();
        EventTree generics = storyline.treeForCategory(EventCategory.GENERIC);
        assertEquals(List.of(generic), generics.events(EventTree.Bucket.GENERIC_KEY));
    }

    @Test
    void locationRequirementGatesEligibilityOnCurrentLocation() {
        StorylineEvent atLab = StorylineEvent.builder("at-lab", "text.at-lab")
                .requirement(EventRequirement.atLocation("rejuv-lab"))
                .build();
        Storyline storyline = Storyline.builder().event(atLab).build();
        StorylineRuntime runtime = new StorylineRuntime();
        StorylineDirector director = new StorylineDirector(storyline, runtime);

        StorylineContext outside = StorylineContext.builder().locationId("city.street").build();
        StorylineContext inside = StorylineContext.builder().locationId("rejuv-lab").build();
        assertTrue(director.eligibleEvents(outside, null).isEmpty(), "Wrong location → not eligible.");
        assertEquals(List.of(atLab), director.eligibleEvents(inside, null));
    }

    @Test
    void parentChildLinkViaEventStatusRequirement() {
        StorylineEvent intro = StorylineEvent.builder("marsh.intro", "text.marsh.intro")
                .trigger(EventTrigger.dialogChain("dialog.marsh.intro"))
                .allowedStatuses("accept", "refuse")
                .build();
        StorylineEvent acceptFollowup = StorylineEvent.builder("marsh.accept.followup", "text.accept.followup")
                .requirement(EventRequirement.eventStatus("marsh.intro", "accept"))
                .build();
        StorylineEvent refuseFollowup = StorylineEvent.builder("marsh.refuse.followup", "text.refuse.followup")
                .requirement(EventRequirement.eventStatus("marsh.intro", "refuse"))
                .build();
        Storyline storyline = Storyline.builder()
                .events(intro, acceptFollowup, refuseFollowup)
                .build();
        StorylineRuntime runtime = new StorylineRuntime();
        StorylineDirector director = new StorylineDirector(storyline, runtime);

        // Both children gated on a status that hasn't been recorded yet.
        assertEquals(List.of(intro), director.eligibleEvents(StorylineContext.empty(), null));

        List<StorylineEvent> unlocked = director.recordOutcome("marsh.intro", "accept");
        assertEquals(List.of(acceptFollowup), unlocked,
                "Only the accept-branch child should surface, not the refuse-branch one.");
        // The refuse branch is still dormant.
        assertFalse(director.eligibleEvents(StorylineContext.empty(), null).contains(refuseFollowup));
        assertTrue(director.eligibleEvents(StorylineContext.empty(), null).contains(acceptFollowup));
    }

    @Test
    void recordOutcomeRejectsStatusOutsideVocabulary() {
        StorylineEvent main = StorylineEvent.builder("event", "text.event")
                .allowedStatuses("accept", "refuse")
                .build();
        Storyline storyline = Storyline.builder().event(main).build();
        StorylineDirector director = new StorylineDirector(storyline, new StorylineRuntime());
        assertThrows(IllegalArgumentException.class, () -> director.recordOutcome("event", "explode"));
    }

    @Test
    void onceOffEventDropsOutOfEligibilityAfterFiring() {
        StorylineEvent once = StorylineEvent.builder("intro.wake", "text.intro.wake").build();
        Storyline storyline = Storyline.builder().event(once).build();
        StorylineRuntime runtime = new StorylineRuntime();
        StorylineDirector director = new StorylineDirector(storyline, runtime);

        assertEquals(1, director.eligibleEvents(StorylineContext.empty(), null).size());
        StorylineDirector.FireResult result = director.fire("intro.wake", StorylineContext.empty(), null);
        assertSame(EventTrigger.none(), result.trigger());
        director.recordCompleted("intro.wake");
        assertTrue(director.eligibleEvents(StorylineContext.empty(), null).isEmpty());
    }

    @Test
    void repeatableEventAccumulatesCount() {
        StorylineEvent repeatable = StorylineEvent.builder("ambient.dog", "text.dog").repeatable().build();
        Storyline storyline = Storyline.builder().event(repeatable).build();
        StorylineRuntime runtime = new StorylineRuntime();
        StorylineDirector director = new StorylineDirector(storyline, runtime);

        director.recordCompleted("ambient.dog");
        director.recordCompleted("ambient.dog");
        director.recordCompleted("ambient.dog");

        assertEquals(3, runtime.completionCount("ambient.dog"));
        assertEquals(List.of(repeatable), director.eligibleEvents(StorylineContext.empty(), null),
                "Repeatable events stay eligible after completion.");
    }

    @Test
    void snapshotRoundTrip() {
        StorylineRuntime runtime = new StorylineRuntime();
        runtime.recordCompletion("a", "accept");
        runtime.recordCompletion("b", EventStatus.COMPLETED);
        runtime.recordCompletion("b", EventStatus.COMPLETED);
        runtime.setFlag("phone-unlocked");

        StorylineRuntime.Snapshot snapshot = runtime.snapshot();
        StorylineRuntime restored = new StorylineRuntime();
        restored.restore(snapshot);
        assertEquals("accept", restored.statusOf("a").orElseThrow());
        assertEquals(2, restored.completionCount("b"));
        assertTrue(restored.hasFlag("phone-unlocked"));
    }

    @Test
    void customRequirementWithDeclaredIndex() {
        // App-specific gate (e.g. weather), explicitly placed into the TIME tree under "rainy".
        EventRequirement.Custom rainy = (EventRequirement.Custom) EventRequirement.custom(
                "is-rainy",
                (runtime, context) -> "rainy".equals(context.attribute("weather").orElse(null)));
        rainy = rainy.withIndex(EventCategory.TIME, "rainy");
        StorylineEvent puddleStomp = StorylineEvent.builder("amb.puddle", "text.puddle")
                .requirement(rainy)
                .build();
        Storyline storyline = Storyline.builder().event(puddleStomp).build();

        // Indexed into TIME/rainy thanks to the explicit declaredIndexes.
        assertEquals(List.of(puddleStomp), storyline.treeForCategory(EventCategory.TIME).events("rainy"));
        // Eligibility honours the predicate.
        StorylineRuntime runtime = new StorylineRuntime();
        StorylineDirector director = new StorylineDirector(storyline, runtime);
        StorylineContext sunny = StorylineContext.builder().attribute("weather", "sunny").build();
        StorylineContext rainyCtx = StorylineContext.builder().attribute("weather", "rainy").build();
        assertTrue(director.eligibleEvents(sunny, null).isEmpty());
        assertEquals(List.of(puddleStomp), director.eligibleEvents(rainyCtx, null));
    }

    @Test
    void compositeAllOfAccumulatesCategoryIndexes() {
        StorylineEvent event = StorylineEvent.builder("two-things", "text")
                .requirement(EventRequirement.allOf(
                        EventRequirement.atLocation("lab"),
                        EventRequirement.atTimeOfDay("morning")))
                .build();
        Storyline storyline = Storyline.builder().event(event).build();
        // AllOf union → event indexed under both LOCATION.lab and TIME.morning.
        assertEquals(List.of(event), storyline.treeForCategory(EventCategory.LOCATION).events("lab"));
        assertEquals(List.of(event), storyline.treeForCategory(EventCategory.TIME).events("morning"));
    }

    @Test
    void notInvertsAndDoesNotIndex() {
        // Event that fires when NOT at the lab. Should not appear under LOCATION/lab.
        StorylineEvent event = StorylineEvent.builder("notlab", "text")
                .requirement(EventRequirement.not(EventRequirement.atLocation("lab")))
                .build();
        Storyline storyline = Storyline.builder().event(event).build();
        assertTrue(storyline.treeForCategory(EventCategory.LOCATION).events("lab").isEmpty());
        // It lands in GENERIC because Not contributes no indexes and no other index-yielding gates exist.
        assertEquals(List.of(event), storyline.treeForCategory(EventCategory.GENERIC).events(EventTree.Bucket.GENERIC_KEY));

        // Eligibility flips correctly.
        StorylineDirector director = new StorylineDirector(storyline, new StorylineRuntime());
        StorylineContext atLab = StorylineContext.builder().locationId("lab").build();
        StorylineContext atHome = StorylineContext.builder().locationId("home").build();
        assertTrue(director.eligibleEvents(atLab, null).isEmpty());
        assertEquals(List.of(event), director.eligibleEvents(atHome, null));
    }

    @Test
    void categoryFilteredEligibleEventsRespectsRequirements() {
        StorylineEvent labOnly = StorylineEvent.builder("lab-event", "t")
                .requirement(EventRequirement.atLocation("lab"))
                .build();
        StorylineEvent characterOnly = StorylineEvent.builder("marsh-event", "t")
                .requirement(EventRequirement.withCharacter("marsh"))
                .build();
        Storyline storyline = Storyline.builder().events(labOnly, characterOnly).build();
        StorylineDirector director = new StorylineDirector(storyline, new StorylineRuntime());
        StorylineContext labAndMarsh = StorylineContext.builder()
                .locationId("lab")
                .characterId("marsh")
                .build();

        List<StorylineEvent> locationEligible = director.eligibleEvents(EventCategory.LOCATION, labAndMarsh, null);
        List<StorylineEvent> characterEligible = director.eligibleEvents(EventCategory.CHARACTER, labAndMarsh, null);
        assertEquals(List.of(labOnly), locationEligible);
        assertEquals(List.of(characterOnly), characterEligible);
    }

    @Test
    void duplicateEventIdsAreRejected() {
        StorylineEvent a = StorylineEvent.builder("same", "text").build();
        StorylineEvent b = StorylineEvent.builder("same", "text").build();
        assertThrows(IllegalArgumentException.class, () -> Storyline.builder().event(a).event(b).build());
    }
}
