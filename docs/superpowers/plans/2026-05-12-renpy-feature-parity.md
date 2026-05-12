# Ren'Py Feature Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add six missing Ren'Py-parity features to the novlfx engine: visual scene transitions, image tag resolution, skip/auto modes, NVL mode, scene-level conditionals, and a gallery framework.

**Architecture:** Each feature is an independent addition to an existing package (`scene`, `display`, `progress`) or a new package (`transitions`, `gallery`). All features are data-model and service-layer only — no JavaFX rendering. Adapters and UI code that consume these additions live in the game application, not the engine.

**Tech Stack:** Java 17, JUnit 5 (jupiter), Gradle `./gradlew --no-daemon`

---

> **NOTE: Each task group below is fully independent.** They can be implemented in any order or in parallel. A team member can pick up any group without reading the others.

---

## Task Group 1: Visual Scene Transitions

### Task 1.1: `SceneTransitionEffect` enum

**Files:**
- Create: `src/main/java/com/eb/javafx/transitions/SceneTransitionEffect.java`
- Create: `src/test/java/com/eb/javafx/transitions/SceneTransitionEffectTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SceneTransitionEffectTest {

    @Test
    void noneEffectIsInstant() {
        assertTrue(SceneTransitionEffect.NONE.isInstant());
    }

    @Test
    void nonInstantEffectHasDuration() {
        assertFalse(SceneTransitionEffect.DISSOLVE.isInstant());
        assertFalse(SceneTransitionEffect.FADE_BLACK.isInstant());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```
Expected: compile error — `SceneTransitionEffect` does not exist.

- [ ] **Step 3: Create the enum**

```java
package com.eb.javafx.transitions;

/** Named visual-effect types played by the display adapter between scene changes. */
public enum SceneTransitionEffect {
    NONE,
    DISSOLVE,
    FADE_BLACK,
    WIPE_LEFT,
    WIPE_RIGHT,
    MOVE_IN_LEFT,
    MOVE_IN_RIGHT;

    public boolean isInstant() {
        return this == NONE;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```
./gradlew --no-daemon test --tests "com.eb.javafx.transitions.SceneTransitionEffectTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```
git add src/main/java/com/eb/javafx/transitions/SceneTransitionEffect.java src/test/java/com/eb/javafx/transitions/SceneTransitionEffectTest.java
git commit -m "feat(transitions): add SceneTransitionEffect enum"
```

---

### Task 1.2: `VisualTransitionDefinition` value object

**Files:**
- Create: `src/main/java/com/eb/javafx/transitions/VisualTransitionDefinition.java`
- Create: `src/test/java/com/eb/javafx/transitions/VisualTransitionDefinitionTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class VisualTransitionDefinitionTest {

    @Test
    void constructsWithValidFields() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("dissolve-fast", SceneTransitionEffect.DISSOLVE, 300);
        assertEquals("dissolve-fast", def.id());
        assertEquals(SceneTransitionEffect.DISSOLVE, def.effect());
        assertEquals(300, def.durationMs());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> new VisualTransitionDefinition("", SceneTransitionEffect.DISSOLVE, 300));
    }

    @Test
    void rejectsNegativeDuration() {
        assertThrows(IllegalArgumentException.class,
            () -> new VisualTransitionDefinition("t", SceneTransitionEffect.DISSOLVE, -1));
    }

    @Test
    void noneEffectAllowsZeroDuration() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("instant", SceneTransitionEffect.NONE, 0);
        assertEquals(0, def.durationMs());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement**

```java
package com.eb.javafx.transitions;

import com.eb.javafx.util.Validation;

/** Immutable named visual transition effect definition. */
public final class VisualTransitionDefinition {
    private final String id;
    private final SceneTransitionEffect effect;
    private final int durationMs;

    public VisualTransitionDefinition(String id, SceneTransitionEffect effect, int durationMs) {
        this.id = Validation.requireNonBlank(id, "Visual transition id is required.");
        this.effect = Validation.requireNonNull(effect, "Visual transition effect is required.");
        if (durationMs < 0) {
            throw new IllegalArgumentException("Visual transition durationMs must not be negative.");
        }
        this.durationMs = durationMs;
    }

    public String id() { return id; }
    public SceneTransitionEffect effect() { return effect; }
    public int durationMs() { return durationMs; }
}
```

- [ ] **Step 4: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.transitions.VisualTransitionDefinitionTest"
```

- [ ] **Step 5: Commit**

```
git add src/main/java/com/eb/javafx/transitions/VisualTransitionDefinition.java src/test/java/com/eb/javafx/transitions/VisualTransitionDefinitionTest.java
git commit -m "feat(transitions): add VisualTransitionDefinition"
```

---

### Task 1.3: `VisualTransitionRegistry`

**Files:**
- Create: `src/main/java/com/eb/javafx/transitions/VisualTransitionRegistry.java`
- Create: `src/test/java/com/eb/javafx/transitions/VisualTransitionRegistryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class VisualTransitionRegistryTest {

    @Test
    void registersAndFindsDefinition() {
        VisualTransitionRegistry registry = new VisualTransitionRegistry();
        registry.register(new VisualTransitionDefinition("fade", SceneTransitionEffect.FADE_BLACK, 500));
        Optional<VisualTransitionDefinition> found = registry.find("fade");
        assertTrue(found.isPresent());
        assertEquals(SceneTransitionEffect.FADE_BLACK, found.get().effect());
    }

    @Test
    void returnsEmptyForUnknownId() {
        VisualTransitionRegistry registry = new VisualTransitionRegistry();
        assertTrue(registry.find("unknown").isEmpty());
    }

    @Test
    void rejectsDuplicateId() {
        VisualTransitionRegistry registry = new VisualTransitionRegistry();
        registry.register(new VisualTransitionDefinition("t", SceneTransitionEffect.DISSOLVE, 300));
        assertThrows(IllegalArgumentException.class,
            () -> registry.register(new VisualTransitionDefinition("t", SceneTransitionEffect.FADE_BLACK, 200)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement**

```java
package com.eb.javafx.transitions;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Startup registry of named visual transition definitions. */
public final class VisualTransitionRegistry {
    private final Map<String, VisualTransitionDefinition> definitions = new LinkedHashMap<>();

    public void register(VisualTransitionDefinition definition) {
        Validation.requireNonNull(definition, "definition");
        if (definitions.containsKey(definition.id())) {
            throw new IllegalArgumentException("Duplicate visual transition id: " + definition.id());
        }
        definitions.put(definition.id(), definition);
    }

    public Optional<VisualTransitionDefinition> find(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public VisualTransitionDefinition require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown visual transition: " + id));
    }
}
```

- [ ] **Step 4: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.transitions.VisualTransitionRegistryTest"
```

- [ ] **Step 5: Commit**

```
git add src/main/java/com/eb/javafx/transitions/VisualTransitionRegistry.java src/test/java/com/eb/javafx/transitions/VisualTransitionRegistryTest.java
git commit -m "feat(transitions): add VisualTransitionRegistry"
```

---

### Task 1.4: `VisualTransitionRequest` and export

**Files:**
- Create: `src/main/java/com/eb/javafx/transitions/VisualTransitionRequest.java`
- Modify: `src/main/java/module-info.java`
- Create: `src/test/java/com/eb/javafx/transitions/VisualTransitionRequestTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class VisualTransitionRequestTest {

    @Test
    void carriesDefinitionReference() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("dissolve", SceneTransitionEffect.DISSOLVE, 400);
        VisualTransitionRequest req = new VisualTransitionRequest(def);
        assertEquals("dissolve", req.definition().id());
        assertEquals(400, req.definition().durationMs());
    }

    @Test
    void rejectsNullDefinition() {
        assertThrows(NullPointerException.class, () -> new VisualTransitionRequest(null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement**

```java
package com.eb.javafx.transitions;

import java.util.Objects;

/** Runtime request for the display adapter to play a visual transition effect. */
public final class VisualTransitionRequest {
    private final VisualTransitionDefinition definition;

    public VisualTransitionRequest(VisualTransitionDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public VisualTransitionDefinition definition() { return definition; }
}
```

- [ ] **Step 4: Export the package in module-info.java**

In `src/main/java/module-info.java`, add after the last `exports` line before the closing `}`:

```java
    exports com.eb.javafx.transitions;
```

- [ ] **Step 5: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.transitions.VisualTransitionRequestTest"
```

- [ ] **Step 6: Commit**

```
git add src/main/java/com/eb/javafx/transitions/VisualTransitionRequest.java src/main/java/module-info.java src/test/java/com/eb/javafx/transitions/VisualTransitionRequestTest.java
git commit -m "feat(transitions): add VisualTransitionRequest, export transitions package"
```

---

## Task Group 2: Image Tag Resolution

### Task 2.1: `DisplayTagDefinition`

**Files:**
- Create: `src/main/java/com/eb/javafx/display/DisplayTagDefinition.java`
- Create: `src/test/java/com/eb/javafx/display/DisplayTagDefinitionTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DisplayTagDefinitionTest {

    @Test
    void constructsWithRequiredFields() {
        DisplayTagDefinition tag = new DisplayTagDefinition("hero_happy", "char_hero_happy", DisplayLayer.CHARACTER, null);
        assertEquals("hero_happy", tag.tag());
        assertEquals("char_hero_happy", tag.displayId());
        assertEquals(DisplayLayer.CHARACTER, tag.layer());
        assertTrue(tag.transformPresetName().isEmpty());
    }

    @Test
    void constructsWithOptionalTransformPreset() {
        DisplayTagDefinition tag = new DisplayTagDefinition("hero_left", "char_hero", DisplayLayer.CHARACTER, "pos_left");
        assertEquals("pos_left", tag.transformPresetName().get());
    }

    @Test
    void rejectsBlankTag() {
        assertThrows(IllegalArgumentException.class,
            () -> new DisplayTagDefinition("", "char_hero", DisplayLayer.CHARACTER, null));
    }

    @Test
    void rejectsBlankDisplayId() {
        assertThrows(IllegalArgumentException.class,
            () -> new DisplayTagDefinition("hero", "", DisplayLayer.CHARACTER, null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement**

```java
package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

import java.util.Optional;

/** Maps a semantic author tag to a concrete display ID, layer, and optional transform preset. */
public final class DisplayTagDefinition {
    private final String tag;
    private final String displayId;
    private final DisplayLayer layer;
    private final String transformPresetName;

    public DisplayTagDefinition(String tag, String displayId, DisplayLayer layer, String transformPresetName) {
        this.tag = Validation.requireNonBlank(tag, "Display tag is required.");
        this.displayId = Validation.requireNonBlank(displayId, "Display tag displayId is required.");
        this.layer = Validation.requireNonNull(layer, "Display tag layer is required.");
        this.transformPresetName = transformPresetName;
    }

    public String tag() { return tag; }
    public String displayId() { return displayId; }
    public DisplayLayer layer() { return layer; }
    public Optional<String> transformPresetName() { return Optional.ofNullable(transformPresetName); }
}
```

- [ ] **Step 4: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.display.DisplayTagDefinitionTest"
```

- [ ] **Step 5: Commit**

```
git add src/main/java/com/eb/javafx/display/DisplayTagDefinition.java src/test/java/com/eb/javafx/display/DisplayTagDefinitionTest.java
git commit -m "feat(display): add DisplayTagDefinition"
```

---

### Task 2.2: `DisplayTagRegistry`

**Files:**
- Create: `src/main/java/com/eb/javafx/display/DisplayTagRegistry.java`
- Create: `src/test/java/com/eb/javafx/display/DisplayTagRegistryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class DisplayTagRegistryTest {

    @Test
    void registersAndFindsTag() {
        DisplayTagRegistry registry = new DisplayTagRegistry();
        registry.register(new DisplayTagDefinition("hero_happy", "char_hero_happy", DisplayLayer.CHARACTER, null));
        Optional<DisplayTagDefinition> found = registry.find("hero_happy");
        assertTrue(found.isPresent());
        assertEquals("char_hero_happy", found.get().displayId());
    }

    @Test
    void returnsEmptyForUnknownTag() {
        DisplayTagRegistry registry = new DisplayTagRegistry();
        assertTrue(registry.find("unknown").isEmpty());
    }

    @Test
    void rejectsDuplicateTag() {
        DisplayTagRegistry registry = new DisplayTagRegistry();
        registry.register(new DisplayTagDefinition("hero", "char_hero_a", DisplayLayer.CHARACTER, null));
        assertThrows(IllegalArgumentException.class,
            () -> registry.register(new DisplayTagDefinition("hero", "char_hero_b", DisplayLayer.CHARACTER, null)));
    }

    @Test
    void requireThrowsForUnknownTag() {
        DisplayTagRegistry registry = new DisplayTagRegistry();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> registry.require("ghost"));
        assertTrue(ex.getMessage().contains("ghost"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement**

```java
package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Startup registry of semantic display tag definitions. */
public final class DisplayTagRegistry {
    private final Map<String, DisplayTagDefinition> tags = new LinkedHashMap<>();

    public void register(DisplayTagDefinition definition) {
        Validation.requireNonNull(definition, "definition");
        if (tags.containsKey(definition.tag())) {
            throw new IllegalArgumentException("Duplicate display tag: " + definition.tag());
        }
        tags.put(definition.tag(), definition);
    }

    public Optional<DisplayTagDefinition> find(String tag) {
        return Optional.ofNullable(tags.get(tag));
    }

    public DisplayTagDefinition require(String tag) {
        return find(tag).orElseThrow(() -> new IllegalArgumentException("Unknown display tag: " + tag));
    }
}
```

- [ ] **Step 4: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.display.DisplayTagRegistryTest"
```

- [ ] **Step 5: Commit**

```
git add src/main/java/com/eb/javafx/display/DisplayTagRegistry.java src/test/java/com/eb/javafx/display/DisplayTagRegistryTest.java
git commit -m "feat(display): add DisplayTagRegistry"
```

---

### Task 2.3: `DisplayTagResolver`

**Files:**
- Create: `src/main/java/com/eb/javafx/display/DisplayTagResolver.java`
- Create: `src/test/java/com/eb/javafx/display/DisplayTagResolverTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.display;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DisplayTagResolverTest {
    private DisplayTagRegistry tagRegistry;
    private DisplayTagResolver resolver;

    @BeforeEach
    void setUp() {
        tagRegistry = new DisplayTagRegistry();
        tagRegistry.register(new DisplayTagDefinition("hero_happy", "char_hero_happy", DisplayLayer.CHARACTER, null));
        tagRegistry.register(new DisplayTagDefinition("hero_left", "char_hero_neutral", DisplayLayer.CHARACTER, "pos_left"));
        resolver = new DisplayTagResolver(tagRegistry);
    }

    @Test
    void resolvesTagToDisplayIdAndLayer() {
        DisplayTagResolution result = resolver.resolve("hero_happy");
        assertEquals("char_hero_happy", result.displayId());
        assertEquals(DisplayLayer.CHARACTER, result.layer());
        assertTrue(result.transformPresetName().isEmpty());
    }

    @Test
    void resolvesTagWithTransformPreset() {
        DisplayTagResolution result = resolver.resolve("hero_left");
        assertEquals("pos_left", result.transformPresetName().get());
    }

    @Test
    void throwsForUnknownTag() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve("ghost_npc"));
        assertTrue(ex.getMessage().contains("ghost_npc"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement `DisplayTagResolution` (small result type)**

```java
package com.eb.javafx.display;

import java.util.Optional;

/** Result of resolving a semantic display tag to concrete display coordinates. */
public final class DisplayTagResolution {
    private final String displayId;
    private final DisplayLayer layer;
    private final String transformPresetName;

    DisplayTagResolution(String displayId, DisplayLayer layer, String transformPresetName) {
        this.displayId = displayId;
        this.layer = layer;
        this.transformPresetName = transformPresetName;
    }

    public String displayId() { return displayId; }
    public DisplayLayer layer() { return layer; }
    public Optional<String> transformPresetName() { return Optional.ofNullable(transformPresetName); }
}
```

Save to: `src/main/java/com/eb/javafx/display/DisplayTagResolution.java`

- [ ] **Step 4: Implement `DisplayTagResolver`**

```java
package com.eb.javafx.display;

import java.util.Objects;

/** Resolves semantic display tags to concrete display IDs, layers, and optional transform presets. */
public final class DisplayTagResolver {
    private final DisplayTagRegistry tagRegistry;

    public DisplayTagResolver(DisplayTagRegistry tagRegistry) {
        this.tagRegistry = Objects.requireNonNull(tagRegistry, "tagRegistry");
    }

    public DisplayTagResolution resolve(String tag) {
        DisplayTagDefinition def = tagRegistry.require(tag);
        return new DisplayTagResolution(def.displayId(), def.layer(), def.transformPresetName().orElse(null));
    }
}
```

- [ ] **Step 5: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.display.DisplayTagResolverTest"
```

- [ ] **Step 6: Commit**

```
git add src/main/java/com/eb/javafx/display/DisplayTagDefinition.java src/main/java/com/eb/javafx/display/DisplayTagRegistry.java src/main/java/com/eb/javafx/display/DisplayTagResolution.java src/main/java/com/eb/javafx/display/DisplayTagResolver.java src/test/java/com/eb/javafx/display/DisplayTagResolverTest.java
git commit -m "feat(display): add DisplayTagResolver and DisplayTagResolution"
```

---

## Task Group 3: Skip / Auto Modes

### Task 3.1: `SeenStepTracker`

**Files:**
- Create: `src/main/java/com/eb/javafx/scene/SeenStepTracker.java`
- Create: `src/main/java/com/eb/javafx/scene/SeenStepSnapshot.java`
- Create: `src/test/java/com/eb/javafx/scene/SeenStepTrackerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SeenStepTrackerTest {

    @Test
    void markAndCheckSeen() {
        SeenStepTracker tracker = new SeenStepTracker();
        assertFalse(tracker.hasSeen("scene1", "step1"));
        tracker.markSeen("scene1", "step1");
        assertTrue(tracker.hasSeen("scene1", "step1"));
    }

    @Test
    void differentStepsAreIndependent() {
        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene1", "step1");
        assertFalse(tracker.hasSeen("scene1", "step2"));
        assertFalse(tracker.hasSeen("scene2", "step1"));
    }

    @Test
    void snapshotRoundTrip() {
        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene1", "step1");
        tracker.markSeen("scene2", "step3");

        SeenStepSnapshot snapshot = tracker.snapshot();
        SeenStepTracker restored = new SeenStepTracker();
        restored.restore(snapshot);

        assertTrue(restored.hasSeen("scene1", "step1"));
        assertTrue(restored.hasSeen("scene2", "step3"));
        assertFalse(restored.hasSeen("scene1", "step3"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement `SeenStepSnapshot`**

```java
package com.eb.javafx.scene;

import java.util.Set;

/** Immutable snapshot of which scene steps have been seen by the player. */
public final class SeenStepSnapshot {
    private final Set<String> seenKeys;

    SeenStepSnapshot(Set<String> seenKeys) {
        this.seenKeys = Set.copyOf(seenKeys);
    }

    Set<String> seenKeys() { return seenKeys; }
}
```

- [ ] **Step 4: Implement `SeenStepTracker`**

```java
package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashSet;
import java.util.Set;

/** Mutable tracker of which scene steps have been seen; used by skip mode to fast-forward seen text. */
public final class SeenStepTracker {
    private final Set<String> seenKeys = new LinkedHashSet<>();

    public void markSeen(String sceneId, String stepId) {
        Validation.requireNonBlank(sceneId, "sceneId");
        Validation.requireNonBlank(stepId, "stepId");
        seenKeys.add(sceneId + ":" + stepId);
    }

    public boolean hasSeen(String sceneId, String stepId) {
        return seenKeys.contains(sceneId + ":" + stepId);
    }

    public SeenStepSnapshot snapshot() {
        return new SeenStepSnapshot(seenKeys);
    }

    public void restore(SeenStepSnapshot snapshot) {
        Validation.requireNonNull(snapshot, "snapshot");
        seenKeys.clear();
        seenKeys.addAll(snapshot.seenKeys());
    }
}
```

- [ ] **Step 5: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.SeenStepTrackerTest"
```

- [ ] **Step 6: Commit**

```
git add src/main/java/com/eb/javafx/scene/SeenStepTracker.java src/main/java/com/eb/javafx/scene/SeenStepSnapshot.java src/test/java/com/eb/javafx/scene/SeenStepTrackerTest.java
git commit -m "feat(scene): add SeenStepTracker and SeenStepSnapshot for skip mode"
```

---

### Task 3.2: `ScenePlaybackMode` and `SceneExecutor.advanceSkipping()`

**Files:**
- Create: `src/main/java/com/eb/javafx/scene/ScenePlaybackMode.java`
- Modify: `src/main/java/com/eb/javafx/scene/SceneExecutor.java`
- Create: `src/test/java/com/eb/javafx/scene/SceneExecutorSkipModeTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class SceneExecutorSkipModeTest {

    private ActionContext actionContext() {
        GameState state = GameState.create(new GameClock(List.of(
            new com.eb.javafx.gamesupport.CodeDefinition("first", "First", Map.of()),
            new com.eb.javafx.gamesupport.CodeDefinition("second", "Second", Map.of())
        )));
        return new ActionContext(state, new GameRandomService(), List.of(), new CodeTableDefinition(List.of(), Map.of()));
    }

    @Test
    void skipModeBypassesSeenTextAndPausesOnUnseen() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("seen-line", "text.seen"),
            SceneStep.narration("unseen-line", "text.unseen")
        )));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene", "seen-line");

        SceneExecutionResult result = executor.advanceSkipping(context, executor.start("scene"), tracker);
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, result.status());
        assertEquals("text.unseen", result.step().textDefinition());
    }

    @Test
    void skipModeAlwaysStopsAtChoiceEvenIfPreviouslySeen() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("the-choice", List.of(
                SceneChoice.of("opt", "choice.opt", SceneTransition.complete())
            ))
        )));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene", "the-choice");

        SceneExecutionResult result = executor.advanceSkipping(context, executor.start("scene"), tracker);
        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, result.status());
    }

    @Test
    void skipModeCompletesWhenAllStepsAreSeen() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("line1", "text.line1"),
            SceneStep.narration("line2", "text.line2")
        )));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene", "line1");
        tracker.markSeen("scene", "line2");

        SceneExecutionResult result = executor.advanceSkipping(context, executor.start("scene"), tracker);
        assertEquals(SceneExecutionStatus.COMPLETED, result.status());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Create `ScenePlaybackMode`**

```java
package com.eb.javafx.scene;

/** Controls how the scene executor responds to text steps during playback. */
public enum ScenePlaybackMode {
    /** Normal play: pause on every text step. */
    NORMAL,
    /** Skip mode: bypass seen text steps; always stop at unseen steps and all CHOICE steps. */
    SKIP,
    /** Auto mode: text steps advance automatically after a UI-configured delay; engine advances headlessly like NORMAL. */
    AUTO
}
```

- [ ] **Step 4: Add `advanceSkipping()` to `SceneExecutor`**

Open `src/main/java/com/eb/javafx/scene/SceneExecutor.java` and add this method after `selectChoice()`:

```java
public SceneExecutionResult advanceSkipping(ActionContext context, SceneFlowState state, SeenStepTracker seenSteps) {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(seenSteps, "seenSteps");
    SceneFlowState current = Objects.requireNonNull(state, "state");
    while (true) {
        SceneDefinition scene = sceneRegistry.requireScene(current.activeSceneId());
        if (current.stepIndex() >= scene.steps().size()) {
            return complete(current, "Scene completed: " + scene.id());
        }
        SceneStep step = scene.steps().get(current.stepIndex());
        switch (step.type()) {
            case DIALOGUE, NARRATION -> {
                if (seenSteps.hasSeen(current.activeSceneId(), step.id())) {
                    seenSteps.markSeen(current.activeSceneId(), step.id());
                    current = applyTransition(current, step.transition());
                } else {
                    seenSteps.markSeen(current.activeSceneId(), step.id());
                    return new SceneExecutionResult(SceneExecutionStatus.DISPLAYING_TEXT, current, step, List.of(), null);
                }
            }
            case CHOICE -> {
                List<SceneChoice> availableChoices = step.choices().stream()
                        .filter(choice -> choice.availability(context).isAllowed())
                        .toList();
                return new SceneExecutionResult(SceneExecutionStatus.WAITING_FOR_CHOICE, current, step, availableChoices, null);
            }
            case ACTION -> {
                ActionResult result = applyEffects(context, step.effects());
                if (!result.success()) {
                    return fail(current, result.message());
                }
                current = applyTransition(current, step.transition());
            }
            case TRANSITION -> current = applyTransition(current, step.transition());
        }
    }
}
```

- [ ] **Step 5: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.SceneExecutorSkipModeTest"
```

- [ ] **Step 6: Commit**

```
git add src/main/java/com/eb/javafx/scene/ScenePlaybackMode.java src/main/java/com/eb/javafx/scene/SeenStepTracker.java src/main/java/com/eb/javafx/scene/SeenStepSnapshot.java src/main/java/com/eb/javafx/scene/SceneExecutor.java src/test/java/com/eb/javafx/scene/SceneExecutorSkipModeTest.java
git commit -m "feat(scene): add ScenePlaybackMode and SceneExecutor.advanceSkipping for skip mode"
```

---

## Task Group 4: NVL Mode

### Task 4.1: `SceneDisplayMode` metadata helper on `SceneStep`

**Files:**
- Create: `src/main/java/com/eb/javafx/scene/SceneDisplayMode.java`
- Modify: `src/main/java/com/eb/javafx/scene/SceneStep.java`
- Create: `src/test/java/com/eb/javafx/scene/SceneDisplayModeTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SceneDisplayModeTest {

    @Test
    void defaultDisplayModeIsAdv() {
        SceneStep step = SceneStep.narration("line", "text.line");
        assertEquals(SceneDisplayMode.ADV, step.displayMode());
    }

    @Test
    void withNvlDisplayModeSetsMetadata() {
        SceneStep step = SceneStep.narration("line", "text.line").withDisplayMode(SceneDisplayMode.NVL);
        assertEquals(SceneDisplayMode.NVL, step.displayMode());
    }

    @Test
    void dialogueStepDefaultsToAdv() {
        SceneStep step = SceneStep.dialogue("line", "hero", "text.hello", null);
        assertEquals(SceneDisplayMode.ADV, step.displayMode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Create `SceneDisplayMode`**

```java
package com.eb.javafx.scene;

/** Presentation mode hint for dialogue/narration steps. */
public enum SceneDisplayMode {
    /** Standard ADV mode: single dialogue box at the bottom of the screen. */
    ADV,
    /** NVL mode: full-screen text area where lines accumulate. */
    NVL
}
```

- [ ] **Step 4: Add `displayMode()` and `withDisplayMode()` to `SceneStep`**

In `src/main/java/com/eb/javafx/scene/SceneStep.java`, add after `withMetadataValue()`:

```java
public SceneDisplayMode displayMode() {
    String value = metadata.get("displayMode");
    if (value == null) return SceneDisplayMode.ADV;
    return SceneDisplayMode.valueOf(value);
}

public SceneStep withDisplayMode(SceneDisplayMode mode) {
    return withMetadataValue("displayMode", Objects.requireNonNull(mode, "mode").name());
}
```

- [ ] **Step 5: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.SceneDisplayModeTest"
```

- [ ] **Step 6: Verify no regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.*"
```

- [ ] **Step 7: Commit**

```
git add src/main/java/com/eb/javafx/scene/SceneDisplayMode.java src/main/java/com/eb/javafx/scene/SceneStep.java src/test/java/com/eb/javafx/scene/SceneDisplayModeTest.java
git commit -m "feat(scene): add SceneDisplayMode (NVL/ADV) as metadata hint on SceneStep"
```

---

## Task Group 5: Scene-Level Conditionals

### Task 5.1: `SceneConditionExpression` parser

**Files:**
- Create: `src/main/java/com/eb/javafx/scene/SceneConditionExpression.java`
- Create: `src/test/java/com/eb/javafx/scene/SceneConditionExpressionTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SceneConditionExpressionTest {

    @Test
    void parseFlagExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("flag:my_flag");
        assertEquals(SceneConditionExpression.Kind.FLAG, expr.kind());
        assertEquals("my_flag", expr.id());
        assertFalse(expr.negated());
    }

    @Test
    void parseNegatedFlagExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("!flag:my_flag");
        assertEquals(SceneConditionExpression.Kind.FLAG, expr.kind());
        assertTrue(expr.negated());
    }

    @Test
    void parseUnlockExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("unlock:cg_01");
        assertEquals(SceneConditionExpression.Kind.UNLOCK, expr.kind());
        assertEquals("cg_01", expr.id());
    }

    @Test
    void parseCounterGteExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("counter:score>=10");
        assertEquals(SceneConditionExpression.Kind.COUNTER, expr.kind());
        assertEquals("score", expr.id());
        assertEquals(SceneConditionExpression.CounterOp.GTE, expr.counterOp());
        assertEquals(10, expr.counterThreshold());
    }

    @Test
    void parseCounterEqExpression() {
        SceneConditionExpression expr = SceneConditionExpression.parse("counter:level==3");
        assertEquals(SceneConditionExpression.CounterOp.EQ, expr.counterOp());
        assertEquals(3, expr.counterThreshold());
    }

    @Test
    void rejectsMalformedExpression() {
        assertThrows(IllegalArgumentException.class, () -> SceneConditionExpression.parse("bad"));
        assertThrows(IllegalArgumentException.class, () -> SceneConditionExpression.parse("counter:noop"));
        assertThrows(IllegalArgumentException.class, () -> SceneConditionExpression.parse(""));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement**

```java
package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed condition expression for a CONDITIONAL scene step.
 *
 * <p>Supported forms:
 * <ul>
 *   <li>{@code flag:id} — checks ProgressTracker.hasFlag(id)</li>
 *   <li>{@code !flag:id} — negated flag check</li>
 *   <li>{@code unlock:id} — checks ProgressTracker.isUnlocked(id)</li>
 *   <li>{@code !unlock:id} — negated unlock check</li>
 *   <li>{@code counter:id>=N}, {@code counter:id>N}, {@code counter:id<N}, {@code counter:id<=N}, {@code counter:id==N}</li>
 * </ul>
 */
public final class SceneConditionExpression {
    private static final Pattern COUNTER_PATTERN =
        Pattern.compile("^(!?)counter:([\\w.\\-]+)(>=|>|<=|<|==)(-?\\d+)$");
    private static final Pattern FLAG_PATTERN =
        Pattern.compile("^(!?)(flag|unlock):([\\w.\\-]+)$");

    public enum Kind { FLAG, UNLOCK, COUNTER }
    public enum CounterOp { GT, GTE, LT, LTE, EQ }

    private final Kind kind;
    private final String id;
    private final boolean negated;
    private final CounterOp counterOp;
    private final int counterThreshold;

    private SceneConditionExpression(Kind kind, String id, boolean negated, CounterOp counterOp, int counterThreshold) {
        this.kind = kind;
        this.id = id;
        this.negated = negated;
        this.counterOp = counterOp;
        this.counterThreshold = counterThreshold;
    }

    public static SceneConditionExpression parse(String raw) {
        Validation.requireNonBlank(raw, "Condition expression is required.");
        Matcher counterMatcher = COUNTER_PATTERN.matcher(raw);
        if (counterMatcher.matches()) {
            boolean neg = !counterMatcher.group(1).isEmpty();
            String id = counterMatcher.group(2);
            CounterOp op = switch (counterMatcher.group(3)) {
                case ">=" -> CounterOp.GTE;
                case ">"  -> CounterOp.GT;
                case "<=" -> CounterOp.LTE;
                case "<"  -> CounterOp.LT;
                case "==" -> CounterOp.EQ;
                default -> throw new IllegalArgumentException("Unknown counter operator in: " + raw);
            };
            int threshold = Integer.parseInt(counterMatcher.group(4));
            return new SceneConditionExpression(Kind.COUNTER, id, neg, op, threshold);
        }
        Matcher flagMatcher = FLAG_PATTERN.matcher(raw);
        if (flagMatcher.matches()) {
            boolean neg = !flagMatcher.group(1).isEmpty();
            Kind kind = flagMatcher.group(2).equals("flag") ? Kind.FLAG : Kind.UNLOCK;
            String id = flagMatcher.group(3);
            return new SceneConditionExpression(kind, id, neg, null, 0);
        }
        throw new IllegalArgumentException("Unrecognised condition expression: " + raw);
    }

    public Kind kind() { return kind; }
    public String id() { return id; }
    public boolean negated() { return negated; }
    public CounterOp counterOp() { return counterOp; }
    public int counterThreshold() { return counterThreshold; }
}
```

- [ ] **Step 4: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.SceneConditionExpressionTest"
```

- [ ] **Step 5: Commit**

```
git add src/main/java/com/eb/javafx/scene/SceneConditionExpression.java src/test/java/com/eb/javafx/scene/SceneConditionExpressionTest.java
git commit -m "feat(scene): add SceneConditionExpression parser (flag/unlock/counter DSL)"
```

---

### Task 5.2: `SceneConditionEvaluator`

**Files:**
- Create: `src/main/java/com/eb/javafx/scene/SceneConditionEvaluator.java`
- Create: `src/test/java/com/eb/javafx/scene/SceneConditionEvaluatorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.scene;

import com.eb.javafx.progress.ProgressTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SceneConditionEvaluatorTest {
    private ProgressTracker progress;
    private SceneConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        progress = new ProgressTracker();
        evaluator = new SceneConditionEvaluator(progress);
    }

    @Test
    void flagTrueWhenSet() {
        progress.setFlag("hero_met", true);
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("flag:hero_met")));
    }

    @Test
    void flagFalseWhenNotSet() {
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("flag:hero_met")));
    }

    @Test
    void negatedFlagInvertsResult() {
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("!flag:hero_met")));
        progress.setFlag("hero_met", true);
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("!flag:hero_met")));
    }

    @Test
    void unlockEvaluatesCorrectly() {
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("unlock:cg_01")));
        progress.unlock("cg_01");
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("unlock:cg_01")));
    }

    @Test
    void counterGteEvaluatesCorrectly() {
        progress.incrementCounter("score", 5);
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("counter:score>=5")));
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("counter:score>=4")));
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("counter:score>=6")));
    }

    @Test
    void counterEqEvaluatesCorrectly() {
        progress.incrementCounter("level", 3);
        assertTrue(evaluator.evaluate(SceneConditionExpression.parse("counter:level==3")));
        assertFalse(evaluator.evaluate(SceneConditionExpression.parse("counter:level==4")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement**

```java
package com.eb.javafx.scene;

import com.eb.javafx.progress.ProgressTracker;
import java.util.Objects;

/** Evaluates a parsed SceneConditionExpression against live ProgressTracker state. */
public final class SceneConditionEvaluator {
    private final ProgressTracker progress;

    public SceneConditionEvaluator(ProgressTracker progress) {
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    public boolean evaluate(SceneConditionExpression expr) {
        Objects.requireNonNull(expr, "expr");
        boolean raw = switch (expr.kind()) {
            case FLAG -> progress.hasFlag(expr.id());
            case UNLOCK -> progress.isUnlocked(expr.id());
            case COUNTER -> evaluateCounter(expr);
        };
        return expr.negated() ? !raw : raw;
    }

    private boolean evaluateCounter(SceneConditionExpression expr) {
        int value = progress.counter(expr.id());
        return switch (expr.counterOp()) {
            case GT  -> value > expr.counterThreshold();
            case GTE -> value >= expr.counterThreshold();
            case LT  -> value < expr.counterThreshold();
            case LTE -> value <= expr.counterThreshold();
            case EQ  -> value == expr.counterThreshold();
        };
    }
}
```

- [ ] **Step 4: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.SceneConditionEvaluatorTest"
```

- [ ] **Step 5: Commit**

```
git add src/main/java/com/eb/javafx/scene/SceneConditionEvaluator.java src/test/java/com/eb/javafx/scene/SceneConditionEvaluatorTest.java
git commit -m "feat(scene): add SceneConditionEvaluator"
```

---

### Task 5.3: `CONDITIONAL` step type wired into `SceneExecutor`

**Files:**
- Modify: `src/main/java/com/eb/javafx/scene/SceneStepType.java`
- Modify: `src/main/java/com/eb/javafx/scene/SceneStep.java`
- Modify: `src/main/java/com/eb/javafx/scene/SceneExecutor.java`
- Create: `src/test/java/com/eb/javafx/scene/SceneExecutorConditionalTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.progress.ProgressTracker;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class SceneExecutorConditionalTest {

    private ActionContext contextWithProgress(ProgressTracker progress) {
        GameState state = GameState.create(new GameClock(List.of(
            new CodeDefinition("first", "First", Map.of())
        )));
        return new ActionContext(state, new GameRandomService(), List.of(), new CodeTableDefinition(List.of(), Map.of()));
    }

    @Test
    void conditionalTakesThenBranchWhenTrue() {
        ProgressTracker progress = new ProgressTracker();
        progress.setFlag("hero_met", true);

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("start", List.of(
            SceneStep.conditional("branch", "flag:hero_met",
                SceneTransition.jump("then_scene"),
                SceneTransition.jump("else_scene")),
            SceneStep.narration("fallthrough", "text.fallthrough")
        )));
        registry.register(SceneDefinition.of("then_scene", List.of(
            SceneStep.narration("then_line", "text.then")
        )));
        registry.register(SceneDefinition.of("else_scene", List.of(
            SceneStep.narration("else_line", "text.else")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, new SceneConditionEvaluator(progress));
        ActionContext context = contextWithProgress(progress);

        SceneExecutionResult result = executor.advanceUntilPause(context, executor.start("start"));
        assertEquals(SceneExecutionStatus.DISPLAYING_TEXT, result.status());
        assertEquals("text.then", result.step().textDefinition());
    }

    @Test
    void conditionalTakesElseBranchWhenFalse() {
        ProgressTracker progress = new ProgressTracker();

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("start", List.of(
            SceneStep.conditional("branch", "flag:hero_met",
                SceneTransition.jump("then_scene"),
                SceneTransition.jump("else_scene"))
        )));
        registry.register(SceneDefinition.of("then_scene", List.of(SceneStep.narration("t", "text.then"))));
        registry.register(SceneDefinition.of("else_scene", List.of(SceneStep.narration("e", "text.else"))));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, new SceneConditionEvaluator(progress));
        ActionContext context = contextWithProgress(progress);

        SceneExecutionResult result = executor.advanceUntilPause(context, executor.start("start"));
        assertEquals("text.else", result.step().textDefinition());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Add `CONDITIONAL` to `SceneStepType`**

```java
public enum SceneStepType {
    DIALOGUE,
    NARRATION,
    CHOICE,
    ACTION,
    TRANSITION,
    CONDITIONAL
}
```

- [ ] **Step 4: Add `conditional()` factory to `SceneStep`**

In `SceneStep.java`, add after the `transition()` factory:

```java
public static SceneStep conditional(String id, String conditionExpression, SceneTransition thenTransition, SceneTransition elseTransition) {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("conditionExpression", Validation.requireNonBlank(conditionExpression, "conditionExpression"));
    meta.put("elseTransitionType", Objects.requireNonNull(elseTransition, "elseTransition").type().name());
    if (elseTransition.targetSceneId() != null) {
        meta.put("elseTransitionTarget", elseTransition.targetSceneId());
    }
    return new SceneStep(id, SceneStepType.CONDITIONAL, null, null, null,
            List.of(), List.of(),
            Objects.requireNonNull(thenTransition, "thenTransition"),
            meta);
}

public String conditionExpression() {
    return metadata.get("conditionExpression");
}

public SceneTransition elseTransition() {
    String typeStr = metadata.get("elseTransitionType");
    if (typeStr == null) return SceneTransition.next();
    SceneTransitionType type = SceneTransitionType.valueOf(typeStr);
    String target = metadata.get("elseTransitionTarget");
    return switch (type) {
        case NEXT -> SceneTransition.next();
        case JUMP -> SceneTransition.jump(target);
        case CALL -> SceneTransition.call(target);
        case RETURN -> SceneTransition.returnToCaller();
        case COMPLETE -> SceneTransition.complete();
        case FAIL -> SceneTransition.fail(target);
    };
}
```

- [ ] **Step 5: Update `SceneExecutor` to accept `SceneConditionEvaluator` and handle `CONDITIONAL`**

Replace the `SceneExecutor` constructor with:

```java
private final SceneRegistry sceneRegistry;
private final SceneConditionEvaluator conditionEvaluator;

public SceneExecutor(SceneRegistry sceneRegistry) {
    this(sceneRegistry, null);
}

public SceneExecutor(SceneRegistry sceneRegistry, SceneConditionEvaluator conditionEvaluator) {
    this.sceneRegistry = Objects.requireNonNull(sceneRegistry, "sceneRegistry");
    this.conditionEvaluator = conditionEvaluator;
}
```

Add the `CONDITIONAL` case in `advanceUntilPause()` switch inside the `while(true)` loop (after the existing `TRANSITION` case):

```java
case CONDITIONAL -> {
    if (conditionEvaluator == null) {
        throw new IllegalStateException("SceneConditionEvaluator required for CONDITIONAL steps.");
    }
    SceneConditionExpression expr = SceneConditionExpression.parse(step.conditionExpression());
    boolean conditionMet = conditionEvaluator.evaluate(expr);
    current = applyTransition(current, conditionMet ? step.transition() : step.elseTransition());
}
```

- [ ] **Step 6: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.SceneExecutorConditionalTest"
```

- [ ] **Step 7: Run full scene tests to check for regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.*"
```

- [ ] **Step 8: Commit**

```
git add src/main/java/com/eb/javafx/scene/ src/test/java/com/eb/javafx/scene/SceneExecutorConditionalTest.java
git commit -m "feat(scene): add CONDITIONAL step type with flag/unlock/counter DSL"
```

---

## Task Group 6: Gallery Framework

### Task 6.1: `GalleryEntry` and `GalleryDefinition`

**Files:**
- Create: `src/main/java/com/eb/javafx/gallery/GalleryEntry.java`
- Create: `src/main/java/com/eb/javafx/gallery/GalleryDefinition.java`
- Create: `src/test/java/com/eb/javafx/gallery/GalleryDefinitionTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.gallery;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class GalleryDefinitionTest {

    @Test
    void constructsWithValidFields() {
        GalleryEntry entry = new GalleryEntry("cg_01", "img/cg_01.png", "gallery.cg_01.caption", "unlock_cg_01");
        GalleryDefinition def = new GalleryDefinition("main-gallery", "gallery.title", List.of(entry));
        assertEquals("main-gallery", def.id());
        assertEquals("gallery.title", def.titleTextKey());
        assertEquals(1, def.entries().size());
    }

    @Test
    void entryExposesFields() {
        GalleryEntry entry = new GalleryEntry("cg_02", "img/cg_02.png", "gallery.cg_02.caption", "unlock_cg_02");
        assertEquals("cg_02", entry.id());
        assertEquals("img/cg_02.png", entry.imageRef());
        assertEquals("gallery.cg_02.caption", entry.captionTextKey());
        assertEquals("unlock_cg_02", entry.requiredUnlockId());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> new GalleryDefinition("", "title", List.of()));
    }

    @Test
    void entriesListIsImmutable() {
        GalleryDefinition def = new GalleryDefinition("g", "title", List.of());
        assertThrows(UnsupportedOperationException.class, () -> def.entries().add(
            new GalleryEntry("x", "img/x.png", "cap", "unlock_x")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement `GalleryEntry`**

```java
package com.eb.javafx.gallery;

import com.eb.javafx.util.Validation;

/** One viewable image in a gallery, gated behind a progress unlock. */
public final class GalleryEntry {
    private final String id;
    private final String imageRef;
    private final String captionTextKey;
    private final String requiredUnlockId;

    public GalleryEntry(String id, String imageRef, String captionTextKey, String requiredUnlockId) {
        this.id = Validation.requireNonBlank(id, "Gallery entry id is required.");
        this.imageRef = Validation.requireNonBlank(imageRef, "Gallery entry imageRef is required.");
        this.captionTextKey = Validation.requireNonBlank(captionTextKey, "Gallery entry captionTextKey is required.");
        this.requiredUnlockId = Validation.requireNonBlank(requiredUnlockId, "Gallery entry requiredUnlockId is required.");
    }

    public String id() { return id; }
    public String imageRef() { return imageRef; }
    public String captionTextKey() { return captionTextKey; }
    public String requiredUnlockId() { return requiredUnlockId; }
}
```

- [ ] **Step 4: Implement `GalleryDefinition`**

```java
package com.eb.javafx.gallery;

import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Objects;

/** Named collection of gallery entries presented together in a gallery screen. */
public final class GalleryDefinition {
    private final String id;
    private final String titleTextKey;
    private final List<GalleryEntry> entries;

    public GalleryDefinition(String id, String titleTextKey, List<GalleryEntry> entries) {
        this.id = Validation.requireNonBlank(id, "Gallery id is required.");
        this.titleTextKey = Validation.requireNonBlank(titleTextKey, "Gallery titleTextKey is required.");
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public String id() { return id; }
    public String titleTextKey() { return titleTextKey; }
    public List<GalleryEntry> entries() { return entries; }
}
```

- [ ] **Step 5: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.gallery.GalleryDefinitionTest"
```

- [ ] **Step 6: Commit**

```
git add src/main/java/com/eb/javafx/gallery/GalleryEntry.java src/main/java/com/eb/javafx/gallery/GalleryDefinition.java src/test/java/com/eb/javafx/gallery/GalleryDefinitionTest.java
git commit -m "feat(gallery): add GalleryEntry and GalleryDefinition"
```

---

### Task 6.2: `GalleryRegistry`

**Files:**
- Create: `src/main/java/com/eb/javafx/gallery/GalleryRegistry.java`
- Create: `src/test/java/com/eb/javafx/gallery/GalleryRegistryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.gallery;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class GalleryRegistryTest {

    @Test
    void registersAndFindsGallery() {
        GalleryRegistry registry = new GalleryRegistry();
        registry.register(new GalleryDefinition("main", "gallery.title", List.of(
            new GalleryEntry("cg_01", "img/cg_01.png", "gallery.cg_01", "unlock_cg_01")
        )));
        Optional<GalleryDefinition> found = registry.find("main");
        assertTrue(found.isPresent());
        assertEquals(1, found.get().entries().size());
    }

    @Test
    void returnsEmptyForUnknownGallery() {
        GalleryRegistry registry = new GalleryRegistry();
        assertTrue(registry.find("unknown").isEmpty());
    }

    @Test
    void rejectsDuplicateGalleryId() {
        GalleryRegistry registry = new GalleryRegistry();
        registry.register(new GalleryDefinition("g", "title", List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> registry.register(new GalleryDefinition("g", "title2", List.of())));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement**

```java
package com.eb.javafx.gallery;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Startup registry of named gallery definitions. */
public final class GalleryRegistry {
    private final Map<String, GalleryDefinition> galleries = new LinkedHashMap<>();

    public void register(GalleryDefinition definition) {
        Validation.requireNonNull(definition, "definition");
        if (galleries.containsKey(definition.id())) {
            throw new IllegalArgumentException("Duplicate gallery id: " + definition.id());
        }
        galleries.put(definition.id(), definition);
    }

    public Optional<GalleryDefinition> find(String id) {
        return Optional.ofNullable(galleries.get(id));
    }

    public GalleryDefinition require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown gallery: " + id));
    }
}
```

- [ ] **Step 4: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.gallery.GalleryRegistryTest"
```

- [ ] **Step 5: Commit**

```
git add src/main/java/com/eb/javafx/gallery/GalleryRegistry.java src/test/java/com/eb/javafx/gallery/GalleryRegistryTest.java
git commit -m "feat(gallery): add GalleryRegistry"
```

---

### Task 6.3: `GalleryEntryViewModel` and `GalleryService`

**Files:**
- Create: `src/main/java/com/eb/javafx/gallery/GalleryEntryViewModel.java`
- Create: `src/main/java/com/eb/javafx/gallery/GalleryService.java`
- Modify: `src/main/java/module-info.java`
- Create: `src/test/java/com/eb/javafx/gallery/GalleryServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.eb.javafx.gallery;

import com.eb.javafx.progress.ProgressTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class GalleryServiceTest {
    private GalleryRegistry registry;
    private ProgressTracker progress;
    private GalleryService service;

    @BeforeEach
    void setUp() {
        registry = new GalleryRegistry();
        registry.register(new GalleryDefinition("main", "gallery.title", List.of(
            new GalleryEntry("cg_01", "img/cg_01.png", "gallery.cg_01", "unlock_cg_01"),
            new GalleryEntry("cg_02", "img/cg_02.png", "gallery.cg_02", "unlock_cg_02")
        )));
        progress = new ProgressTracker();
        service = new GalleryService(registry, progress);
    }

    @Test
    void lockedEntriesReturnUnlockedFalseAndNoImageRef() {
        List<GalleryEntryViewModel> views = service.viewModels("main");
        assertEquals(2, views.size());
        GalleryEntryViewModel first = views.get(0);
        assertFalse(first.unlocked());
        assertTrue(first.imageRef().isEmpty());
    }

    @Test
    void unlockedEntryExposesImageRef() {
        progress.unlock("unlock_cg_01");
        List<GalleryEntryViewModel> views = service.viewModels("main");
        GalleryEntryViewModel first = views.get(0);
        assertTrue(first.unlocked());
        assertEquals("img/cg_01.png", first.imageRef().get());
    }

    @Test
    void mixedLockedAndUnlocked() {
        progress.unlock("unlock_cg_02");
        List<GalleryEntryViewModel> views = service.viewModels("main");
        assertFalse(views.get(0).unlocked());
        assertTrue(views.get(1).unlocked());
    }

    @Test
    void throwsForUnknownGallery() {
        assertThrows(IllegalArgumentException.class, () -> service.viewModels("nonexistent"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```

- [ ] **Step 3: Implement `GalleryEntryViewModel`**

```java
package com.eb.javafx.gallery;

import java.util.Optional;

/** Read-only gallery entry view model: exposes image ref only when unlocked. */
public final class GalleryEntryViewModel {
    private final String id;
    private final String captionTextKey;
    private final boolean unlocked;
    private final String imageRef;

    GalleryEntryViewModel(String id, String captionTextKey, boolean unlocked, String imageRef) {
        this.id = id;
        this.captionTextKey = captionTextKey;
        this.unlocked = unlocked;
        this.imageRef = imageRef;
    }

    public String id() { return id; }
    public String captionTextKey() { return captionTextKey; }
    public boolean unlocked() { return unlocked; }
    public Optional<String> imageRef() { return Optional.ofNullable(unlocked ? imageRef : null); }
}
```

- [ ] **Step 4: Implement `GalleryService`**

```java
package com.eb.javafx.gallery;

import com.eb.javafx.progress.ProgressTracker;

import java.util.List;
import java.util.Objects;

/** Queries a GalleryRegistry and ProgressTracker to produce GalleryEntryViewModels. */
public final class GalleryService {
    private final GalleryRegistry registry;
    private final ProgressTracker progress;

    public GalleryService(GalleryRegistry registry, ProgressTracker progress) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    public List<GalleryEntryViewModel> viewModels(String galleryId) {
        GalleryDefinition def = registry.require(galleryId);
        return def.entries().stream()
            .map(entry -> {
                boolean unlocked = progress.isUnlocked(entry.requiredUnlockId());
                return new GalleryEntryViewModel(entry.id(), entry.captionTextKey(), unlocked, entry.imageRef());
            })
            .toList();
    }
}
```

- [ ] **Step 5: Export package in module-info.java**

Add after the last `exports` line:

```java
    exports com.eb.javafx.gallery;
```

- [ ] **Step 6: Run test**

```
./gradlew --no-daemon test --tests "com.eb.javafx.gallery.GalleryServiceTest"
```

- [ ] **Step 7: Commit**

```
git add src/main/java/com/eb/javafx/gallery/ src/main/java/module-info.java src/test/java/com/eb/javafx/gallery/GalleryServiceTest.java
git commit -m "feat(gallery): add GalleryEntryViewModel and GalleryService"
```

---

## Final Validation

After all task groups are complete, run the full scene and display test suites to confirm no regressions:

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.*" --tests "com.eb.javafx.display.*" --tests "com.eb.javafx.transitions.*" --tests "com.eb.javafx.gallery.*"
```

Expected: All tests PASS.

---

## Self-Review Checklist

- [x] **Spec coverage:** All 6 items from the TODO are covered by a task group.
- [x] **No placeholders:** Every step contains actual Java code and exact Gradle commands.
- [x] **Type consistency:** `SceneConditionExpression`, `SeenStepTracker`, `VisualTransitionDefinition` etc. are defined before they are used in later tasks.
- [x] **module-info:** `transitions` and `gallery` packages are exported in Tasks 1.4 and 6.3 respectively.
- [x] **Existing API preserved:** `SceneExecutor(SceneRegistry)` single-arg constructor kept; CONDITIONAL evaluator is optional so existing tests don't break.
