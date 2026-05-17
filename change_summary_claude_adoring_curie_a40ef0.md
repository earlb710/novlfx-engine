# Change summary

## Problem

AltLife uses the engine's `MAIN_APP_LAYOUT` + `DialogEntriesView` for its gameplay
screen but reported seven issues against it: mouse left/right click and key
bindings doing nothing, the dialog block not styled black at 50% opacity, the
history toggle "clearing" the story area while leaving a block above the dialog,
the history view flickering between two sizes, and history rendering lines past
the current reading position.

Root causes split between the engine (a layout feedback loop in history mode,
no way to opt out of the built-in cursor walks, no way to clip history at the
cursor) and AltLife (wired the wrong story node into the history toggle, never
applied a background fill, and had a back/forward wrapper that competed with
the dialog's internal cursor).

## Engine changes (`com.eb.javafx.ui.DialogEntriesView`)

1. **`setInternalNavigationEnabled(boolean)`** — gates the constructor-installed
   mouse-clicked handler and the scene-level `KEY_PRESSED` filter. Default
   `true` keeps existing demo/test behaviour; hosts that drive navigation from
   their own state machine (AltLife) can set it `false` so the dialog stops
   eating left/right clicks and Backspace/Space.
2. **`setHistoryClipsAtCursor(boolean)`** — opt-in flag. When `true`, history
   mode renders only `entries[0..currentIndex]`. Default `false` preserves the
   existing "history shows everything" behaviour the demo documents. The setter
   triggers an immediate `rebuild()` when the view is already in history mode
   so the change is visible without a navigation step.
3. **History-mode height-binding flicker fix.** The previous binding was
   `prefHeight = min(entriesContainer.height, centre.height)`. The dialog's
   height drove its ScrollPane viewport width, which changed Label wrapping,
   which changed the content's natural height, which fed back through the
   binding — the dialog oscillated between two sizes. Replaced with
   `prefHeight = centre.height` so the dialog always claims the full available
   space in history mode (matching the documented "fills the viewport" UX).
4. **Scroll-to-bottom double-deferral.** `rebuild()` previously used a single
   `Platform.runLater(() -> setVvalue(getVmax()))` to pin the scroll position
   after a content change. The deferred call fired before the JavaFX layout
   pulse committed the new content height, so the ScrollPane pinned to the
   *old* bottom — the newest entry appeared half-visible until the user
   scrolled by hand. Wrapped in a second `runLater` so the pin happens on the
   tick after layout completes, when `getVmax()` reflects the new content
   size.
5. Dropped the now-unused `Bindings` import.

## Engine tests (`DialogEntriesViewTest`)

Four new headless tests:

- `disablingInternalNavigationMakesLeftRightClicksNoOps`
- `reenablingInternalNavigationRestoresClickHandling`
- `historyClipsAtCursorRendersOnlyEntriesUpToAndIncludingCursor`
- `historyClipsAtCursorFlagTakesEffectMidHistoryMode`
- `historyModeHeightBindsToCentreHeightNotContentHeight`

## AltLife changes

### `AltLifeMainAppLayout.buildGameplayRoot` / `buildRoot`

- Walks the rendered layout to find the engine-built
  `layout-main-app-story-area` wrapper StackPane and passes that to
  `dialog.bindHistoryToggle(...)` instead of the inner story `VBox`. Hiding
  only the inner VBox left the wrapping ScrollPane + StackPane managed, so
  history mode appeared to "leave a block above the dialog" — the dialog
  could not reclaim the space above it. Falls back to the inner VBox if the
  wrapper isn't found.
- Calls `dialog.setInternalNavigationEnabled(false)` so the dialog's own click
  and key handlers stop competing with AltLife's footer-driven navigation.
- Calls `dialog.setHistoryClipsAtCursor(true)` so the history view never
  reveals lines the player hasn't reached yet.
- Applies a solid black, 50% opacity background to the dialog block (outer
  chrome + the ScrollPane's `.viewport`) via inline style. Skin-loaded
  viewport gets the same style applied on `skinProperty` change so the fill
  survives skin recreation. Padding stays driven by `dialogInsets` from the
  layout JSON.

### `AltLifeSceneFlowScreen.createMainAppLayoutScene`

- Removed the back/forward wrappers that pre-checked `dialog.canGoBack()` /
  `dialog.canGoForward()`. After `syncDialogEntries` rebuilds the dialog from
  `stepHistory`, the cursor is always parked at the last entry — so
  `canGoForward()` was always `false` (left-click became a permanent no-op)
  and `canGoBack()` walked the dialog cursor in a way that drifted out of
  sync with the scene-step state. The actions now route straight to
  `handleBack` / `goForward`, and `syncDialogEntries` is the single source
  of truth for what the dialog shows.

## Validation

- `./gradlew --no-daemon compileJava compileTestJava` — clean.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest`
  — passing.
- AltLife composite-build compile against this worktree
  (`-PnovlfxEngineCompositeDir=...`) — clean.

## Notes

- AltLife pins the engine to commit `73c05a4…` via JitPack in
  `AltLife/javafx/build.gradle`. After this engine change ships, AltLife's
  `novlfxEngineRevision` must be bumped to a commit that includes
  `DialogEntriesView.setInternalNavigationEnabled` and
  `setHistoryClipsAtCursor` for the AltLife-side fixes to compile.
- The demo (`DialogBlockTestScreenApplication`) is unchanged. It still uses
  the engine defaults (internal navigation on, history shows everything),
  matching the behaviour its javadoc describes.
