# Ren'Py Parity Phase 3 — Audio Completeness

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the four audio completeness features from Phase 3: voice channel with auto-advance, scene transition sounds, music queue, and audio channel priority/ducking config.

**Architecture:** Three packages are touched: `com.eb.javafx.audio` (voice channel constant, queue, ducking config), `com.eb.javafx.prefs` (three new voice preferences), `com.eb.javafx.scene` (voiceRef on SceneStep, voiceRequest on SceneExecutionResult, wiring in SceneExecutor), and `com.eb.javafx.transitions` (soundRef on VisualTransitionDefinition/Request). All new types are value objects or small service additions. No new step types. No module-info changes needed.

**Tech Stack:** Java 17, Gradle (`./gradlew --no-daemon`), JUnit 5. All tests in `src/test/java/com/eb/javafx/...` matching the production package.

---

> **Note on scope:** This is Phase 3 of 4. Phases 1–2 (scene mechanics, display completeness) are already complete. Phase 4 (UI and meta polish) will be a separate plan.

---

## File map

**New files:**
- `src/main/java/com/eb/javafx/audio/DuckingPolicy.java`
- `src/main/java/com/eb/javafx/audio/AudioChannelConfig.java`
- `src/test/java/com/eb/javafx/audio/VoiceChannelPrefsTest.java`
- `src/test/java/com/eb/javafx/scene/VoiceRequestSceneTest.java`
- `src/test/java/com/eb/javafx/audio/AudioQueueTest.java`
- `src/test/java/com/eb/javafx/audio/AudioChannelDuckingTest.java`
- `src/test/java/com/eb/javafx/transitions/VisualTransitionSoundTest.java`

**Modified files:**
- `src/main/java/com/eb/javafx/prefs/PreferencesService.java` — voiceEnabled, voiceVolume, autoAdvanceOnVoiceEnd preferences
- `src/main/java/com/eb/javafx/audio/AudioService.java` — VOICE_CHANNEL constant, VOICE channel registration, queueMusic, clearQueue, queuedRequest, registerChannel(AudioChannelConfig), channelConfig
- `src/main/java/com/eb/javafx/audio/SoundRequest.java` — queued() factory
- `src/main/java/com/eb/javafx/scene/SceneStep.java` — voiceRef(), withVoiceRef()
- `src/main/java/com/eb/javafx/scene/SceneExecutionResult.java` — voiceRequest field, withVoiceRequest(), voiceRequest() accessor
- `src/main/java/com/eb/javafx/scene/SceneExecutor.java` — populate voiceRequest in DIALOGUE/NARRATION cases
- `src/main/java/com/eb/javafx/transitions/VisualTransitionDefinition.java` — soundRef field, withSoundRef()
- `src/main/java/com/eb/javafx/transitions/VisualTransitionRequest.java` — soundRef() delegation
- `src/test/java/com/eb/javafx/audio/AudioServiceTest.java` — update channel count assertion from 4 to 5

---

## Task 1: Voice preferences + VOICE_CHANNEL constant

**Files:**
- Modify: `src/main/java/com/eb/javafx/prefs/PreferencesService.java`
- Modify: `src/main/java/com/eb/javafx/audio/AudioService.java`
- Modify: `src/test/java/com/eb/javafx/audio/AudioServiceTest.java`
- Create: `src/test/java/com/eb/javafx/audio/VoiceChannelPrefsTest.java`

- [ ] **Step 1.1: Write failing tests**

```java
// src/test/java/com/eb/javafx/audio/VoiceChannelPrefsTest.java
package com.eb.javafx.audio;

import com.eb.javafx.prefs.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

final class VoiceChannelPrefsTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    @Test
    void voiceEnabledDefaultsToTrue() {
        PreferencesService prefs = new PreferencesService();
        prefs.load();
        assertTrue(prefs.voiceEnabled());
    }

    @Test
    void voiceVolumeDefaultsToOne() {
        PreferencesService prefs = new PreferencesService();
        prefs.load();
        assertEquals(1.0, prefs.voiceVolume(), 0.0001);
    }

    @Test
    void autoAdvanceOnVoiceEndDefaultsToFalse() {
        PreferencesService prefs = new PreferencesService();
        prefs.load();
        assertFalse(prefs.autoAdvanceOnVoiceEnd());
    }

    @Test
    void saveVoicePreferencesPersistAndReload() {
        PreferencesService prefs = new PreferencesService();
        prefs.saveVoiceVolume(0.6);
        prefs.saveVoiceEnabled(false);
        prefs.saveAutoAdvanceOnVoiceEnd(true);

        PreferencesService reloaded = new PreferencesService();
        reloaded.load();

        assertEquals(0.6, reloaded.voiceVolume(), 0.0001);
        assertFalse(reloaded.voiceEnabled());
        assertTrue(reloaded.autoAdvanceOnVoiceEnd());
    }

    @Test
    void initializeRegistersVoiceChannelWithVoiceVolume() {
        PreferencesService prefs = new PreferencesService();
        prefs.saveVoiceVolume(0.7);
        prefs.load();

        AudioService service = new AudioService();
        service.initialize(prefs);

        assertTrue(service.channels().containsKey(AudioService.VOICE_CHANNEL));
        assertEquals(0.7, service.channelVolume(AudioService.VOICE_CHANNEL), 0.0001);
    }
}
```

- [ ] **Step 1.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `voiceEnabled()`, `voiceVolume()`, `autoAdvanceOnVoiceEnd()`, `VOICE_CHANNEL` not found.

- [ ] **Step 1.3: Add VOICE_CHANNEL constant to AudioService**

Add after `INTIMATE_EFFECTS_CHANNEL`:

```java
    public static final String VOICE_CHANNEL = "voice";
```

- [ ] **Step 1.4: Add voice preference fields and keys to PreferencesService**

Add after the `MUTE_ALL_KEY` constant:

```java
    private static final String VOICE_VOLUME_KEY = "audio.voiceVolume";
    private static final String VOICE_ENABLED_KEY = "audio.voiceEnabled";
    private static final String AUTO_ADVANCE_ON_VOICE_END_KEY = "audio.autoAdvanceOnVoiceEnd";
```

Add after the `muteAll` field:

```java
    private double voiceVolume;
    private boolean voiceEnabled;
    private boolean autoAdvanceOnVoiceEnd;
```

Add at the end of `load()` before the closing brace:

```java
        voiceVolume = clamp(preferences.getDouble(VOICE_VOLUME_KEY, 1.0), 0.0, 1.0);
        voiceEnabled = preferences.getBoolean(VOICE_ENABLED_KEY, true);
        autoAdvanceOnVoiceEnd = preferences.getBoolean(AUTO_ADVANCE_ON_VOICE_END_KEY, false);
```

Add accessors after `muteAll()`:

```java
    /** Returns the persisted voice channel volume multiplier. */
    public double voiceVolume() {
        return voiceVolume;
    }

    /** Returns whether the voice channel is enabled. */
    public boolean voiceEnabled() {
        return voiceEnabled;
    }

    /** Returns whether advancing to the next step automatically when voice playback ends. */
    public boolean autoAdvanceOnVoiceEnd() {
        return autoAdvanceOnVoiceEnd;
    }
```

Add save methods after `saveMuteAll`:

```java
    /** Persists a clamped voice channel volume. */
    public void saveVoiceVolume(double voiceVolume) {
        this.voiceVolume = clamp(voiceVolume, 0.0, 1.0);
        preferences.putDouble(VOICE_VOLUME_KEY, this.voiceVolume);
    }

    /** Persists whether the voice channel is enabled. */
    public void saveVoiceEnabled(boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
        preferences.putBoolean(VOICE_ENABLED_KEY, voiceEnabled);
    }

    /** Persists the auto-advance-on-voice-end preference. */
    public void saveAutoAdvanceOnVoiceEnd(boolean autoAdvanceOnVoiceEnd) {
        this.autoAdvanceOnVoiceEnd = autoAdvanceOnVoiceEnd;
        preferences.putBoolean(AUTO_ADVANCE_ON_VOICE_END_KEY, autoAdvanceOnVoiceEnd);
    }
```

- [ ] **Step 1.5: Register VOICE channel in AudioService.initialize()**

In `AudioService.initialize()`, add after the `INTIMATE_EFFECTS_CHANNEL` registration:

```java
        registerChannel(new AudioChannelDefinition(VOICE_CHANNEL, "Character voice lines.", false, 1, 1.0));
        channelVolumes.put(VOICE_CHANNEL, preferencesService.voiceVolume());
```

- [ ] **Step 1.6: Update existing AudioServiceTest channel count**

In `AudioServiceTest.initializeRegistersDefaultChannelsAndMasterVolume`, change:

```java
        assertEquals(4, service.channels().size());
```

to:

```java
        assertEquals(5, service.channels().size());
```

- [ ] **Step 1.7: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.audio.VoiceChannelPrefsTest" --tests "com.eb.javafx.audio.AudioServiceTest"
```
Expected: all PASS.

- [ ] **Step 1.8: Commit**

```
git add src/main/java/com/eb/javafx/prefs/PreferencesService.java
git add src/main/java/com/eb/javafx/audio/AudioService.java
git add src/test/java/com/eb/javafx/audio/AudioServiceTest.java
git add src/test/java/com/eb/javafx/audio/VoiceChannelPrefsTest.java
git commit -m "feat(audio): add VOICE_CHANNEL constant and voiceEnabled/voiceVolume/autoAdvanceOnVoiceEnd preferences"
```

---

## Task 2: voiceRef on SceneStep + voiceRequest wiring

**Files:**
- Modify: `src/main/java/com/eb/javafx/scene/SceneStep.java`
- Modify: `src/main/java/com/eb/javafx/scene/SceneExecutionResult.java`
- Modify: `src/main/java/com/eb/javafx/scene/SceneExecutor.java`
- Create: `src/test/java/com/eb/javafx/scene/VoiceRequestSceneTest.java`

- [ ] **Step 2.1: Write failing tests**

```java
// src/test/java/com/eb/javafx/scene/VoiceRequestSceneTest.java
package com.eb.javafx.scene;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.audio.SoundRequest;
import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class VoiceRequestSceneTest {

    private ActionContext actionContext() {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        CodeTableDefinition timeSlots = new CodeTableDefinition("time-slots", "Time Slots",
            List.of(new CodeDefinition("first", "First", 10, List.of())));
        return new ActionContext(new GameState("main-menu"), randomService, new GameClock(timeSlots));
    }

    @Test
    void voiceRefDefaultsToNull() {
        SceneStep step = SceneStep.narration("n1", "text.n1");
        assertNull(step.voiceRef());
    }

    @Test
    void withVoiceRefStoresRef() {
        SceneStep step = SceneStep.narration("n1", "text.n1").withVoiceRef("audio/vo/n1.ogg");
        assertEquals("audio/vo/n1.ogg", step.voiceRef());
    }

    @Test
    void executorPopulatesVoiceRequestWhenStepHasVoiceRef() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("n1", "text.n1").withVoiceRef("audio/vo/n1.ogg")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertTrue(result.voiceRequest().isPresent());
        SoundRequest req = result.voiceRequest().get();
        assertEquals(AudioService.VOICE_CHANNEL, req.channelId());
        assertEquals("audio/vo/n1.ogg", req.sourcePath());
        assertFalse(req.loop());
    }

    @Test
    void executorEmitsNoVoiceRequestWhenStepHasNoVoiceRef() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("n1", "text.n1")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertFalse(result.voiceRequest().isPresent());
    }

    @Test
    void withVoiceRequestProducesImmutableCopyLeavingOriginalEmpty() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.narration("n1", "text.n1")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        SceneExecutionResult base = executor.advanceUntilPause(actionContext(), executor.start("scene"));
        SoundRequest voiceReq = new SoundRequest(AudioService.VOICE_CHANNEL, "audio/vo/n1.ogg", false, 1.0);
        SceneExecutionResult withVoice = base.withVoiceRequest(voiceReq);

        assertFalse(base.voiceRequest().isPresent());
        assertTrue(withVoice.voiceRequest().isPresent());
        assertEquals("audio/vo/n1.ogg", withVoice.voiceRequest().get().sourcePath());
    }

    @Test
    void dialogueStepAlsoPopulatesVoiceRequest() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("scene", List.of(
            SceneStep.dialogue("d1", "narrator", "text.d1", null).withVoiceRef("audio/vo/d1.ogg")
        )));
        registry.validateScenes();

        SceneExecutor executor = new SceneExecutor(registry);
        SceneExecutionResult result = executor.advanceUntilPause(actionContext(), executor.start("scene"));

        assertTrue(result.voiceRequest().isPresent());
        assertEquals("audio/vo/d1.ogg", result.voiceRequest().get().sourcePath());
    }
}
```

- [ ] **Step 2.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `voiceRef()`, `withVoiceRef()`, `voiceRequest()`, `withVoiceRequest()` not found.

- [ ] **Step 2.3: Add voiceRef accessors to SceneStep**

Add after `withMenuLoop()` in `SceneStep.java`:

```java
    public String voiceRef() {
        return metadata.get("voiceRef");
    }

    public SceneStep withVoiceRef(String voiceRef) {
        return withMetadataValue("voiceRef",
                Validation.requireNonBlank(voiceRef, "Voice ref is required."));
    }
```

- [ ] **Step 2.4: Add voiceRequest field and accessors to SceneExecutionResult**

Add import at the top of `SceneExecutionResult.java`:

```java
import com.eb.javafx.audio.SoundRequest;
import java.util.Optional;
```

Add field after `canRollback`:

```java
    private final SoundRequest voiceRequest;
```

Replace the two existing constructors with:

```java
    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message, boolean canRollback) {
        this(status, state, step, availableChoices, message, canRollback, null);
    }

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message) {
        this(status, state, step, availableChoices, message, false, null);
    }

    private SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message, boolean canRollback, SoundRequest voiceRequest) {
        this.status = status;
        this.state = state;
        this.step = step;
        this.availableChoices = List.copyOf(availableChoices);
        this.message = message;
        this.canRollback = canRollback;
        this.voiceRequest = voiceRequest;
    }
```

Add after `canRollback()`:

```java
    public Optional<SoundRequest> voiceRequest() {
        return Optional.ofNullable(voiceRequest);
    }

    public SceneExecutionResult withVoiceRequest(SoundRequest voiceRequest) {
        return new SceneExecutionResult(status, state, step, availableChoices, message, canRollback, voiceRequest);
    }
```

- [ ] **Step 2.5: Wire voiceRequest in SceneExecutor**

Add import at the top of `SceneExecutor.java`:

```java
import com.eb.javafx.audio.AudioService;
import com.eb.javafx.audio.SoundRequest;
```

Replace the `DIALOGUE, NARRATION` case in `advanceUntilPause`:

```java
                case DIALOGUE, NARRATION -> {
                    if (rollbackBuffer != null) rollbackBuffer.snapshot(current);
                    SceneExecutionResult result = new SceneExecutionResult(
                            SceneExecutionStatus.DISPLAYING_TEXT, current, step, List.of(), null,
                            rollbackBuffer != null && rollbackBuffer.canRollback());
                    SoundRequest voice = buildVoiceRequest(step);
                    return voice != null ? result.withVoiceRequest(voice) : result;
                }
```

Add private helper method after `resolveChoices`:

```java
    private SoundRequest buildVoiceRequest(SceneStep step) {
        String ref = step.voiceRef();
        return ref != null ? new SoundRequest(AudioService.VOICE_CHANNEL, ref, false, 1.0) : null;
    }
```

- [ ] **Step 2.6: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.VoiceRequestSceneTest"
```
Expected: PASS (6 tests).

- [ ] **Step 2.7: Run broader scene tests for regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.scene.*"
```
Expected: all pass.

- [ ] **Step 2.8: Commit**

```
git add src/main/java/com/eb/javafx/scene/SceneStep.java
git add src/main/java/com/eb/javafx/scene/SceneExecutionResult.java
git add src/main/java/com/eb/javafx/scene/SceneExecutor.java
git add src/test/java/com/eb/javafx/scene/VoiceRequestSceneTest.java
git commit -m "feat(scene): add voiceRef to SceneStep and voiceRequest to SceneExecutionResult; wire SceneExecutor"
```

---

## Task 3: Scene transition sounds

**Files:**
- Modify: `src/main/java/com/eb/javafx/transitions/VisualTransitionDefinition.java`
- Modify: `src/main/java/com/eb/javafx/transitions/VisualTransitionRequest.java`
- Create: `src/test/java/com/eb/javafx/transitions/VisualTransitionSoundTest.java`

- [ ] **Step 3.1: Write failing tests**

```java
// src/test/java/com/eb/javafx/transitions/VisualTransitionSoundTest.java
package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class VisualTransitionSoundTest {

    @Test
    void soundRefDefaultsToNull() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("fade", SceneTransitionEffect.DISSOLVE, 500);
        assertNull(def.soundRef());
    }

    @Test
    void withSoundRefStoresSoundRef() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("wipe", SceneTransitionEffect.WIPE_LEFT, 300)
                .withSoundRef("audio/sfx/wipe.ogg");
        assertEquals("audio/sfx/wipe.ogg", def.soundRef());
    }

    @Test
    void withSoundRefPreservesOtherFields() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("wipe", SceneTransitionEffect.WIPE_LEFT, 300)
                .withSoundRef("audio/sfx/wipe.ogg");
        assertEquals("wipe", def.id());
        assertEquals(SceneTransitionEffect.WIPE_LEFT, def.effect());
        assertEquals(300, def.durationMs());
    }

    @Test
    void withSoundRefRequiresNonBlankRef() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("fade", SceneTransitionEffect.DISSOLVE, 500);
        assertThrows(IllegalArgumentException.class, () -> def.withSoundRef(""));
        assertThrows(IllegalArgumentException.class, () -> def.withSoundRef(null));
    }

    @Test
    void requestDelegatesToDefinitionSoundRef() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("fade", SceneTransitionEffect.DISSOLVE, 500)
                .withSoundRef("audio/sfx/dissolve.ogg");
        VisualTransitionRequest request = new VisualTransitionRequest(def);
        assertEquals("audio/sfx/dissolve.ogg", request.soundRef());
    }

    @Test
    void requestSoundRefIsNullWhenDefinitionHasNone() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("fade", SceneTransitionEffect.DISSOLVE, 500);
        VisualTransitionRequest request = new VisualTransitionRequest(def);
        assertNull(request.soundRef());
    }
}
```

- [ ] **Step 3.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `soundRef()` and `withSoundRef()` not found on `VisualTransitionDefinition`; `soundRef()` not found on `VisualTransitionRequest`.

- [ ] **Step 3.3: Add soundRef field and builder to VisualTransitionDefinition**

Replace the entire content of `VisualTransitionDefinition.java`:

```java
package com.eb.javafx.transitions;

import com.eb.javafx.util.Validation;

/** Immutable named visual transition effect definition. */
public final class VisualTransitionDefinition {
    private final String id;
    private final SceneTransitionEffect effect;
    private final int durationMs;
    private final String soundRef;

    public VisualTransitionDefinition(String id, SceneTransitionEffect effect, int durationMs) {
        this.id = Validation.requireNonBlank(id, "Visual transition id is required.");
        this.effect = Validation.requireNonNull(effect, "Visual transition effect is required.");
        if (durationMs < 0) {
            throw new IllegalArgumentException("Visual transition durationMs must not be negative.");
        }
        this.durationMs = durationMs;
        this.soundRef = null;
    }

    private VisualTransitionDefinition(String id, SceneTransitionEffect effect, int durationMs, String soundRef) {
        this.id = id;
        this.effect = effect;
        this.durationMs = durationMs;
        this.soundRef = soundRef;
    }

    public String id() { return id; }
    public SceneTransitionEffect effect() { return effect; }
    public int durationMs() { return durationMs; }

    /** Returns the audio asset ref to play during this transition, or null if none. */
    public String soundRef() { return soundRef; }

    /** Returns a copy of this definition with the given audio asset ref attached. */
    public VisualTransitionDefinition withSoundRef(String soundRef) {
        return new VisualTransitionDefinition(id, effect, durationMs,
                Validation.requireNonBlank(soundRef, "Transition sound ref is required."));
    }
}
```

- [ ] **Step 3.4: Add soundRef delegation to VisualTransitionRequest**

Replace the content of `VisualTransitionRequest.java`:

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

    /** Returns the audio asset ref to play during this transition, or null if none. */
    public String soundRef() { return definition.soundRef(); }
}
```

- [ ] **Step 3.5: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.transitions.VisualTransitionSoundTest"
```
Expected: PASS (6 tests).

- [ ] **Step 3.6: Run transitions tests for regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.transitions.*"
```
Expected: all pass.

- [ ] **Step 3.7: Commit**

```
git add src/main/java/com/eb/javafx/transitions/VisualTransitionDefinition.java
git add src/main/java/com/eb/javafx/transitions/VisualTransitionRequest.java
git add src/test/java/com/eb/javafx/transitions/VisualTransitionSoundTest.java
git commit -m "feat(transitions): add soundRef to VisualTransitionDefinition and VisualTransitionRequest"
```

---

## Task 4: Music queue

**Files:**
- Modify: `src/main/java/com/eb/javafx/audio/SoundRequest.java`
- Modify: `src/main/java/com/eb/javafx/audio/AudioService.java`
- Create: `src/test/java/com/eb/javafx/audio/AudioQueueTest.java`

- [ ] **Step 4.1: Write failing tests**

```java
// src/test/java/com/eb/javafx/audio/AudioQueueTest.java
package com.eb.javafx.audio;

import com.eb.javafx.prefs.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

final class AudioQueueTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    private AudioService initializedService() {
        PreferencesService prefs = new PreferencesService();
        prefs.load();
        AudioService service = new AudioService();
        service.initialize(prefs);
        return service;
    }

    @Test
    void queuedFactoryBuildsRequestForChannel() {
        SoundRequest request = SoundRequest.queued("music/theme2.ogg", AudioService.MUSIC_CHANNEL);
        assertEquals(AudioService.MUSIC_CHANNEL, request.channelId());
        assertEquals("music/theme2.ogg", request.sourcePath());
        assertFalse(request.loop());
        assertEquals(1.0, request.relativeVolume(), 0.0001);
    }

    @Test
    void queueMusicStoresPendingRequest() {
        AudioService service = initializedService();
        SoundRequest queued = SoundRequest.queued("music/track2.ogg", AudioService.MUSIC_CHANNEL);
        service.queueMusic(queued);

        assertTrue(service.queuedRequest(AudioService.MUSIC_CHANNEL).isPresent());
        assertEquals("music/track2.ogg", service.queuedRequest(AudioService.MUSIC_CHANNEL).get().sourcePath());
    }

    @Test
    void queueMusicReplacesExistingQueueEntry() {
        AudioService service = initializedService();
        service.queueMusic(SoundRequest.queued("music/track2.ogg", AudioService.MUSIC_CHANNEL));
        service.queueMusic(SoundRequest.queued("music/track3.ogg", AudioService.MUSIC_CHANNEL));

        assertEquals("music/track3.ogg", service.queuedRequest(AudioService.MUSIC_CHANNEL).get().sourcePath());
    }

    @Test
    void clearQueueRemovesPendingEntry() {
        AudioService service = initializedService();
        service.queueMusic(SoundRequest.queued("music/track2.ogg", AudioService.MUSIC_CHANNEL));
        service.clearQueue(AudioService.MUSIC_CHANNEL);

        assertFalse(service.queuedRequest(AudioService.MUSIC_CHANNEL).isPresent());
    }

    @Test
    void stopChannelAlsoClearsQueue() {
        AudioService service = initializedService();
        service.play(new SoundRequest(AudioService.MUSIC_CHANNEL, "music/track1.ogg", true, 1.0));
        service.queueMusic(SoundRequest.queued("music/track2.ogg", AudioService.MUSIC_CHANNEL));

        service.stopChannel(AudioService.MUSIC_CHANNEL);

        assertFalse(service.queuedRequest(AudioService.MUSIC_CHANNEL).isPresent());
    }

    @Test
    void queuedRequestEmptyByDefault() {
        AudioService service = initializedService();
        assertFalse(service.queuedRequest(AudioService.MUSIC_CHANNEL).isPresent());
    }
}
```

- [ ] **Step 4.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `SoundRequest.queued()`, `queueMusic()`, `clearQueue()`, `queuedRequest()` not found.

- [ ] **Step 4.3: Add queued() factory to SoundRequest**

Add after the constructor in `SoundRequest.java`:

```java
    /**
     * Creates a queued sound request at full relative volume and no loop for the given channel.
     *
     * @param audioRef non-blank authored asset path
     * @param channelId registered audio channel ID
     */
    public static SoundRequest queued(String audioRef, String channelId) {
        return new SoundRequest(channelId, audioRef, false, 1.0);
    }
```

- [ ] **Step 4.4: Add queue fields and methods to AudioService**

Add field after `lastPlaybackCommands`:

```java
    private final Map<String, SoundRequest> queuedRequests = new LinkedHashMap<>();
```

Add `queuedRequests.clear()` in `initialize()` after `lastPlaybackCommands.clear()`:

```java
        queuedRequests.clear();
```

Add methods after `stopChannel`:

```java
    /**
     * Schedules a track to play after the current playback on the same channel ends naturally.
     * Calling again replaces the pending entry — single-depth queue matching Ren'Py semantics.
     *
     * @param request the sound request to queue; its channel must already be registered
     */
    public void queueMusic(SoundRequest request) {
        assertInitialized();
        requireChannel(request.channelId());
        queuedRequests.put(request.channelId(), request);
    }

    /** Cancels any pending queued request for the given channel. */
    public void clearQueue(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        queuedRequests.remove(channelId);
    }

    /** Returns the pending queued request for a channel, if one has been scheduled. */
    public Optional<SoundRequest> queuedRequest(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        return Optional.ofNullable(queuedRequests.get(channelId));
    }
```

Update `stopChannel` to clear the queue on stop:

```java
    /** Clears the last command for a channel and cancels any pending queued request. */
    public void stopChannel(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        lastPlaybackCommands.remove(channelId);
        queuedRequests.remove(channelId);
    }
```

- [ ] **Step 4.5: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.audio.AudioQueueTest"
```
Expected: PASS (6 tests).

- [ ] **Step 4.6: Run audio tests for regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.audio.*"
```
Expected: all pass.

- [ ] **Step 4.7: Commit**

```
git add src/main/java/com/eb/javafx/audio/SoundRequest.java
git add src/main/java/com/eb/javafx/audio/AudioService.java
git add src/test/java/com/eb/javafx/audio/AudioQueueTest.java
git commit -m "feat(audio): add music queue — SoundRequest.queued(), AudioService.queueMusic/clearQueue/queuedRequest"
```

---

## Task 5: Audio channel priorities and ducking config

**Files:**
- Create: `src/main/java/com/eb/javafx/audio/DuckingPolicy.java`
- Create: `src/main/java/com/eb/javafx/audio/AudioChannelConfig.java`
- Modify: `src/main/java/com/eb/javafx/audio/AudioService.java`
- Create: `src/test/java/com/eb/javafx/audio/AudioChannelDuckingTest.java`

- [ ] **Step 5.1: Write failing tests**

```java
// src/test/java/com/eb/javafx/audio/AudioChannelDuckingTest.java
package com.eb.javafx.audio;

import com.eb.javafx.prefs.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

final class AudioChannelDuckingTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    private AudioService initializedService() {
        PreferencesService prefs = new PreferencesService();
        prefs.load();
        AudioService service = new AudioService();
        service.initialize(prefs);
        return service;
    }

    @Test
    void audioChannelConfigStoresAllFields() {
        AudioChannelConfig config = new AudioChannelConfig(
                AudioService.VOICE_CHANNEL, 10, 1.0, DuckingPolicy.REDUCE_TO_PERCENT, 0.3);
        assertEquals(AudioService.VOICE_CHANNEL, config.channelId());
        assertEquals(10, config.priority());
        assertEquals(1.0, config.defaultVolume(), 0.0001);
        assertEquals(DuckingPolicy.REDUCE_TO_PERCENT, config.duckingPolicy());
        assertEquals(0.3, config.duckingPercent(), 0.0001);
    }

    @Test
    void duckingPolicyNoneIgnoresDuckingPercent() {
        AudioChannelConfig config = new AudioChannelConfig(
                AudioService.MUSIC_CHANNEL, 5, 1.0, DuckingPolicy.NONE, 0.99);
        assertEquals(DuckingPolicy.NONE, config.duckingPolicy());
        assertEquals(0.0, config.duckingPercent(), 0.0001);
    }

    @Test
    void audioChannelConfigRequiresNonBlankChannelId() {
        assertThrows(IllegalArgumentException.class, () ->
                new AudioChannelConfig("", 5, 1.0, DuckingPolicy.NONE, 0.0));
    }

    @Test
    void audioChannelConfigRequiresVolumeInUnitInterval() {
        assertThrows(IllegalArgumentException.class, () ->
                new AudioChannelConfig(AudioService.MUSIC_CHANNEL, 5, 1.5, DuckingPolicy.NONE, 0.0));
    }

    @Test
    void registerChannelConfigStoresAndRetrievesConfig() {
        AudioService service = initializedService();
        AudioChannelConfig config = new AudioChannelConfig(
                AudioService.VOICE_CHANNEL, 10, 1.0, DuckingPolicy.PAUSE, 0.0);
        service.registerChannel(config);

        AudioChannelConfig retrieved = service.channelConfig(AudioService.VOICE_CHANNEL);
        assertEquals(DuckingPolicy.PAUSE, retrieved.duckingPolicy());
        assertEquals(10, retrieved.priority());
    }

    @Test
    void unregisteredChannelReturnsDefaultConfig() {
        AudioService service = initializedService();
        AudioChannelConfig config = service.channelConfig(AudioService.MUSIC_CHANNEL);

        assertEquals(AudioService.MUSIC_CHANNEL, config.channelId());
        assertEquals(0, config.priority());
        assertEquals(DuckingPolicy.NONE, config.duckingPolicy());
        assertEquals(0.0, config.duckingPercent(), 0.0001);
    }

    @Test
    void registerChannelConfigDoesNotRequirePreExistingAudioChannelDefinition() {
        AudioService service = initializedService();
        service.registerChannel(new AudioChannelDefinition("custom", "Custom channel.", false, 1, 1.0));
        service.registerChannel(new AudioChannelConfig("custom", 3, 0.8, DuckingPolicy.REDUCE_TO_PERCENT, 0.5));

        assertEquals(DuckingPolicy.REDUCE_TO_PERCENT, service.channelConfig("custom").duckingPolicy());
    }
}
```

- [ ] **Step 5.2: Run to verify compilation failure**

```
./gradlew --no-daemon testClasses
```
Expected: `DuckingPolicy`, `AudioChannelConfig`, `registerChannel(AudioChannelConfig)`, `channelConfig()` not found.

- [ ] **Step 5.3: Create DuckingPolicy enum**

```java
// src/main/java/com/eb/javafx/audio/DuckingPolicy.java
package com.eb.javafx.audio;

/** Policy applied to lower-priority channels when a higher-priority channel starts playing. */
public enum DuckingPolicy {
    NONE,
    REDUCE_TO_PERCENT,
    PAUSE
}
```

- [ ] **Step 5.4: Create AudioChannelConfig**

```java
// src/main/java/com/eb/javafx/audio/AudioChannelConfig.java
package com.eb.javafx.audio;

import com.eb.javafx.util.Validation;

/**
 * Priority and ducking configuration for a named audio channel.
 *
 * <p>Channels with higher priority reduce or pause lower-priority channels when they start.
 * Channels not explicitly registered use a default config with priority 0 and {@link DuckingPolicy#NONE}.
 * The engine stores configs; adapters apply the volume changes at play time.</p>
 */
public final class AudioChannelConfig {
    private final String channelId;
    private final int priority;
    private final double defaultVolume;
    private final DuckingPolicy duckingPolicy;
    private final double duckingPercent;

    /**
     * Creates a channel priority and ducking configuration.
     *
     * @param channelId the channel this config applies to
     * @param priority higher values indicate more important channels
     * @param defaultVolume startup volume multiplier from 0.0 to 1.0
     * @param duckingPolicy how lower-priority channels are affected when this one plays
     * @param duckingPercent target volume for REDUCE_TO_PERCENT policy; ignored for other policies
     */
    public AudioChannelConfig(String channelId, int priority, double defaultVolume,
                              DuckingPolicy duckingPolicy, double duckingPercent) {
        this.channelId = Validation.requireNonBlank(channelId, "Channel id is required.");
        this.priority = priority;
        this.defaultVolume = Validation.requireUnitInterval(defaultVolume,
                "Channel config default volume must be between 0 and 1.");
        this.duckingPolicy = Validation.requireNonNull(duckingPolicy, "Ducking policy is required.");
        this.duckingPercent = duckingPolicy == DuckingPolicy.REDUCE_TO_PERCENT
                ? Validation.requireUnitInterval(duckingPercent,
                        "Ducking percent must be between 0 and 1.")
                : 0.0;
    }

    public String channelId() { return channelId; }
    public int priority() { return priority; }
    public double defaultVolume() { return defaultVolume; }
    public DuckingPolicy duckingPolicy() { return duckingPolicy; }
    public double duckingPercent() { return duckingPercent; }
}
```

- [ ] **Step 5.5: Add registerChannel(AudioChannelConfig) and channelConfig() to AudioService**

Add field after `queuedRequests`:

```java
    private final Map<String, AudioChannelConfig> channelConfigs = new LinkedHashMap<>();
```

Add `channelConfigs.clear()` in `initialize()` after `queuedRequests.clear()`:

```java
        channelConfigs.clear();
```

Add overloaded method after `registerChannel(AudioChannelDefinition)`:

```java
    /**
     * Registers priority and ducking configuration for a named channel.
     * Replaces any previously registered config for that channel ID.
     * The channel ID does not need to have an {@link AudioChannelDefinition} registered first.
     *
     * @param config priority and ducking policy for the channel
     */
    public void registerChannel(AudioChannelConfig config) {
        channelConfigs.put(config.channelId(), Validation.requireNonNull(config, "config"));
    }
```

Add `channelConfig` query method after `lastPlaybackCommand`:

```java
    /**
     * Returns the registered priority and ducking configuration for a channel.
     * Channels with no registered config return a default with priority 0 and {@link DuckingPolicy#NONE}.
     *
     * @param channelId registered audio channel ID
     */
    public AudioChannelConfig channelConfig(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        return channelConfigs.getOrDefault(channelId,
                new AudioChannelConfig(channelId, 0, 1.0, DuckingPolicy.NONE, 0.0));
    }
```

- [ ] **Step 5.6: Run tests to verify pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.audio.AudioChannelDuckingTest"
```
Expected: PASS (7 tests).

- [ ] **Step 5.7: Run all audio tests for regressions**

```
./gradlew --no-daemon test --tests "com.eb.javafx.audio.*"
```
Expected: all pass.

- [ ] **Step 5.8: Commit**

```
git add src/main/java/com/eb/javafx/audio/DuckingPolicy.java
git add src/main/java/com/eb/javafx/audio/AudioChannelConfig.java
git add src/main/java/com/eb/javafx/audio/AudioService.java
git add src/test/java/com/eb/javafx/audio/AudioChannelDuckingTest.java
git commit -m "feat(audio): add DuckingPolicy, AudioChannelConfig, and AudioService.registerChannel/channelConfig for priorities and ducking"
```

---

## Task 6: Final build and change summary

- [ ] **Step 6.1: Run the full build**

```
./gradlew --no-daemon build
```
Expected: BUILD SUCCESSFUL with no test failures.

- [ ] **Step 6.2: Write change summary**

Append to `change_summary.md` in the repository root (create if absent):

```markdown
## Phase 3 — Ren'Py Parity: Audio Completeness

Four audio features added to reach Ren'Py parity.

### Voice channel + auto-advance (3.1)
`AudioService.VOICE_CHANNEL = "voice"` added alongside existing channel constants. `AudioService.initialize()` registers the voice channel and reads `voiceVolume` from preferences. `PreferencesService` gains `voiceEnabled` (boolean, default true), `voiceVolume` (double, default 1.0), and `autoAdvanceOnVoiceEnd` (boolean, default false), each with a load path, accessor, and save method. `SceneStep` gains `voiceRef()` and `withVoiceRef(String)` metadata accessors. `SceneExecutionResult` gains `voiceRequest()` returning `Optional<SoundRequest>` and `withVoiceRequest(SoundRequest)` immutable wither. `SceneExecutor` populates `voiceRequest` on DIALOGUE and NARRATION steps that carry a `voiceRef`. Auto-advance is adapter-driven: adapter calls `SceneExecutor.advance()` when voice ends; the engine receives a normal advance signal.

### Scene transition sounds (3.2)
`VisualTransitionDefinition` gains a nullable `soundRef` field and `withSoundRef(String)` wither. `VisualTransitionRequest.soundRef()` delegates to the definition. Existing constructor and all callers unchanged.

### Music queue (3.3)
`SoundRequest.queued(String audioRef, String channelId)` static factory builds a non-looping full-volume request. `AudioService` gains `queueMusic(SoundRequest)` (single-depth queue — replaces any pending entry), `clearQueue(String channelId)`, and `queuedRequest(String channelId)` returning `Optional<SoundRequest>`. `stopChannel` now also clears any queued entry for that channel.

### Audio channel priorities and ducking (3.4)
`DuckingPolicy` enum: `NONE`, `REDUCE_TO_PERCENT`, `PAUSE`. `AudioChannelConfig` value object: channelId, priority (int), defaultVolume (double), duckingPolicy, duckingPercent. `AudioService.registerChannel(AudioChannelConfig)` overload stores configs keyed by channel ID. `AudioService.channelConfig(String)` returns the registered config or a default (priority 0, NONE) for unregistered channels. Engine defines the contract; adapters query `channelConfig()` and apply volume changes at play time.

**Files changed:**
- `src/main/java/com/eb/javafx/prefs/PreferencesService.java`
- `src/main/java/com/eb/javafx/audio/AudioService.java`
- `src/main/java/com/eb/javafx/audio/SoundRequest.java`
- `src/main/java/com/eb/javafx/audio/DuckingPolicy.java` (new)
- `src/main/java/com/eb/javafx/audio/AudioChannelConfig.java` (new)
- `src/main/java/com/eb/javafx/scene/SceneStep.java`
- `src/main/java/com/eb/javafx/scene/SceneExecutionResult.java`
- `src/main/java/com/eb/javafx/scene/SceneExecutor.java`
- `src/main/java/com/eb/javafx/transitions/VisualTransitionDefinition.java`
- `src/main/java/com/eb/javafx/transitions/VisualTransitionRequest.java`
```

- [ ] **Step 6.3: Rename change summary per branch naming convention**

Branch `claude/vigilant-vaughan-af6760` → filename `change_summary_claude_vigilant_vaughan_af6760.md`:

```
mv change_summary.md change_summary_claude_vigilant_vaughan_af6760.md
git add change_summary_claude_vigilant_vaughan_af6760.md
git commit -m "docs: add Phase 3 Ren'Py parity change summary"
```

---

## Self-review

### Spec coverage check

| Spec item | Covered by task |
|-----------|----------------|
| 3.1 `AudioChannel.VOICE` constant | Task 1 — `AudioService.VOICE_CHANNEL` |
| 3.1 `SceneStep.voiceRef()` | Task 2 |
| 3.1 `SceneExecutionResult.voiceRequest()` | Task 2 |
| 3.1 `PreferencesService` voiceEnabled/voiceVolume/autoAdvanceOnVoiceEnd | Task 1 |
| 3.1 auto-advance is adapter-driven (no timer in engine) | Documented in change summary; adapter calls `advance()` |
| 3.2 `VisualTransitionDefinition.soundRef()` | Task 3 |
| 3.2 `VisualTransitionRequest` carries soundRef | Task 3 |
| 3.3 `AudioService.queueMusic(SoundRequest)` | Task 4 |
| 3.3 single-depth queue (replace on second call) | Task 4 |
| 3.3 `SoundRequest.queued()` factory | Task 4 |
| 3.3 `AudioService.clearQueue(String)` | Task 4 |
| 3.4 `DuckingPolicy` enum | Task 5 |
| 3.4 `AudioChannelConfig` value object | Task 5 |
| 3.4 `AudioService.registerChannel(AudioChannelConfig)` | Task 5 |
| 3.4 default config (priority 0, NONE) for unregistered channels | Task 5 — `channelConfig()` fallback |
| 3.4 engine defines contract, adapter implements | `channelConfig()` query method; no auto-ducking in `play()` |

No gaps found.

### Placeholder scan

No TBD, TODO, or incomplete steps. All code blocks are complete.

### Type consistency check

- `AudioService.VOICE_CHANNEL` (String `"voice"`) used consistently in Task 1 registration, Task 2 `buildVoiceRequest`, and Task 2 tests.
- `SoundRequest.queued(audioRef, channelId)` — parameter order: asset path first, channel second. Used consistently in Task 4 tests.
- `AudioChannelConfig` constructor parameter order (channelId, priority, defaultVolume, duckingPolicy, duckingPercent) consistent between Task 5 implementation and all test usages.
- `VisualTransitionDefinition.withSoundRef(String)` returns `VisualTransitionDefinition` — used correctly in Task 3 tests.
- `SceneExecutionResult.withVoiceRequest(SoundRequest)` returns `SceneExecutionResult` — wither pattern used in Task 2 executor wiring and tests.
