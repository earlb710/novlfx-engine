# Change summary — dialog block reusability

## Why
The `runDialogBlockTestScreen` demo was the only path that wired the dialog block into a real
`MAIN_APP_LAYOUT`. Three pieces of engine plumbing leaked into the demo body — `bindToFooter`,
`bindHistoryToggle` (with a manually computed height share), and `installKeyboardShortcuts`. Any
new app reusing `DialogEntriesView` would have to copy the same boilerplate, and the manually
computed `1.0 - DEFAULT_STORY_DIALOG_RATIO` silently broke if the layout plan used a non-default
ratio. The speaker column width was also a hardcoded `160`, so apps with longer (or much shorter)
speaker names couldn't tune it.

## What changed

### Engine
- `DialogEntriesView`:
  - New `setSpeakerColumnWidth(double)` / `speakerColumnWidth()` API; previous hardcoded `160`
    becomes `DEFAULT_SPEAKER_COLUMN_WIDTH` and is the default for the now-instance field.
  - `renderEntry` / `renderSpoken` made instance methods so they read the configurable width.
  - `installKeyboardShortcuts(Scene)` is now idempotent (tracks wired scenes) and auto-installs
    via a `sceneProperty()` listener as soon as the view is added to a scene.
  - `bindToFooter(Node)` and `bindHistoryToggle(Node, Node, double)` are idempotent — repeat
    calls to the same footer are silently skipped so renderer auto-wiring plus a manual app call
    don't stack listeners.
- `MainAppLayoutRenderer.render(...)` now auto-detects a `DialogEntriesView` in the dialog slot
  and calls `bindToFooter(root)` plus `bindHistoryToggle(root, storyArea, 1.0 - plan.storyDialogRatio())`
  itself, using the plan's actual ratio (no more leaky `DEFAULT_STORY_DIALOG_RATIO` assumption).
  Auto-wire is gated on `showFooter` and no-ops when the dialog slot isn't a `DialogEntriesView`.

### Demo
- `DialogBlockTestScreenApplication.start(Stage)` is back to content-only: build speakers, build
  the dialog view, seed conversations, build the plan/resolver, render, scene, show. The three
  plumbing lines (`bindToFooter`, `bindHistoryToggle`, `installKeyboardShortcuts`) and the manual
  `dialogHeightShare` computation are gone — the engine handles them.

### Tests
- New `DialogEntriesViewTest` cases:
  - `setSpeakerColumnWidthAppliesToExistingAndNewEntries`
  - `setSpeakerColumnWidthRejectsNonPositive`
  - `installKeyboardShortcutsIsIdempotent`
  - `bindToFooterIsIdempotent`
- New `MainAppLayoutRendererDialogAutoWireTest` locks the auto-wiring contract: footer back/forward
  drive `goBack()`/`goForward()` without any app-side wiring, the ◷ history toggle toggles
  `isHistoryMode()`, and `showFooter=false` skips the auto-wire path entirely.

### Docs
- `docs/USER_MANUAL.md` updated:
  - "Typical wiring" example no longer calls `bindToFooter` / `installKeyboardShortcuts` and notes
    that `render(...)` handles them.
  - New examples for multi-line text (hard `\n` line breaks vs natural wrap), speaker column
    width configuration, and the history (◷) toggle (programmatic + manual wiring).
  - API table gains `setSpeakerColumnWidth(double)` and notes the auto-wire / idempotent
    behaviour for `bindToFooter`, `bindHistoryToggle`, and `installKeyboardShortcuts`.

## Validation
- `./gradlew --no-daemon testClasses` — clean compile.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.*` — all UI tests pass, including the
  new `MainAppLayoutRendererDialogAutoWireTest` and the four new `DialogEntriesViewTest` cases.
- `./gradlew --no-daemon test --tests com.eb.javafx.testscreen.DialogBlockTestScreenApplicationTest`
  — the demo's seed contract is unchanged (10 conversations, multi-line + long-wrap entries).
