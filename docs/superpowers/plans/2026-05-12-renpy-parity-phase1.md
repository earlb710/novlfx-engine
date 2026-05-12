# Ren'Py Parity Phase 1 — Foundational Scene Mechanics

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add six core Ren'Py scene mechanics to the novlfx engine: dialogue rollback, conditional choice visibility, text pacing tags, timed choices, menu captions, and sticky/loop menus.

**Architecture:** All changes are in `com.eb.javafx.scene` and `com.eb.javafx.text`. The rollback system introduces a `RollbackBuffer` that snapshots scene flow state before each pause, using the existing `SaveSnapshotCodec<T>` interface for persistence-capable contributors. Conditional choices, pacing tags, timed choices, captions, and loop menus are added via metadata-backed accessors on existing types — no new step types needed.

**Tech Stack:** Java 17, Gradle (`./gradlew --no-daemon`), JUnit 5. All tests in `src/test/java/com/eb/javafx/...` matching the production package.

---

> **Note on scope:** This is Phase 1 of 4. Phases 2–4 (display completeness, audio completeness, UI/meta polish) will be separate plans. Each phase is independently shippable.

---

## File map

**New files:**
- `src/main/java/com/eb/javafx/scene/RollbackContributor.java`
- `src/main/java/com/eb/javafx/scene/PersistableRollbackContributor.java`
- `src/main/java/com/eb/javafx/scene/RollbackEntry.java`
- `src/main/java/com/eb/javafx/scene/RollbackBuffer.java`
- `src/main/java/com/eb/javafx/scene/RollbackSnapshotCodec.java`
- `src/main/java/com/eb/javafx/scene/ConditionPolicy.java`
- `src/test/java/com/eb/javafx/scene/RollbackBufferTest.java`
- `src/test/java/com/eb/javafx/scene/SceneExecutorRollbackTest.java`
- `src/test/java/com/eb/javafx/scene/ConditionalChoiceVisibilityTest.java`
- `src/test/java/com/eb/javafx/scene/TextPacingTagsTest.java`
- `src/test/java/com/eb/javafx/scene/TimedChoiceMenuTest.java`
- `src/test/java/com/eb/javafx/scene/LoopMenuTest.java`

**Modified files:**
- `src/main/java/com/eb/javafx/scene/SceneExecutionResult.java` — add `canRollback` field
- `src/main/java/com/eb/javafx/scene/SceneExecutor.java` — rollback support, condition filtering, loop menu
- `src/main/java/com/eb/javafx/scene/SceneChoice.java` — condition expression + policy + exitsMenu
- `src/main/java/com/eb/javafx/scene/SceneStep.java` — choiceTimeoutMs, choiceTimeoutDefaultId, menuCaptionTextKey, menuLoop
- `src/main/java/com/eb/javafx/text/TextTokenType.java` — WAIT_CLICK, NO_WAIT, SET_CPS, FAST_FORWARD
- `src/main/java/com/eb/javafx/text/TextToken.java` — factory methods for new token types
- `src/main/java/com/eb/javafx/text/TextTagParser.java` — handle new pacing tags

---

## Task 1: RollbackContributor interfaces

**Files:**
- Create: `src/main/java/com/eb/javafx/scene/RollbackContributor.java`
- Create: `src/main/java/com/eb/javafx/scene/PersistableRollbackContributor.java`

- [ ] **Step 1.1: Write the failing test**

```java
// src/test/java/com/eb/javafx/scene/RollbackBufferTest.java
package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RollbackBufferTest {

    @Test
    void contributorCapturesAndRestores() {
        int[] value = {42};
        RollbackContributor<Integer> contributor = new RollbackContributor<>() {
            @Override public Integer capture() { return value[0]; }
            @Override public void restore(Integer snapshot) { value[0] = snapshot; }
        };

        Integer snapshot = contributor.capture();
        value[0] = 99;
        contributor.restore(snapshot);
        assertEquals(42, value[0]);
    }
}
```

- [ ] **Step 1.2: Run test to verify it fails**

```
./gradlew --no-daemon testClasses
```
Expected: compilation error — `RollbackContributor` not found.

- [ ] **Step 1.3: Create RollbackContributor**

```java
// src/main/java/com/eb/javafx/scene/RollbackContributor.java
package com.eb.javafx.scene;

public interface RollbackContributor<T> {
    T capture();
    void restore(T snapshot);
}
```

- [ ] **Step 1.4: Create PersistableRollbackContributor**

```java
// src/main/java/com/eb/javafx/scene/PersistableRollbackContributor.java
package com.eb.javafx.scene;

import com.eb.javafx.save.SaveSnapshotCodec;

public interface PersistableRollbackContributor<T> extends RollbackContributor<T>, SaveSnapshotCodec<T> {
}
```

- [ ] **Step 1.5: Run test to verify it passes**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.RollbackBufferTest"
```
Expected: PASS.

- [ ] **Step 1.6: Commit**

```
git add src/main/java/com/eb/javafx/scene/RollbackContributor.java
git add src/main/java/com/eb/javafx/scene/PersistableRollbackContributor.java
git add src/test/java/com/eb/javafx/scene/RollbackBufferTest.java
git commit -m "feat(scene): add RollbackContributor and PersistableRollbackContributor interfaces"
```

---

## Task 2: RollbackEntry and RollbackBuffer

**Files:**
- Create: `src/main/java/com/eb/javafx/scene/RollbackEntry.java`
- Create: `src/main/java/com/eb/javafx/scene/RollbackBuffer.java`
- Modify: `src/test/java/com/eb/javafx/scene/RollbackBufferTest.java`

- [ ] **Step 2.1: Write failing tests**

Add to `RollbackBufferTest`:

```java
    private static ActionContext actionContext() {
        com.eb.javafx.random.GameRandomService randomService = new com.eb.javafx.random.GameRandomService();
        randomService.initialize();
        com.eb.javafx.gamesupport.CodeTableDefinition timeSlots =
            new com.eb.javafx.gamesupport.CodeTableDefinition("time-slots", "Time Slots",
                java.util.List.of(new com.eb.javafx.gamesupport.CodeDefinition("first", "First", 10, java.util.List.of())));
        return new com.eb.javafx.gamesupport.ActionContext(
            new com.eb.javafx.state.GameState("main-menu"),
            randomService,
            new com.eb.javafx.gamesupport.GameClock(timeSlots));
    }

    @Test
    void bufferSnapshotsAndRestoresContributors() {
        int[] value = {10};
        RollbackContributor<Integer> contributor = new RollbackContributor<>() {
            @Override public Integer capture() { return value[0]; }
            @Override public void restore(Integer snapshot) { value[0] = snapshot; }
        };
        RollbackBuffer buffer = new RollbackBuffer(10);
        buffer.register("counter", contributor);

        SceneFlowState state = SceneFlowState.start("scene1");
        buffer.snapshot(state);
        value[0] = 99;
        buffer.snapshot(SceneFlowState.start("scene2"));

        assertTrue(buffer.canRollback());

        buffer.pop(); // discard current
        RollbackEntry previous = buffer.pop().orElseThrow();
        buffer.restore(previous);

        assertEquals(10, value[0]);
        assertEquals("scene1", previous.flowState().activeSceneId());
    }

    @Test
    void bufferCapacityEvidesOldestEntries() {
        RollbackBuffer buffer = new RollbackBuffer(3);
        for (int i = 0; i < 5; i++) {
            buffer.snapshot(SceneFlowState.start("scene" + i));
        }
        assertEquals(3, buffer.size());
    }

    @Test
    void canRollbackRequiresTwoEntries() {
        RollbackBuffer buffer = new RollbackBuffer(10);
        assertFalse(buffer.canRollback());
        buffer.snapshot(SceneFlowState.start("s1"));
        assertFalse(buffer.canRollback());
        buffer.snapshot(SceneFlowState.start("s2"));
        assertTrue(buffer.canRollback());
    }
```

- [ ] **Step 2.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `RollbackEntry` and `RollbackBuffer` not found.

- [ ] **Step 2.3: Create RollbackEntry**

```java
// src/main/java/com/eb/javafx/scene/RollbackEntry.java
package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.Map;

public record RollbackEntry(SceneFlowState flowState, Map<String, Object> contributorValues) {
    public RollbackEntry {
        Validation.requireNonNull(flowState, "flowState");
        contributorValues = Map.copyOf(Validation.requireNonNull(contributorValues, "contributorValues"));
    }
}
```

- [ ] **Step 2.4: Create RollbackBuffer**

```java
// src/main/java/com/eb/javafx/scene/RollbackBuffer.java
package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RollbackBuffer {
    private final int capacity;
    private final Map<String, RollbackContributor<?>> contributors = new LinkedHashMap<>();
    private final ArrayDeque<RollbackEntry> entries;

    public RollbackBuffer(int capacity) {
        this.capacity = Validation.requirePositive(capacity, "Rollback buffer capacity must be positive.");
        this.entries = new ArrayDeque<>(capacity + 1);
    }

    public <T> void register(String id, RollbackContributor<T> contributor) {
        contributors.put(
            Validation.requireNonBlank(id, "Rollback contributor id is required."),
            Validation.requireNonNull(contributor, "contributor"));
    }

    public void snapshot(SceneFlowState flowState) {
        Validation.requireNonNull(flowState, "flowState");
        Map<String, Object> values = new LinkedHashMap<>();
        contributors.forEach((id, contributor) -> values.put(id, contributor.capture()));
        entries.addLast(new RollbackEntry(flowState, values));
        if (entries.size() > capacity) {
            entries.removeFirst();
        }
    }

    public Optional<RollbackEntry> pop() {
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.removeLast());
    }

    @SuppressWarnings("unchecked")
    public void restore(RollbackEntry entry) {
        Validation.requireNonNull(entry, "entry");
        contributors.forEach((id, contributor) -> {
            Object value = entry.contributorValues().get(id);
            if (value != null) {
                ((RollbackContributor<Object>) contributor).restore(value);
            }
        });
    }

    public boolean canRollback() {
        return entries.size() >= 2;
    }

    public int size() {
        return entries.size();
    }

    public Map<String, RollbackContributor<?>> contributors() {
        return Map.copyOf(contributors);
    }
}
```

- [ ] **Step 2.5: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.RollbackBufferTest"
```
Expected: PASS (all 4 tests including the contributor interface test from Task 1).

- [ ] **Step 2.6: Commit**

```
git add src/main/java/com/eb/javafx/scene/RollbackEntry.java
git add src/main/java/com/eb/javafx/scene/RollbackBuffer.java
git add src/test/java/com/eb/javafx/scene/RollbackBufferTest.java
git commit -m "feat(scene): add RollbackEntry record and RollbackBuffer ring buffer"
```

---

## Task 3: Wire rollback into SceneExecutor and SceneExecutionResult

**Files:**
- Modify: `src/main/java/com/eb/javafx/scene/SceneExecutionResult.java`
- Modify: `src/main/java/com/eb/javafx/scene/SceneExecutor.java`
- Create: `src/test/java/com/eb/javafx/scene/SceneExecutorRollbackTest.java`

- [ ] **Step 3.1: Write failing test**

```java
// src/test/java/com/eb/javafx/scene/SceneExecutorRollbackTest.java
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

final class SceneExecutorRollbackTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots",
            List.of(new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void rollbackRestoresPreviousDialogueLine() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("line1", "text.line1"),
            SceneStep.narration("line2", "text.line2")
        )));
        registry.validateScenes();

        RollbackBuffer buffer = new RollbackBuffer(10);
        SceneExecutor executor = new SceneExecutor(registry, null, buffer);
        ActionContext context = actionContext();

        SceneExecutionResult result1 = executor.advanceUntilPause(context, executor.start("scene"));
        assertEquals("text.line1", result1.step().textDefinition());
        assertFalse(result1.canRollback());

        SceneExecutionResult result2 = executor.continueFromText(context, result1.state());
        assertEquals("text.line2", result2.step().textDefinition());
        assertTrue(result2.canRollback());

        SceneExecutionResult rolledBack = executor.rollback(context);
        assertEquals("text.line1", rolledBack.step().textDefinition());
        assertFalse(rolledBack.canRollback());
    }

    @Test
    void rollbackThrowsWhenNothingToRollBackTo() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("line1", "text.line1")
        )));
        registry.validateScenes();

        RollbackBuffer buffer = new RollbackBuffer(10);
        SceneExecutor executor = new SceneExecutor(registry, null, buffer);

        executor.advanceUntilPause(actionContext(), executor.start("scene"));
        assertThrows(IllegalStateException.class, () -> executor.rollback(actionContext()));
    }

    @Test
    void canRollbackIsFalseWithNoBuffer() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("line1", "text.line1")
        )));
        registry.validateScenes();
        SceneExecutor executor = new SceneExecutor(registry);

        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));
        assertFalse(result.canRollback());
    }
}
```

- [ ] **Step 3.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `canRollback()` not found on `SceneExecutionResult`, three-arg `SceneExecutor` constructor not found.

- [ ] **Step 3.3: Add canRollback to SceneExecutionResult**

Replace the single constructor in `SceneExecutionResult.java` with:

```java
    private final boolean canRollback;

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message, boolean canRollback) {
        this.status = status;
        this.state = state;
        this.step = step;
        this.availableChoices = List.copyOf(availableChoices);
        this.message = message;
        this.canRollback = canRollback;
    }

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message) {
        this(status, state, step, availableChoices, message, false);
    }

    public boolean canRollback() {
        return canRollback;
    }
```

- [ ] **Step 3.4: Add RollbackBuffer field and three-arg constructor to SceneExecutor**

Add to the top of `SceneExecutor`:

```java
    private final RollbackBuffer rollbackBuffer;
```

Add new constructor (keep existing two-arg and one-arg constructors):

```java
    public SceneExecutor(SceneRegistry sceneRegistry, SceneConditionEvaluator conditionEvaluator, RollbackBuffer rollbackBuffer) {
        this.sceneRegistry = Objects.requireNonNull(sceneRegistry, "sceneRegistry");
        this.conditionEvaluator = conditionEvaluator;
        this.rollbackBuffer = rollbackBuffer;
    }
```

Update the existing two-arg constructor to delegate:

```java
    public SceneExecutor(SceneRegistry sceneRegistry, SceneConditionEvaluator conditionEvaluator) {
        this(sceneRegistry, conditionEvaluator, null);
    }
```

- [ ] **Step 3.5: Snapshot before each pause in advanceUntilPause**

In `SceneExecutor.advanceUntilPause`, replace the DIALOGUE/NARRATION and CHOICE cases:

```java
                case DIALOGUE, NARRATION -> {
                    if (rollbackBuffer != null) rollbackBuffer.snapshot(current);
                    return new SceneExecutionResult(SceneExecutionStatus.DISPLAYING_TEXT, current, step, List.of(), null,
                            rollbackBuffer != null && rollbackBuffer.canRollback());
                }
                case CHOICE -> {
                    if (rollbackBuffer != null) rollbackBuffer.snapshot(current);
                    List<SceneChoice> availableChoices = step.choices().stream()
                            .filter(choice -> choice.availability(context).isAllowed())
                            .toList();
                    return new SceneExecutionResult(SceneExecutionStatus.WAITING_FOR_CHOICE, current, step, availableChoices, null,
                            rollbackBuffer != null && rollbackBuffer.canRollback());
                }
```

- [ ] **Step 3.6: Add rollback() method to SceneExecutor**

Add after `advanceSkipping`:

```java
    public SceneExecutionResult rollback(ActionContext context) {
        if (rollbackBuffer == null || !rollbackBuffer.canRollback()) {
            throw new IllegalStateException("Rollback is not available.");
        }
        rollbackBuffer.pop(); // discard current step snapshot
        RollbackEntry previous = rollbackBuffer.pop().orElseThrow();
        rollbackBuffer.restore(previous);
        return advanceUntilPause(context, previous.flowState());
    }
```

- [ ] **Step 3.7: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.SceneExecutorRollbackTest"
```
Expected: PASS (3 tests).

- [ ] **Step 3.8: Run broader scene tests to check no regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.*"
```
Expected: all pass.

- [ ] **Step 3.9: Commit**

```
git add src/main/java/com/eb/javafx/scene/SceneExecutionResult.java
git add src/main/java/com/eb/javafx/scene/SceneExecutor.java
git add src/test/java/com/eb/javafx/scene/SceneExecutorRollbackTest.java
git commit -m "feat(scene): wire RollbackBuffer into SceneExecutor; add canRollback to SceneExecutionResult"
```

---

## Task 4: ConditionPolicy and SceneChoice condition support

**Files:**
- Create: `src/main/java/com/eb/javafx/scene/ConditionPolicy.java`
- Modify: `src/main/java/com/eb/javafx/scene/SceneChoice.java`
- Create: `src/test/java/com/eb/javafx/scene/ConditionalChoiceVisibilityTest.java`

- [ ] **Step 4.1: Write failing test**

```java
// src/test/java/com/eb/javafx/scene/ConditionalChoiceVisibilityTest.java
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

import static org.junit.jupiter.api.Assertions.*;

final class ConditionalChoiceVisibilityTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots",
            List.of(new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void sceneChoiceStoresConditionExpressionAndPolicy() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.complete())
            .withCondition("flag:unlocked", ConditionPolicy.HIDE);

        assertEquals("flag:unlocked", choice.conditionExpression());
        assertEquals(ConditionPolicy.HIDE, choice.conditionPolicy());
    }

    @Test
    void hidePolicyExcludesFalseChoiceFromResult() {
        ProgressTracker progress = new ProgressTracker(); // flag not set
        SceneConditionEvaluator evaluator = new SceneConditionEvaluator(progress);

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("always", "text.always", SceneTransition.complete()),
                SceneChoice.of("gated", "text.gated", SceneTransition.complete())
                    .withCondition("flag:unlocked", ConditionPolicy.HIDE)
            ))
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, evaluator);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertEquals(1, result.availableChoices().size());
        assertEquals("always", result.availableChoices().get(0).id());
    }

    @Test
    void greyPolicyIncludesFalseChoiceAsDisabled() {
        ProgressTracker progress = new ProgressTracker(); // flag not set
        SceneConditionEvaluator evaluator = new SceneConditionEvaluator(progress);

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("always", "text.always", SceneTransition.complete()),
                SceneChoice.of("gated", "text.gated", SceneTransition.complete())
                    .withCondition("flag:unlocked", ConditionPolicy.GREY)
            ))
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, evaluator);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertEquals(2, result.availableChoices().size());
        SceneChoice greyed = result.availableChoices().stream()
            .filter(c -> c.id().equals("gated")).findFirst().orElseThrow();
        assertFalse(greyed.availability(actionContext()).isAllowed());
    }

    @Test
    void conditionMetAlwaysIncludesChoice() {
        ProgressTracker progress = new ProgressTracker();
        progress.setFlag("unlocked");
        SceneConditionEvaluator evaluator = new SceneConditionEvaluator(progress);

        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("gated", "text.gated", SceneTransition.complete())
                    .withCondition("flag:unlocked", ConditionPolicy.HIDE)
            ))
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry, evaluator);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertEquals(1, result.availableChoices().size());
        assertEquals("gated", result.availableChoices().get(0).id());
    }
}
```

- [ ] **Step 4.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `ConditionPolicy` and `withCondition` not found.

- [ ] **Step 4.3: Create ConditionPolicy enum**

```java
// src/main/java/com/eb/javafx/scene/ConditionPolicy.java
package com.eb.javafx.scene;

public enum ConditionPolicy {
    HIDE,
    GREY
}
```

- [ ] **Step 4.4: Add condition accessors to SceneChoice**

Add to `SceneChoice.java` after `withIcon`:

```java
    public String conditionExpression() {
        return metadata.get("conditionExpression");
    }

    public ConditionPolicy conditionPolicy() {
        String value = metadata.get("conditionPolicy");
        return value == null ? ConditionPolicy.HIDE : ConditionPolicy.valueOf(value);
    }

    public SceneChoice withCondition(String expression, ConditionPolicy policy) {
        java.util.Map<String, String> updated = new LinkedHashMap<>(metadata);
        updated.put("conditionExpression", Validation.requireNonBlank(expression, "Condition expression is required."));
        updated.put("conditionPolicy", Objects.requireNonNull(policy, "policy").name());
        return withMetadata(updated);
    }
```

- [ ] **Step 4.5: Add condition filtering to SceneExecutor's CHOICE handling**

Replace the CHOICE case in `advanceUntilPause` with:

```java
                case CHOICE -> {
                    if (rollbackBuffer != null) rollbackBuffer.snapshot(current);
                    List<SceneChoice> resolvedChoices = resolveChoices(step.choices(), context);
                    return new SceneExecutionResult(SceneExecutionStatus.WAITING_FOR_CHOICE, current, step, resolvedChoices, null,
                            rollbackBuffer != null && rollbackBuffer.canRollback());
                }
```

Add private helper method to `SceneExecutor`:

```java
    private List<SceneChoice> resolveChoices(List<SceneChoice> choices, ActionContext context) {
        return choices.stream()
                .flatMap(choice -> {
                    String expr = choice.conditionExpression();
                    if (expr == null || conditionEvaluator == null) {
                        return choice.availability(context).isAllowed()
                                ? java.util.stream.Stream.of(choice)
                                : java.util.stream.Stream.empty();
                    }
                    boolean conditionMet = conditionEvaluator.evaluate(SceneConditionExpression.parse(expr));
                    if (conditionMet) {
                        return choice.availability(context).isAllowed()
                                ? java.util.stream.Stream.of(choice)
                                : java.util.stream.Stream.empty();
                    }
                    return switch (choice.conditionPolicy()) {
                        case HIDE -> java.util.stream.Stream.empty();
                        case GREY -> java.util.stream.Stream.of(
                                choice.disabled(choice.disabledReason() != null
                                        ? choice.disabledReason()
                                        : "Not available"));
                    };
                })
                .toList();
    }
```

Also update the CHOICE case in `advanceSkipping` to use `resolveChoices`:

```java
                case CHOICE -> {
                    List<SceneChoice> resolvedChoices = resolveChoices(step.choices(), context);
                    return new SceneExecutionResult(SceneExecutionStatus.WAITING_FOR_CHOICE, current, step, resolvedChoices, null);
                }
```

- [ ] **Step 4.6: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.ConditionalChoiceVisibilityTest"
```
Expected: PASS (4 tests).

- [ ] **Step 4.7: Run scene tests for regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.*"
```
Expected: all pass.

- [ ] **Step 4.8: Commit**

```
git add src/main/java/com/eb/javafx/scene/ConditionPolicy.java
git add src/main/java/com/eb/javafx/scene/SceneChoice.java
git add src/main/java/com/eb/javafx/scene/SceneExecutor.java
git add src/test/java/com/eb/javafx/scene/ConditionalChoiceVisibilityTest.java
git commit -m "feat(scene): add ConditionPolicy and per-choice condition expression with HIDE/GREY support"
```

---

## Task 5: Text pacing control tags

**Files:**
- Modify: `src/main/java/com/eb/javafx/text/TextTokenType.java`
- Modify: `src/main/java/com/eb/javafx/text/TextToken.java`
- Modify: `src/main/java/com/eb/javafx/text/TextTagParser.java`
- Create: `src/test/java/com/eb/javafx/scene/TextPacingTagsTest.java`

- [ ] **Step 5.1: Write failing test**

```java
// src/test/java/com/eb/javafx/scene/TextPacingTagsTest.java
package com.eb.javafx.scene;

import com.eb.javafx.text.TextTagParser;
import com.eb.javafx.text.TextToken;
import com.eb.javafx.text.TextTokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TextPacingTagsTest {

    private final TextTagParser parser = new TextTagParser();

    @Test
    void waitClickTagProducesWaitClickToken() {
        List<TextToken> tokens = parser.parse("Hello{w}world");
        assertEquals(3, tokens.size());
        assertEquals(TextTokenType.WAIT_CLICK, tokens.get(1).type());
    }

    @Test
    void noWaitTagProducesNoWaitToken() {
        List<TextToken> tokens = parser.parse("Fast{nw}");
        assertEquals(2, tokens.size());
        assertEquals(TextTokenType.NO_WAIT, tokens.get(1).type());
    }

    @Test
    void cpsTagProducesSetCpsToken() {
        List<TextToken> tokens = parser.parse("{cps=30}text");
        assertEquals(2, tokens.size());
        assertEquals(TextTokenType.SET_CPS, tokens.get(0).type());
        assertEquals(30, tokens.get(0).cps());
    }

    @Test
    void fastTagProducesFastForwardToken() {
        List<TextToken> tokens = parser.parse("{fast}text");
        assertEquals(2, tokens.size());
        assertEquals(TextTokenType.FAST_FORWARD, tokens.get(0).type());
    }

    @Test
    void existingWaitWithDurationStillWorks() {
        List<TextToken> tokens = parser.parse("{w=2.5}text");
        assertEquals(2, tokens.size());
        assertEquals(TextTokenType.PAUSE, tokens.get(0).type());
        assertEquals(2.5, tokens.get(0).durationSeconds(), 0.001);
    }

    @Test
    void existingParagraphTagStillWorks() {
        List<TextToken> tokens = parser.parse("a{p}b");
        assertEquals(3, tokens.size());
        assertEquals(TextTokenType.PARAGRAPH, tokens.get(1).type());
    }
}
```

- [ ] **Step 5.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `WAIT_CLICK`, `NO_WAIT`, `SET_CPS`, `FAST_FORWARD`, `cps()` not found.

- [ ] **Step 5.3: Add new values to TextTokenType**

Replace the entire `TextTokenType.java`:

```java
package com.eb.javafx.text;

public enum TextTokenType {
    TEXT,
    ICON,
    PAUSE,
    PARAGRAPH,
    WAIT_CLICK,
    NO_WAIT,
    SET_CPS,
    FAST_FORWARD
}
```

- [ ] **Step 5.4: Add factory methods and cps() to TextToken**

Add to `TextToken.java` after the `paragraph()` factory:

```java
    /** Creates a wait-for-click pacing token. */
    public static TextToken waitClick() {
        return new TextToken(TextTokenType.WAIT_CLICK, "", "", TextStyle.plain(), 0.0);
    }

    /** Creates a no-wait (auto-advance) pacing token. */
    public static TextToken noWait() {
        return new TextToken(TextTokenType.NO_WAIT, "", "", TextStyle.plain(), 0.0);
    }

    /** Creates a set-characters-per-second pacing token. */
    public static TextToken setCps(int cps) {
        return new TextToken(TextTokenType.SET_CPS, "", "", TextStyle.plain(), cps);
    }

    /** Creates a fast-forward (skip typewriter animation) pacing token. */
    public static TextToken fastForward() {
        return new TextToken(TextTokenType.FAST_FORWARD, "", "", TextStyle.plain(), 0.0);
    }

    /** Returns the characters-per-second value for SET_CPS tokens. */
    public int cps() {
        return (int) durationSeconds;
    }
```

- [ ] **Step 5.5: Handle new pacing tags in TextTagParser**

In `TextTagParser.applyTag`, add new cases in the `switch` statement before the `default` block:

```java
            case "w" -> {
                flushText(tokens, text, state);
                tokens.add(TextToken.waitClick());
                return true;
            }
            case "nw" -> {
                flushText(tokens, text, state);
                tokens.add(TextToken.noWait());
                return true;
            }
            case "fast" -> {
                flushText(tokens, text, state);
                tokens.add(TextToken.fastForward());
                return true;
            }
```

In the `default` block, add before the `return false` at the end:

```java
                if (tag.startsWith("cps=")) {
                    flushText(tokens, text, state);
                    tokens.add(TextToken.setCps(parseCps(tag.substring("cps=".length()))));
                    return true;
                }
```

Add `parseCps` method after `parseDuration`:

```java
    private int parseCps(String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value <= 0) throw new IllegalArgumentException("CPS must be positive: " + rawValue);
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid cps tag value: " + rawValue, exception);
        }
    }
```

- [ ] **Step 5.6: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.TextPacingTagsTest"
```
Expected: PASS (6 tests).

- [ ] **Step 5.7: Run text package tests for regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.text.*"
```
Expected: all pass.

- [ ] **Step 5.8: Commit**

```
git add src/main/java/com/eb/javafx/text/TextTokenType.java
git add src/main/java/com/eb/javafx/text/TextToken.java
git add src/main/java/com/eb/javafx/text/TextTagParser.java
git add src/test/java/com/eb/javafx/scene/TextPacingTagsTest.java
git commit -m "feat(text): add WAIT_CLICK, NO_WAIT, SET_CPS, FAST_FORWARD pacing token types"
```

---

## Task 6: Timed choices and menu captions on SceneStep

**Files:**
- Modify: `src/main/java/com/eb/javafx/scene/SceneStep.java`
- Create: `src/test/java/com/eb/javafx/scene/TimedChoiceMenuTest.java`

- [ ] **Step 6.1: Write failing test**

```java
// src/test/java/com/eb/javafx/scene/TimedChoiceMenuTest.java
package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TimedChoiceMenuTest {

    @Test
    void choiceTimeoutDefaultsToNull() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        ));
        assertNull(step.choiceTimeoutMs());
        assertNull(step.choiceTimeoutDefaultId());
    }

    @Test
    void withChoiceTimeoutStoresValues() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        )).withChoiceTimeout(5000L, "opt");

        assertEquals(5000L, step.choiceTimeoutMs());
        assertEquals("opt", step.choiceTimeoutDefaultId());
    }

    @Test
    void withChoiceTimeoutNullDefaultIdIsAllowed() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        )).withChoiceTimeout(3000L, null);

        assertEquals(3000L, step.choiceTimeoutMs());
        assertNull(step.choiceTimeoutDefaultId());
    }

    @Test
    void menuCaptionDefaultsToNull() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        ));
        assertNull(step.menuCaptionTextKey());
    }

    @Test
    void withMenuCaptionStoresTextKey() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        )).withMenuCaption("choice.caption");

        assertEquals("choice.caption", step.menuCaptionTextKey());
    }
}
```

- [ ] **Step 6.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `choiceTimeoutMs()`, `choiceTimeoutDefaultId()`, `menuCaptionTextKey()`, `withChoiceTimeout()`, `withMenuCaption()` not found.

- [ ] **Step 6.3: Add timed choice and caption accessors to SceneStep**

Add to `SceneStep.java` after `withDisplayMode`:

```java
    public Long choiceTimeoutMs() {
        String value = metadata.get("choiceTimeoutMs");
        return value == null ? null : Long.parseLong(value);
    }

    public String choiceTimeoutDefaultId() {
        return metadata.get("choiceTimeoutDefaultId");
    }

    public String menuCaptionTextKey() {
        return metadata.get("menuCaptionTextKey");
    }

    public boolean menuLoop() {
        return Boolean.parseBoolean(metadata.get("menuLoop"));
    }

    public SceneStep withChoiceTimeout(long timeoutMs, String defaultChoiceId) {
        Map<String, String> updated = new LinkedHashMap<>(metadata);
        updated.put("choiceTimeoutMs", Long.toString(timeoutMs));
        if (defaultChoiceId != null) {
            updated.put("choiceTimeoutDefaultId", defaultChoiceId);
        }
        return withMetadata(updated);
    }

    public SceneStep withMenuCaption(String captionTextKey) {
        return withMetadataValue("menuCaptionTextKey",
                Validation.requireNonBlank(captionTextKey, "Menu caption text key is required."));
    }

    public SceneStep withMenuLoop() {
        return withMetadataValue("menuLoop", "true");
    }
```

- [ ] **Step 6.4: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.TimedChoiceMenuTest"
```
Expected: PASS (5 tests).

- [ ] **Step 6.5: Commit**

```
git add src/main/java/com/eb/javafx/scene/SceneStep.java
git add src/test/java/com/eb/javafx/scene/TimedChoiceMenuTest.java
git commit -m "feat(scene): add choiceTimeoutMs, choiceTimeoutDefaultId, menuCaptionTextKey, menuLoop to SceneStep"
```

---

## Task 7: Sticky / loop menus

**Files:**
- Modify: `src/main/java/com/eb/javafx/scene/SceneChoice.java`
- Modify: `src/main/java/com/eb/javafx/scene/SceneExecutor.java`
- Create: `src/test/java/com/eb/javafx/scene/LoopMenuTest.java`

- [ ] **Step 7.1: Write failing test**

```java
// src/test/java/com/eb/javafx/scene/LoopMenuTest.java
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

final class LoopMenuTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots",
            List.of(new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void exitsMenuDefaultsToTrue() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.complete());
        assertTrue(choice.exitsMenu());
    }

    @Test
    void asMenuReturnSetsExitsMenuFalse() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.next()).asMenuReturn();
        assertFalse(choice.exitsMenu());
    }

    @Test
    void loopMenuRepresentsAfterNonExitChoice() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("stay", "text.stay", SceneTransition.next()).asMenuReturn(),
                SceneChoice.of("leave", "text.leave", SceneTransition.complete())
            )).withMenuLoop()
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SceneExecutionResult first = executor.advanceUntilPause(context, executor.start("scene"));
        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, first.status());

        // selecting the non-exit choice should re-present the menu
        SceneExecutionResult afterStay = executor.selectChoice(context, first.state(), "stay");
        assertEquals(SceneExecutionStatus.WAITING_FOR_CHOICE, afterStay.status());
        assertEquals("menu", afterStay.step().id());

        // selecting the exit choice should leave the menu
        SceneExecutionResult afterLeave = executor.selectChoice(context, afterStay.state(), "leave");
        assertEquals(SceneExecutionStatus.COMPLETED, afterLeave.status());
    }

    @Test
    void nonLoopMenuDoesNotRepeat() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.choice("menu", List.of(
                SceneChoice.of("opt", "text.opt", SceneTransition.complete())
            ))
            // no withMenuLoop()
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        ActionContext context = actionContext();

        SceneExecutionResult first = executor.advanceUntilPause(context, executor.start("scene"));
        SceneExecutionResult after = executor.selectChoice(context, first.state(), "opt");
        assertEquals(SceneExecutionStatus.COMPLETED, after.status());
    }
}
```

- [ ] **Step 7.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `exitsMenu()`, `asMenuReturn()` not found on `SceneChoice`.

- [ ] **Step 7.3: Add exitsMenu accessors to SceneChoice**

Add to `SceneChoice.java` after `withCondition`:

```java
    public boolean exitsMenu() {
        String value = metadata.get("exitsMenu");
        return !"false".equals(value);
    }

    public SceneChoice asMenuReturn() {
        return withMetadataValue("exitsMenu", "false");
    }
```

- [ ] **Step 7.4: Add loop menu handling to SceneExecutor.selectChoice**

In `SceneExecutor.selectChoice`, replace the final return statement:

```java
        List<String> selectedChoiceIds = new ArrayList<>(state.selectedChoiceIds());
        selectedChoiceIds.add(choice.id());
        SceneFlowState selectedState = new SceneFlowState(state.activeSceneId(), state.stepIndex(),
                state.callStack(), selectedChoiceIds, state.pendingUiInterruption());

        if (step.menuLoop() && !choice.exitsMenu()) {
            return advanceUntilPause(context, selectedState);
        }

        return advanceUntilPause(context, applyTransition(selectedState, choice.transition()));
```

- [ ] **Step 7.5: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.LoopMenuTest"
```
Expected: PASS (4 tests).

- [ ] **Step 7.6: Run all scene tests for regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.*"
```
Expected: all pass.

- [ ] **Step 7.7: Commit**

```
git add src/main/java/com/eb/javafx/scene/SceneChoice.java
git add src/main/java/com/eb/javafx/scene/SceneExecutor.java
git add src/test/java/com/eb/javafx/scene/LoopMenuTest.java
git commit -m "feat(scene): add sticky/loop menu support via SceneChoice.exitsMenu and SceneStep.menuLoop"
```

---

## Task 8: RollbackSnapshotCodec

**Files:**
- Create: `src/main/java/com/eb/javafx/scene/RollbackSnapshotCodec.java`

The codec persists the scene flow state for each buffer entry. Contributor state is restored from the main save data (same approach as all other save codecs). Non-persistable contributors reset to post-load state — this is intentional and matches standard VN engine behaviour.

- [ ] **Step 8.1: Write failing test**

Add to `RollbackBufferTest.java`:

```java
    @Test
    void rollbackSnapshotCodecRoundTripsFlowStates() {
        RollbackBuffer buffer = new RollbackBuffer(10);
        buffer.snapshot(SceneFlowState.start("scene1"));
        buffer.snapshot(SceneFlowState.start("scene2"));

        RollbackSnapshotCodec codec = new RollbackSnapshotCodec();
        assertEquals("rollback", codec.sectionId());
        assertEquals(1, codec.schemaVersion());

        String json = codec.toJson(buffer);
        RollbackBuffer restored = codec.fromJson(json, "test");

        assertEquals(2, restored.size());
        restored.pop(); // scene2
        RollbackEntry scene1 = restored.pop().orElseThrow();
        assertEquals("scene1", scene1.flowState().activeSceneId());
    }
```

- [ ] **Step 8.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `RollbackSnapshotCodec` not found.

- [ ] **Step 8.3: Create RollbackSnapshotCodec**

```java
// src/main/java/com/eb/javafx/scene/RollbackSnapshotCodec.java
package com.eb.javafx.scene;

import com.eb.javafx.save.SaveSnapshotCodec;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.SimpleJson;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RollbackSnapshotCodec implements SaveSnapshotCodec<RollbackBuffer> {
    public static final String SECTION_ID = "rollback";
    public static final int SCHEMA_VERSION = 1;

    private final int capacity;

    public RollbackSnapshotCodec() {
        this(100);
    }

    public RollbackSnapshotCodec(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public String sectionId() {
        return SECTION_ID;
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public String toJson(RollbackBuffer buffer) {
        Validation.requireNonNull(buffer, "buffer");
        StringBuilder entries = new StringBuilder("[");
        boolean first = true;
        RollbackBuffer temp = new RollbackBuffer(buffer.size() + 1);
        List<RollbackEntry> ordered = new ArrayList<>();
        // drain to list preserving order
        while (buffer.size() > 0) {
            buffer.pop().ifPresent(e -> ordered.add(0, e));
        }
        // re-fill buffer so it is unchanged after toJson
        for (RollbackEntry entry : ordered) {
            buffer.snapshot(entry.flowState());
        }
        for (RollbackEntry entry : ordered) {
            if (!first) entries.append(",");
            entries.append(entryToJson(entry));
            first = false;
        }
        entries.append("]");
        return "{\n  \"entries\": " + entries + "\n}";
    }

    @Override
    public RollbackBuffer fromJson(String json, String sourceName) {
        Object parsed = SimpleJson.parse(json, sourceName);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("Rollback snapshot JSON root must be an object in: " + sourceName);
        }
        Object rawEntries = root.get("entries");
        RollbackBuffer buffer = new RollbackBuffer(capacity);
        if (rawEntries instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> entryMap)) {
                    throw new IllegalArgumentException("Rollback entry must be an object in: " + sourceName);
                }
                buffer.snapshot(flowStateFromMap(entryMap, sourceName));
            }
        }
        return buffer;
    }

    private String entryToJson(RollbackEntry entry) {
        SceneFlowState s = entry.flowState();
        return "{\"sceneId\": " + JsonStrings.quote(s.activeSceneId())
                + ", \"stepIndex\": " + s.stepIndex() + "}";
    }

    private SceneFlowState flowStateFromMap(Map<?, ?> map, String sourceName) {
        Object sceneId = map.get("sceneId");
        Object stepIndex = map.get("stepIndex");
        if (!(sceneId instanceof String sid)) {
            throw new IllegalArgumentException("Rollback entry sceneId must be a string in: " + sourceName);
        }
        if (!(stepIndex instanceof Number idx)) {
            throw new IllegalArgumentException("Rollback entry stepIndex must be a number in: " + sourceName);
        }
        return new SceneFlowState(sid, idx.intValue(), List.of(), List.of(), null);
    }
}
```

- [ ] **Step 8.4: Run test to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.RollbackBufferTest"
```
Expected: all PASS.

- [ ] **Step 8.5: Run full scene test suite**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.*"
```
Expected: all pass.

- [ ] **Step 8.6: Commit**

```
git add src/main/java/com/eb/javafx/scene/RollbackSnapshotCodec.java
git add src/test/java/com/eb/javafx/scene/RollbackBufferTest.java
git commit -m "feat(scene): add RollbackSnapshotCodec for save-file persistence of rollback buffer"
```

---

## Task 9: Final build and change summary

- [ ] **Step 9.1: Run the full build**

```
./gradlew --no-daemon build
```
Expected: BUILD SUCCESSFUL with no test failures.

- [ ] **Step 9.2: Write change summary**

Append to `change_summary.md` in the repository root:

```markdown
---

## Phase 1 — Ren'Py Parity: Foundational Scene Mechanics

Six core scene mechanics added to reach Ren'Py feature parity.

### Dialogue rollback
`RollbackContributor<T>` and `PersistableRollbackContributor<T>` (extends `SaveSnapshotCodec<T>`) define the contribution contract. `RollbackBuffer` is a fixed-capacity ring buffer that snapshots all registered contributors before each DIALOGUE, NARRATION, and CHOICE pause. `SceneExecutor` gains an optional `RollbackBuffer` constructor parameter and a `rollback(ActionContext)` method that pops two entries, restores the previous state, and re-advances. `SceneExecutionResult` gains `canRollback()`. `RollbackSnapshotCodec` persists the buffer's flow states as a save section.

### Conditional choice visibility
`ConditionPolicy` enum (HIDE, GREY). `SceneChoice.withCondition(expression, policy)` stores a `SceneConditionExpression` string and policy in metadata. `SceneExecutor` evaluates conditions at CHOICE step time: HIDE excludes the choice from the result, GREY includes it as disabled.

### Text pacing control tags
`TextTokenType` gains WAIT_CLICK, NO_WAIT, SET_CPS, FAST_FORWARD. `TextToken` gains factory methods and `cps()` accessor. `TextTagParser` handles `{w}` (wait-click), `{nw}` (no-wait), `{cps=N}` (set chars/sec), `{fast}` (fast-forward).

### Timed choices
`SceneStep` gains `choiceTimeoutMs()` (Long, null = no timeout), `choiceTimeoutDefaultId()` (String, null = first option), and `withChoiceTimeout(long, String)` builder method. Adapter drives the timer; calls existing `selectChoice()` on expiry.

### Menu captions
`SceneStep` gains `menuCaptionTextKey()` (String, null = no caption) and `withMenuCaption(String)` builder method.

### Sticky/loop menus
`SceneChoice` gains `exitsMenu()` (default true) and `asMenuReturn()` builder. `SceneStep` gains `menuLoop()` and `withMenuLoop()`. When `menuLoop` is true and a selected choice has `exitsMenu = false`, `SceneExecutor.selectChoice` re-presents the same CHOICE step after applying effects.

**Files changed:**
- `src/main/java/com/eb/javafx/scene/RollbackContributor.java` (new)
- `src/main/java/com/eb/javafx/scene/PersistableRollbackContributor.java` (new)
- `src/main/java/com/eb/javafx/scene/RollbackEntry.java` (new)
- `src/main/java/com/eb/javafx/scene/RollbackBuffer.java` (new)
- `src/main/java/com/eb/javafx/scene/RollbackSnapshotCodec.java` (new)
- `src/main/java/com/eb/javafx/scene/ConditionPolicy.java` (new)
- `src/main/java/com/eb/javafx/scene/SceneExecutor.java`
- `src/main/java/com/eb/javafx/scene/SceneExecutionResult.java`
- `src/main/java/com/eb/javafx/scene/SceneChoice.java`
- `src/main/java/com/eb/javafx/scene/SceneStep.java`
- `src/main/java/com/eb/javafx/text/TextTokenType.java`
- `src/main/java/com/eb/javafx/text/TextToken.java`
- `src/main/java/com/eb/javafx/text/TextTagParser.java`
```

- [ ] **Step 9.3: Rename change summary per branch naming convention**

```
# Branch: claude/relaxed-edison-60b697 → change_summary_claude_relaxed_edison_60b697.md
mv change_summary.md change_summary_claude_relaxed_edison_60b697.md
git add change_summary_claude_relaxed_edison_60b697.md
git commit -m "docs: add Phase 1 Ren'Py parity change summary"
```
