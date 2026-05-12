# Ren'Py Full Parity — Gap Document and Implementation Design

_Created: 2026-05-12_

## Purpose

This document identifies every Ren'Py feature area not yet present in the novlfx engine and
specifies a Java-idiomatic design for each one. The goal is complete parity: any Ren'Py game
should be portable to this engine without losing capabilities.

Python-centric Ren'Py features (inline `python:` blocks, store namespaces, dynamic scripting)
are intentionally excluded. Where Ren'Py uses Python for extensibility, this design uses
Java-idiomatic alternatives: registered factories, expression DSLs, event buses, and
configurable value objects.

---

## Scope boundary

**Engine owns:** data models, registries, executors, codecs, view models, contracts.
**Adapter owns:** rendering, animation playback, input handling, platform APIs.
**Application owns:** authored content, assets, domain-specific progression rules.

---

## Implementation phases

Features are grouped into four phases. Each phase is independently shippable and earlier
phases unblock later ones.

| Phase | Theme | Items |
|-------|-------|-------|
| 1 | Foundational scene mechanics | 6 |
| 2 | Display completeness | 5 |
| 3 | Audio completeness | 4 |
| 4 | UI and meta polish | 7 |

---

## Phase 1 — Foundational scene mechanics

### 1.1 Dialogue rollback

**Ren'Py equivalent:** right-click to roll back through previous dialogue lines.

**Engine design:**

- `RollbackSnapshot` — immutable record capturing all restorable state at one step boundary:
  scene ID, step index, `ProgressSnapshot`, `SeenStepSnapshot`, plus inventory/journal/
  wardrobe snapshots for any of those services registered at bootstrap. Snapshots are taken
  before each step executes.
- `RollbackBuffer` — fixed-size ring buffer of `RollbackSnapshot` entries. Capacity is
  configurable at bootstrap (default 100). When full, oldest entries are discarded.
- `SceneExecutor.rollback(int steps)` — pops N snapshots, restores all state atomically,
  and returns a `SceneExecutionResult` for the restored step so the adapter re-displays it.
- `SceneExecutionResult.canRollback()` — boolean; false when the buffer is empty or rollback
  is disabled (e.g. during skip mode).
- `RollbackSnapshotCodec` — for persisting the buffer into a save section so rollback
  survives save/load.
- Adapter contract: one optional hook `Runnable onRollbackEffect` supplied at executor
  construction time. The engine calls it before re-displaying the restored step. The adapter
  plays whatever rewind visual it chooses; the engine does not dictate the effect.

**Key types:** `RollbackSnapshot`, `RollbackBuffer`, `RollbackSnapshotCodec`
**Modified types:** `SceneExecutor`, `SceneExecutionResult`
**Package:** `com.eb.javafx.scene`

---

### 1.2 Conditional choice visibility

**Ren'Py equivalent:** `menu` items with `if <condition>` guards that hide or grey options.

**Engine design:**

- `ChoiceOption` gains optional `conditionExpression` (a `SceneConditionExpression` — the
  existing expression DSL implemented for the CONDITIONAL step type) and `ConditionPolicy` enum:
  - `HIDE` — option is removed from the view model entirely when the condition is false.
  - `GREY` — option is present in the view model but `enabled = false`.
- `ChoiceOptionViewModel` gains `visible` and `enabled` booleans.
- `SceneExecutor` evaluates conditions when building the CHOICE step view model, before
  the view model is emitted to the adapter.
- No changes to `SceneConditionExpression` or `SceneConditionEvaluator`.

**Key types:** `ConditionPolicy`
**Modified types:** `ChoiceOption`, `ChoiceOptionViewModel`, `SceneExecutor`
**Package:** `com.eb.javafx.scene`

---

### 1.3 Text pacing control tags

**Ren'Py equivalent:** in-line text tags `{w}`, `{w=N}`, `{p}`, `{nw}`, `{cps=N}`, `{fast}`.

**Engine design:**

New token types added to the existing `TextTagParser` token stream:

| Tag | Token type | Payload |
|-----|-----------|---------|
| `{w}` | `WAIT_CLICK` | — |
| `{w=N}` | `WAIT_SECONDS` | `float` seconds |
| `{p}` | `PARAGRAPH_BREAK` | — |
| `{nw}` | `NO_WAIT` | — |
| `{cps=N}` | `SET_CPS` | `int` chars/sec |
| `{fast}` | `FAST_FORWARD` | — |

Pacing tokens are emitted inline in the token stream alongside styled text tokens.
`SceneDialogueRowViewModel` already exposes the full token list — adapters consume pacing
tokens to drive typewriter speed, paragraph clearing, and auto-advance. No behaviour change
in `SceneExecutor`.

**Modified types:** `TextTagParser`, token type enum
**Package:** `com.eb.javafx.text`

---

### 1.4 Timed choices

**Ren'Py equivalent:** a choice menu that auto-selects after N seconds.

**Engine design:**

- `SceneStep` gains `choiceTimeoutMs()` (optional `Long`; null = no timeout) and
  `choiceTimeoutDefaultId()` (optional choice ID to auto-select; null = select first option).
- `ChoiceViewModel` exposes both fields.
- The executor does not run the timer — that is adapter territory. When the timeout elapses,
  the adapter calls the existing `SceneExecutor.selectChoice(choiceId)` with the default ID.
  The engine receives a normal choice selection.
- `SceneStep.timedChoice(long timeoutMs, String defaultId)` factory method.

**Modified types:** `SceneStep`, `ChoiceViewModel`
**Package:** `com.eb.javafx.scene`

---

### 1.5 Menu captions

**Ren'Py equivalent:** narration text shown above a choice menu before the options appear.

**Engine design:**

- `SceneStep` gains `menuCaptionTextKey()` (optional String — a localization key).
- `ChoiceViewModel` gains `captionTextKey()` (optional String).
- Adapter renders the caption above the choice list.
- One metadata key on `SceneStep`; no executor changes.

**Modified types:** `SceneStep`, `ChoiceViewModel`
**Package:** `com.eb.javafx.scene`

---

### 1.6 Sticky / loop menus

**Ren'Py equivalent:** a menu that re-displays after a non-exit choice until the player
selects the exit option.

**Engine design:**

- `ChoiceOption` gains `exitsMenu(boolean)` (default `true`). Options with `exitsMenu = false`
  return control to the same CHOICE step after their transition resolves.
- `SceneStep` gains `menuLoop(boolean)` (default `false`). When true, the executor re-presents
  the step after any choice whose `exitsMenu` is false resolves.
- Exit detection is a per-option flag — no graph analysis needed.
- `SceneStep.loopingMenu(List<ChoiceOption>)` factory method where authors mark exit options
  explicitly.

**Modified types:** `SceneStep`, `ChoiceOption`
**Package:** `com.eb.javafx.scene`

---

## Phase 2 — Display completeness

### 2.1 Layered image composition

**Ren'Py equivalent:** `layered image` — composites multiple image parts (body, outfit,
hair, expression) into a single character sprite.

**Engine design:**

- `LayeredImageVariant` — image ref + optional `SceneConditionExpression`. The first variant
  whose condition evaluates true (or the unconditional default) is selected.
- `LayeredImageLayer` — named slot (e.g. `"expression"`) + ordered list of
  `LayeredImageVariant` entries.
- `LayeredImageDefinition` — id, display tag it maps to, ordered list of
  `LayeredImageLayer` entries.
- `LayeredImageRegistry` — startup registry; validates all image refs exist in
  `ImageDisplayRegistry`. Reports missing refs as diagnostics.
- `LayeredImageResolver` — given a `LayeredImageDefinition` and current `ActionContext`,
  evaluates each layer and returns a `LayeredImageComposition`.
- `LayeredImageComposition` — ordered list of (layer name → resolved image ref) pairs in
  z-order. This is what the adapter receives and renders by stacking images.

The engine resolves which images to show; the adapter composites them. No rendering logic
in the engine.

**New package:** `com.eb.javafx.display` (additions)
**Key types:** `LayeredImageVariant`, `LayeredImageLayer`, `LayeredImageDefinition`,
`LayeredImageRegistry`, `LayeredImageResolver`, `LayeredImageComposition`

---

### 2.2 Zorder management

**Ren'Py equivalent:** `zorder` attribute on `show` statements controlling display-stack
ordering.

**Engine design:**

- `DisplayTransform` gains `zorder(int)` field (default 0).
- `DisplayTagDefinition` gains `zorder(int)` field (default 0).
- When the scene executor emits a show/display step result, the resolved `DisplayTransform`
  carries the zorder value. Adapters sort their render list by zorder ascending before
  each frame. No separate registry — zorder travels with the transform.
- `DisplayTransform.withZorder(int)` builder method.

**Modified types:** `DisplayTransform`, `DisplayTagDefinition`
**Package:** `com.eb.javafx.display`

---

### 2.3 Image maps / clickable hotspot areas

**Ren'Py equivalent:** `imagemap` — a background image with named clickable regions used
for map screens and location navigation.

**Engine design:**

- `HotspotDefinition` — id, label text key, fractional bounds (`x`, `y`, `width`, `height`
  in range 0.0–1.0 relative to image dimensions), optional `SceneConditionExpression`, and
  a transition target (scene ID or route ID).
- `HotspotMapDefinition` — id, background image ref, ordered list of `HotspotDefinition`.
- `HotspotMapRegistry` — startup registry; validates the background image ref for each
  registered map exists in `ImageDisplayRegistry`.
- `SceneStepType.HOTSPOT_MAP` — new step type carrying a map ID.
- `HotspotMapViewModel` — background image ref + list of `HotspotOptionViewModel`
  (id, labelTextKey, enabled boolean from condition evaluation, fractional bounds).
- Adapter calls `SceneExecutor.selectHotspot(String hotspotId)` — mirrors
  `selectChoice(id)` in the existing choice flow.

**New types:** `HotspotDefinition`, `HotspotMapDefinition`, `HotspotMapRegistry`,
`HotspotMapViewModel`, `HotspotOptionViewModel`
**Modified types:** `SceneStepType`, `SceneExecutor`
**Package:** `com.eb.javafx.display`

---

### 2.4 ATL completeness

**Ren'Py equivalent:** Animation and Transform Language blocks — `parallel`, `sequence`,
`repeat`, `on show/hide/replace/hover`.

**Engine design:**

The existing `DisplayAnimation` handles linear/ease keyframe sequences. Extended with:

- `AnimationBlockType` enum: `SEQUENCE`, `PARALLEL`, `REPEAT`.
- `AnimationBlock` — contains child `AnimationBlock` entries or leaf keyframes, plus a
  `blockType` and `repeatCount` (0 = infinite for REPEAT blocks).
- `AnimationEventTrigger` enum: `SHOW`, `HIDE`, `REPLACE`, `HOVER`.
- `AnimationDefinition` gains a map of `AnimationEventTrigger → AnimationBlock` for
  trigger-based activation. Untriggered (always-on) animations remain as top-level blocks.
- `DisplayAnimation.forTrigger(AnimationEventTrigger)` returns the block registered for
  that trigger, or empty if none. Adapters call this to install the right animation at the
  right moment (e.g. on show, on hide).
- Existing keyframe animations are valid leaf nodes inside any block type — fully backward
  compatible.

**Modified types:** `DisplayAnimation`, `AnimationDefinition`
**New types:** `AnimationBlock`, `AnimationBlockType`, `AnimationEventTrigger`
**Package:** `com.eb.javafx.display`

---

### 2.5 Talking animations

**Ren'Py equivalent:** a character sprite that animates while dialogue is being displayed
and returns to idle on advance.

**Engine design:**

- `CharacterTemplate` gains optional `talkingAnimationId` and `idleAnimationId` fields.
- `TalkingAnimationCue` — value object: display tag (the character speaking), talking
  animation ID, idle animation ID.
- `SceneExecutionResult` gains optional `talkingCue()`.
- When `SceneExecutor` builds a DIALOGUE result for a named speaker, it looks up the
  speaking character's `CharacterTemplate`. If both animation IDs are present it populates
  `talkingCue`. If the speaker has no template or no animation IDs, `talkingCue` is empty.
- Adapter starts the talking animation when the cue is present and switches to idle on step
  advance. No adapter is required to implement this — absence of a cue is valid.

**New types:** `TalkingAnimationCue`
**Modified types:** `CharacterTemplate`, `SceneExecutionResult`
**Packages:** `com.eb.javafx.characters`, `com.eb.javafx.scene`

---

## Phase 3 — Audio completeness

### 3.1 Voice channel and auto-advance

**Ren'Py equivalent:** `voice` channel with optional auto-advance when playback ends.

**Engine design:**

- `AudioChannel` gains `VOICE` as a named constant alongside existing channel constants.
- `SceneStep` gains `voiceRef()` (optional String — audio asset ref for the voice line).
- `SceneExecutionResult` gains `voiceRequest()` (optional `SoundRequest` pre-configured for
  `AudioChannel.VOICE`). The executor populates it when the step has a voice ref.
- `PreferencesService` gains three new preferences: `voiceEnabled` (boolean, default true),
  `voiceVolume` (float, default 1.0), `autoAdvanceOnVoiceEnd` (boolean, default false).
- Auto-advance is behavioural: when `autoAdvanceOnVoiceEnd` is enabled the adapter calls
  `SceneExecutor.advance()` when voice playback completes. The engine receives it as a
  normal advance signal — no timer or callback in the engine.

**Modified types:** `AudioChannel`, `SceneStep`, `SceneExecutionResult`, `PreferencesService`
**Package:** `com.eb.javafx.audio`, `com.eb.javafx.scene`

---

### 3.2 Scene transition sounds

**Ren'Py equivalent:** a sound effect played during a visual scene transition.

**Engine design:**

- `VisualTransitionDefinition` gains optional `soundRef()` (String — audio asset ref).
- `VisualTransitionRequest` carries `soundRef()` through to the adapter.
- Adapter plays the sound when it starts the transition effect. If no sound ref is present,
  no sound plays. Two field additions; no executor changes.

**Modified types:** `VisualTransitionDefinition`, `VisualTransitionRequest`
**Package:** `com.eb.javafx.transitions`

---

### 3.3 Music queue

**Ren'Py equivalent:** `queue music` — schedule the next track to start when the current
one ends naturally.

**Engine design:**

- `AudioService` contract gains `queueMusic(SoundRequest)` — schedules a track to play
  when the current channel's playback ends without interruption (not on fade or stop).
- The queue is single-depth: calling `queueMusic` again replaces the pending entry. This
  matches Ren'Py's queue semantics without over-engineering a full playlist.
- `SoundRequest` gains a static factory `queued(String audioRef, String channel)` to
  distinguish queued from immediate requests at the call site.
- `AudioService` gains `clearQueue(String channel)` to cancel a pending queued track.

**Modified types:** `AudioService`, `SoundRequest`
**Package:** `com.eb.javafx.audio`

---

### 3.4 Audio channel priorities and ducking

**Ren'Py equivalent:** voice-over-music ducking; higher-priority channels reduce or pause
lower-priority ones.

**Engine design:**

- `DuckingPolicy` enum: `NONE`, `REDUCE_TO_PERCENT`, `PAUSE`.
- `AudioChannelConfig` — id, `priority` (int; higher = more important), `defaultVolume`
  (float), `duckingPolicy` (policy applied to lower-priority channels when this one starts),
  `duckingPercent` (float; used when policy is `REDUCE_TO_PERCENT`).
- `AudioService` contract gains `registerChannel(AudioChannelConfig)`. Channels not
  explicitly registered use a default config with priority 0 and `NONE` ducking.
- Priority evaluation happens at play-time: before starting a channel, the service checks
  all active channels and applies ducking. Unducking happens when the higher-priority
  channel stops or fades out. Engine defines the contract; adapter implementation applies
  the volume changes.

**New types:** `DuckingPolicy`, `AudioChannelConfig`
**Modified types:** `AudioService`
**Package:** `com.eb.javafx.audio`

---

## Phase 4 — UI and meta polish

### 4.1 Overlay / persistent screens

**Ren'Py equivalent:** screens shown with `show screen` that persist across scene changes
(e.g. persistent HUDs, notification overlays).

**Engine design:**

- `RouteModule.registerOverlay(String overlayId, ScreenFactory)` — registers a screen
  that is not destroyed on route navigation.
- `SceneRouter` maintains a map of active overlay instances. On navigation, the main scene
  is rebuilt but overlay instances are retained and re-attached to the new scene graph.
- `SceneRouter.showOverlay(String overlayId)` and `hideOverlay(String overlayId)` control
  visibility without destroying state.
- Overlays receive the same `RouteContext` as the current scene so they can trigger
  navigation, but selecting a route replaces the main scene, not the overlay.
- `OverlayDescriptor` — id, factory, initial visibility (shown/hidden).

**New types:** `OverlayDescriptor`
**Modified types:** `RouteModule`, `SceneRouter`
**Package:** `com.eb.javafx.routing`

---

### 4.2 Screen variants

**Ren'Py equivalent:** screen variants for different window sizes and accessibility modes.

**Engine design:**

- `WindowSizeClass` enum: `COMPACT`, `MEDIUM`, `EXPANDED` (derived from window width
  thresholds configurable at bootstrap).
- `ScreenVariantCriteria` — predicate over `WindowSizeClass` and `AccessibilityMode`
  (existing `AccessibilityProfile` fields).
- When registering a JSON-backed route, authors call `addVariant(criteria, jsonPath)` to
  supply alternative layouts.
- `ScreenVariantResolver.resolve(String screenId, RouteContext context)` selects the best
  matching JSON path at render time, falling back to the default if no variant matches.
  Matching is first-criteria-wins in registration order.
- No engine internals change beyond the route descriptor; the resolver sits between the
  route factory and the JSON loader.

**New types:** `WindowSizeClass`, `ScreenVariantCriteria`, `ScreenVariantResolver`
**Modified types:** `RouteDescriptor`
**Package:** `com.eb.javafx.routing`

---

### 4.3 Image buttons in screen design

**Ren'Py equivalent:** `imagebutton` — a clickable image with idle, hover, and selected
states.

**Engine design:**

- New `ScreenDesignItemType.IMAGE_BUTTON`.
- Three metadata fields: `idleImageRef`, `hoverImageRef` (optional), `selectedImageRef`
  (optional).
- `ScreenDesignValidator` requires `idleImageRef` for IMAGE_BUTTON items.
- `ScreenLayoutRenderer` creates a clickable container with state-driven image switching
  (idle → hover on mouse enter, idle/selected on click).
- Follows the same item type extension pattern as POPLIST, COMBO_BOX, SLIDER, RADIO_GROUP.

**Modified types:** `ScreenDesignItemType`, `ScreenDesignValidator`, `ScreenLayoutRenderer`
**Package:** `com.eb.javafx.ui`

---

### 4.4 CTC indicator definition

**Ren'Py equivalent:** customizable click-to-continue indicator shown while waiting for
player input.

**Engine design:**

- `CtcPosition` enum: `BOTTOM_RIGHT`, `BOTTOM_CENTER`, `INLINE` (appears after the last
  token in the text flow).
- `CtcIndicatorDefinition` — image ref, optional animation ID, `CtcPosition`.
- `CtcIndicatorRegistry` — holds one definition per `SceneDisplayMode` (ADV, NVL). If no
  definition is registered for a mode, no indicator is emitted.
- `SceneDialogueRowViewModel` gains `ctcIndicator()` returning
  `Optional<CtcIndicatorDefinition>`. The executor populates it when the step requires a
  click to advance.
- Adapters render and animate the indicator. No adapter is required to implement it.

**New types:** `CtcPosition`, `CtcIndicatorDefinition`, `CtcIndicatorRegistry`
**Modified types:** `SceneDialogueRowViewModel`
**Package:** `com.eb.javafx.scene`

---

### 4.5 Cross-playthrough persistent data

**Ren'Py equivalent:** `persistent.` variables that survive across multiple playthroughs
and save slots.

**Engine design:**

- `PersistentProgressTracker` — mirrors the `ProgressTracker` API (flags, counters,
  milestones, unlocks) but is backed by Java's `java.util.prefs.Preferences` under a
  game-namespaced node, completely separate from save slots.
- Loaded at bootstrap before any scene runs; saved immediately on each mutation.
- `BootContext` gains `persistentProgress()` returning `PersistentProgressTracker`.
- `PersistentProgressSnapshot` and `PersistentProgressSnapshotCodec` support export and
  import for backup/transfer. Primary storage is always `Preferences`.
- No impact on existing `ProgressTracker`, `SaveSnapshotDocument`, or save/load flows.

**New types:** `PersistentProgressTracker`, `PersistentProgressSnapshot`,
`PersistentProgressSnapshotCodec`
**Modified types:** `BootContext`
**Package:** `com.eb.javafx.progress`

---

### 4.6 Quick save / quick load

**Ren'Py equivalent:** F5 quick save, F9 quick load.

**Engine design:**

- `QuickSaveService` — wraps `SaveLoadService` with a configurable designated slot ID
  (default `"quicksave"`). Exposes `quickSave(GameState)` and
  `quickLoad()` → `Optional<GameState>`.
- `InputAction` gains `QUICK_SAVE` and `QUICK_LOAD` constants.
- `InputMap` pre-registers default bindings: F5 → `QUICK_SAVE`, F9 → `QUICK_LOAD` on
  desktop. Authors can rebind or disable.
- `GlobalApiAdapter` listens for these input actions and delegates to `QuickSaveService`.
- `QuickSaveEvent` — a `GameEvent` emitted after a successful quick save so overlays can
  show confirmation.

**New types:** `QuickSaveService`, `QuickSaveEvent`
**Modified types:** `InputAction`, `InputMap`, `GlobalApiAdapter`
**Package:** `com.eb.javafx.save`

---

### 4.7 Achievement system

**Ren'Py equivalent:** `achievement` system — unlockable meta-game rewards separate from
gallery CGs.

**Engine design:**

- `AchievementDefinition` — id, `nameTextKey`, `descriptionTextKey`, `iconRef` (optional),
  `SceneConditionExpression` unlock condition.
- `AchievementRegistry` — startup registry; validates all icon refs at startup and reports
  missing ones as diagnostics.
- `AchievementState` — mutable set of unlocked achievement IDs.
- `AchievementService` — `checkAll(ActionContext)` evaluates all locked definitions and
  calls `unlock(id)` for those whose condition is met. Each unlock emits a
  `AchievementUnlockedEvent` on `GameEventBus` so adapters can show notifications.
- `AchievementSnapshot` / `AchievementSnapshotCodec` — persist unlocked IDs as a
  `SaveSnapshotSection`.
- Entirely separate from `GalleryService`. Achievements are meta-game rewards; gallery is
  CG viewing.

**New types:** `AchievementDefinition`, `AchievementRegistry`, `AchievementState`,
`AchievementService`, `AchievementSnapshot`, `AchievementSnapshotCodec`,
`AchievementUnlockedEvent`
**New package:** `com.eb.javafx.achievements`

---

## Summary table

| # | Feature | Phase | Package(s) | Invasive? |
|---|---------|-------|-----------|-----------|
| 1.1 | Dialogue rollback | 1 | scene | Yes — SceneExecutor |
| 1.2 | Conditional choice visibility | 1 | scene | Minor — ChoiceOption |
| 1.3 | Text pacing control tags | 1 | text | Additive |
| 1.4 | Timed choices | 1 | scene | Additive |
| 1.5 | Menu captions | 1 | scene | Additive |
| 1.6 | Sticky/loop menus | 1 | scene | Minor — SceneExecutor |
| 2.1 | Layered image composition | 2 | display | Additive |
| 2.2 | Zorder management | 2 | display | Minor — DisplayTransform |
| 2.3 | Image maps / hotspot areas | 2 | display | Additive + new step type |
| 2.4 | ATL completeness | 2 | display | Minor — DisplayAnimation |
| 2.5 | Talking animations | 2 | characters, scene | Additive |
| 3.1 | Voice channel + auto-advance | 3 | audio, scene | Minor — SceneStep |
| 3.2 | Scene transition sounds | 3 | transitions | Additive |
| 3.3 | Music queue | 3 | audio | Additive |
| 3.4 | Audio channel priorities | 3 | audio | Additive |
| 4.1 | Overlay/persistent screens | 4 | routing | Yes — SceneRouter |
| 4.2 | Screen variants | 4 | routing | Additive |
| 4.3 | Image buttons | 4 | ui | Additive |
| 4.4 | CTC indicator | 4 | scene | Additive |
| 4.5 | Cross-playthrough persistence | 4 | progress | Additive |
| 4.6 | Quick save/load | 4 | save | Additive |
| 4.7 | Achievement system | 4 | achievements | Additive |

Invasive = modifies an existing public API or executor behaviour. Additive = new classes only.
