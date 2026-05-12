# novlfx Engine — Ren'Py Feature Parity TODO

_Last updated: 2026-05-12_

This document tracks the six remaining feature areas needed to reach Ren'Py feature parity.
Each item has a linked implementation plan under `docs/superpowers/plans/`.

---

## Backlog

| # | Feature | Priority | Status | Plan |
|---|---------|----------|--------|------|
| 1 | [Visual Scene Transitions](#1-visual-scene-transitions) | High | Not started | [2026-05-12-renpy-feature-parity.md](superpowers/plans/2026-05-12-renpy-feature-parity.md#task-group-1-visual-scene-transitions) |
| 2 | [Image Tag Resolution](#2-image-tag-resolution) | High | Not started | [2026-05-12-renpy-feature-parity.md](superpowers/plans/2026-05-12-renpy-feature-parity.md#task-group-2-image-tag-resolution) |
| 3 | [Skip / Auto Modes](#3-skip--auto-modes) | High | Not started | [2026-05-12-renpy-feature-parity.md](superpowers/plans/2026-05-12-renpy-feature-parity.md#task-group-3-skip--auto-modes) |
| 4 | [NVL Mode](#4-nvl-mode) | Medium | Not started | [2026-05-12-renpy-feature-parity.md](superpowers/plans/2026-05-12-renpy-feature-parity.md#task-group-4-nvl-mode) |
| 5 | [Scene-Level Conditionals](#5-scene-level-conditionals) | Medium | Not started | [2026-05-12-renpy-feature-parity.md](superpowers/plans/2026-05-12-renpy-feature-parity.md#task-group-5-scene-level-conditionals) |
| 6 | [Gallery Framework](#6-gallery-framework) | Medium | Not started | [2026-05-12-renpy-feature-parity.md](superpowers/plans/2026-05-12-renpy-feature-parity.md#task-group-6-gallery-framework) |

---

## 1. Visual Scene Transitions

**What:** Named visual-effect definitions (dissolve, fade, wipe, move) that the display adapter
plays between scene changes. Currently the engine has no concept of visual transitions between
scenes — only flow-control transitions (jump/call/return).

**What's needed:**
- `SceneTransitionEffect` enum (DISSOLVE, FADE_BLACK, WIPE_LEFT, WIPE_RIGHT, MOVE_IN_LEFT, MOVE_IN_RIGHT, NONE)
- `VisualTransitionDefinition` — named definition with effect type and duration ms
- `VisualTransitionRegistry` — startup registry of named definitions
- `VisualTransitionRequest` — runtime value object emitted by the scene executor so adapters can play the effect

**Acceptance criteria:**
- Scene steps can reference a named visual transition
- `SceneExecutionResult` carries an optional `VisualTransitionRequest`
- Registry validates that all transition references exist
- Unit tests cover definition, registry, and result emission

---

## 2. Image Tag Resolution

**What:** Semantic display tag system so scene authors can write `"show hero_happy left"` and
the engine resolves it to a display ID, layer, and transform — rather than authors hard-coding
display IDs and pixel positions.

**What's needed:**
- `DisplayTagDefinition` — tag string → target display ID, layer override, transform preset name
- `DisplayTagRegistry` — startup registry of tag definitions, with validation
- `DisplayTagResolver` — resolves a tag + optional position hint to a concrete `DisplayTransform`

**Acceptance criteria:**
- Tag registry detects duplicate tags and broken display-ID references
- Resolver returns the correct transform for a known tag
- Unknown tags return a descriptive error, not a null
- Unit tests cover resolution, missing tag, and duplicate-tag validation

---

## 3. Skip / Auto Modes

**What:** Two common VN playback modes that Ren'Py ships with out of the box:
- **Skip mode** — fast-forwards through any step the player has already seen (by sceneId+stepId)
- **Auto mode** — automatically advances text steps after a configurable delay

**What's needed:**
- `SeenStepTracker` — mutable set of `"sceneId:stepId"` keys; snapshottable for save/load
- `ScenePlaybackMode` enum — NORMAL, SKIP, AUTO
- `SceneExecutor.advanceSkipping()` — advances without pausing on DIALOGUE/NARRATION steps whose key is in the tracker
- `SeenStepSnapshot` / `SeenStepSnapshotCodec` — persistence support

**Acceptance criteria:**
- Skip mode bypasses seen text steps and pauses on unseen steps and all CHOICE steps
- Skip mode always stops at CHOICE steps even if previously seen
- SeenStepTracker snapshot round-trips through JSON
- Unit tests cover all three modes

---

## 4. NVL Mode

**What:** Full-screen text presentation mode (Ren'Py's NVL mode) where multiple lines accumulate
on screen instead of a single dialogue box at the bottom. The engine side is presentation-hint
only — the UI adapter renders differently based on the hint.

**What's needed:**
- `SceneDisplayMode` enum — ADV (default), NVL
- `SceneStep.withDisplayMode(SceneDisplayMode)` — stored in metadata under key `"displayMode"`
- `SceneStep.displayMode()` — read accessor that defaults to ADV
- `SceneDialogueRowViewModel` updated to expose `displayMode()`

**Acceptance criteria:**
- Steps default to ADV when no metadata key is present
- NVL steps carry the hint through to the view model
- No behaviour change in `SceneExecutor` — mode is purely presentational
- Unit tests verify the metadata round-trip

---

## 5. Scene-Level Conditionals

**What:** A `CONDITIONAL` step type that branches on simple game-state expressions without
requiring a visible CHOICE step. Covers the Ren'Py `if/elif/else` pattern used for invisible
branching.

**What's needed:**
- `SceneConditionExpression` — parseable string DSL: `flag:id`, `!flag:id`, `unlock:id`,
  `counter:id>=N`, `counter:id>N`, `counter:id<N`, `counter:id==N`
- `SceneConditionEvaluator` — evaluates expression against `ActionContext`
- `SceneStep.conditional()` factory — stores thenTransition + elseTransition + expression
- `SceneExecutor` switch case for `CONDITIONAL` — evaluates and applies the matching transition

**Acceptance criteria:**
- All six expression forms evaluate correctly
- Unknown or malformed expressions throw `IllegalArgumentException` at parse time
- `SceneExecutor` tests verify then-branch and else-branch routing
- `SceneRegistry` validation reports broken transition references from CONDITIONAL steps

---

## 6. Gallery Framework

**What:** Reusable gallery/CG system backed by `ProgressTracker` unlocks. Games can register
gallery entries at startup and query which are viewable by the player.

**What's needed:**
- `GalleryEntry` — id, imageRef, captionTextKey, required unlock id
- `GalleryDefinition` — id, titleTextKey, ordered list of entries
- `GalleryRegistry` — startup registry, validates that imageRefs exist in `ImageDisplayRegistry`
- `GalleryService` — queries registry + `ProgressTracker` to return `GalleryEntryViewModel` list
- `GalleryEntryViewModel` — id, captionTextKey, imageRef, `boolean unlocked`

**Acceptance criteria:**
- Locked entries are returned with `unlocked=false` and no image ref exposed
- Unlocked entries expose the full image ref
- Registry validates all image refs at startup and reports missing ones
- Unit tests cover locked, unlocked, and mixed gallery queries
