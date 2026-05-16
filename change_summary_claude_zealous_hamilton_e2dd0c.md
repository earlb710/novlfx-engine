# Change summary

Reworked the dialog block of `MAIN_APP_LAYOUT` so it dominates half of the
central frame and renders entries with a column-aligned, role-coloured layout.

## Layout

- `MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO`: `0.875` → `0.5`. Story and
  dialog now split the central frame evenly by default; existing screen JSON
  that already sets `storyDialogRatio` is unaffected.
- Sample design description in
  `examples/resources/json/screens/main-app-layout-test-screen.json` updated to
  reflect the new default ratio.

## Dialog block visuals

`src/main/resources/com/eb/javafx/ui/default.css`:

- New `.layout-main-app-dialog` rule paints a 50% black background on the
  dialog slot wrapper produced by `MainAppLayoutRenderer`.
- `.dialog-entry` base font is bumped from 14px to 20px and default fill is
  bright white.
- `.dialog-entry-current` makes the newest visible entry bright white, bold,
  and full-size; `.dialog-entry-previous` shrinks earlier entries to 14px
  (combined with the existing 0.5 opacity applied programmatically).
- Bold/italic for shout/whisper now come from descendant selectors
  (`.dialog-entry-shout .dialog-entry-body`,
  `.dialog-entry-whisper .dialog-entry-body`) so the body label can be styled
  uniformly.

## Dialog entry rendering

`src/main/java/com/eb/javafx/ui/DialogEntriesView.java`:

- `SpokenEntry` now renders as an `HBox` containing a fixed-width speaker
  `Label` column (160px) plus a wrapping body `Label`. The fixed-width column
  guarantees message bodies line up across consecutive entries even when
  speaker names differ in length; the wrapping body supports multi-line
  messages.
- The speaker label text is `"{speaker.label()}:"` (no trailing space — the
  `HBox` spacing provides the gap).
- When `DialogSpeaker.textColor()` is set, the colour is applied as an inline
  `-fx-text-fill` style to *both* the speaker label and the body label, so a
  different colour per role (narrator, MC, book girl, random girl, …) can be
  driven entirely through the speaker model. Inline style beats class CSS, so
  the role colour wins over `.dialog-entry-current` / `.dialog-entry-previous`
  defaults.
- New public constant `BODY_STYLE_CLASS = "dialog-entry-body"` carried by the
  body label so application stylesheets can target it directly.
- Removed direct `Text`/`TextFlow`/`Font`/`FontPosture`/`FontWeight`/`Color`
  usage from the renderer; bold/italic now come from CSS.

## Tests

`src/test/java/com/eb/javafx/ui/DialogEntriesViewTest.java`:

- Existing TextFlow-based assertions rewritten to verify the new HBox layout
  (speaker `Label` + body `Label`, both wrap-aware).
- Added `speakerWithTextColorTintsBothSpeakerAndBody` to lock in that
  `DialogSpeaker.textColor()` propagates to both labels.
- Added `speakerWithoutTextColorLeavesInlineStyleEmpty` so unstyled speakers
  fall back to the CSS defaults.
- Added `speakerColumnHasFixedWidthSoMessagesAlign` to lock in the speaker
  column width contract.

## Documentation

`docs/USER_MANUAL.md`:

- Default story/dialog ratio mention updated to `0.5`.
- Dialog entries widget section rewritten to describe the HBox layout,
  multi-line bodies, role colour propagation, larger current-entry font, and
  faded/smaller previous entries.
- Style-hook table extended with `.dialog-entry-body` and
  `.layout-main-app-dialog`.
- Added a code snippet showing four role speakers (narrator, MC, book girl,
  random girl) with distinct hex colours.

## Validation

- `./gradlew --no-daemon compileJava testClasses` — OK.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest --tests com.eb.javafx.ui.MainAppLayoutPlanTest --tests com.eb.javafx.ui.MainAppLayoutSampleJsonTest` — OK.
- No manual UI run was performed; the dialog block layout is verified through
  unit tests and CSS / structural assertions only.

---

# Change summary — dialog block scrollbar

Added a right-hand vertical scrollbar to the dialog block so players can scroll
back through previous conversations.

## Widget refactor

`src/main/java/com/eb/javafx/ui/DialogEntriesView.java`:

- Class now `extends ScrollPane` instead of `VBox`. An inner
  `entriesContainer` (`VBox`) holds the rendered entry nodes and is set as the
  ScrollPane's content.
- New public method `entryNodes()` exposes
  `entriesContainer.getChildrenUnmodifiable()` so tests and renderers can still
  inspect the rendered entries (the inherited `ScrollPane.getChildren()` is
  protected and returns skin children, not entries).
- New public constant `ENTRIES_CONTAINER_STYLE_CLASS =
  "dialog-entries-container"` carried by the inner container.
- ScrollPane configuration: `setFitToWidth(true)`, `setFitToHeight(false)`,
  `hbarPolicy = NEVER`, `vbarPolicy = AS_NEEDED`. The horizontal bar is
  suppressed since long messages already wrap inside the dialog width; the
  vertical bar appears only when history overflows the viewport.
- `DEFAULT_MAX_VISIBLE_ENTRIES` bumped from `5` to `Integer.MAX_VALUE` so by
  default every history entry is rendered and reachable via the scrollbar.
  Apps can still cap rendering for very long histories via
  `setMaxVisibleEntries(int)`.
- `rebuild()` now manipulates `entriesContainer.getChildren()` and finishes
  with `setVvalue(getVmax())` so the viewport snaps to the bottom after every
  append. The user can still scroll up through older conversations; the snap
  only fires on rebuild (i.e. on add / nav / clear), not on free scrolling.
- Mouse click handler stays on `this` (the ScrollPane) so the existing
  primary-advance / secondary-rewind behaviour survives — and tests that
  fire synthetic events on `view` keep working.

## CSS

`src/main/resources/com/eb/javafx/ui/default.css`:

- `.dialog-entries-view` now defines the dialog block's own 50% black
  background, transparent JavaFX ScrollPane `-fx-background`, zero padding,
  and transparent border — so the widget shows the 50% black even when it
  lives outside the main-app-layout slot.
- `.dialog-entries-view > .viewport` and `.dialog-entries-view > .corner` are
  forced transparent so the underlying 50% black surfaces through the
  ScrollPane skin.
- New `.dialog-entries-container` rule carries the entries' alignment,
  spacing, and 8×12 inner padding (previously lived on the outer view).
- New slim, dark-theme scrollbar styling:
  - `.dialog-entries-view > .scroll-bar:vertical` — 10px wide, transparent
    background.
  - Track painted at 5% white; thumb at 50% accent-cyan, ramping to 80% on
    hover/press.
  - Increment/decrement buttons and their arrows zeroed so the bar reads as a
    minimal stripe rather than a stock scroll control.

## Tests

`src/test/java/com/eb/javafx/ui/DialogEntriesViewTest.java`:

- All `view.getChildren()` references switched to `view.entryNodes()` — the
  inherited ScrollPane `getChildren()` is not publicly callable on the widget.
- Added `viewIsAScrollPaneWithRightHandVerticalScrollbar` to lock in the
  ScrollPane inheritance, the `AS_NEEDED` / `NEVER` policies, and
  `fitToWidth=true`.
- Added `unboundedDefaultLetsAllEntriesRenderIntoTheScrollableContainer` to
  confirm 50 entries all render (no maxVisibleEntries cap by default).
- Added `rebuildScrollsViewportToBottomToKeepCurrentEntryVisible` to lock in
  the auto-scroll-to-bottom behaviour after append.

## Documentation

`docs/USER_MANUAL.md`:

- Dialog entries widget section rewritten to describe ScrollPane inheritance,
  right-hand scrollbar, auto-scroll-to-bottom-on-append behaviour, and the new
  `entryNodes()` accessor for callers that need the rendered entry list.
- Style-hook table extended with `.dialog-entries-container`; the
  `.dialog-entries-view` row updated to note it carries the dialog background
  and scrollbar styling.

## Validation

- `./gradlew --no-daemon compileJava testClasses` — OK.
- `./gradlew --no-daemon test --tests "com.eb.javafx.ui.*"` — OK (all
  ui-package tests pass, including the new scroll-related ones).

---

# Change summary — dialog block manual demo

Added a runnable manual demo that shows the dialog block with ten conversations
between five characters so the column-aligned speaker layout, per-role
tinting, 50% black background, and right-hand scrollbar can all be eyeballed
in one launch.

## New manual launcher

`src/test/java/com/eb/javafx/testscreen/DialogBlockTestScreenApplication.java`:

- JavaFX `Application` that wires up a `MAIN_APP_LAYOUT` with the default 0.5
  story/dialog ratio, a transparent story slot showing a placeholder label,
  and a `DialogEntriesView` in the dialog slot.
- Builds five `DialogSpeaker`s — narrator (`#a0b0c0`), MC / Hero (`#88ddff`),
  book girl / Sarah (`#ffaaff`), random girl / Stranger (`#aaffaa`), old
  mentor (`#ddcc88`) — so the per-role colour rendering is exercised.
- `seedTenConversations(...)` pre-fills the dialog with exactly ten short
  conversations covering: narrator scene-set, MC meeting Sarah, narrator
  interlude, MC bumping into Stranger, the two girls crossing paths, MC
  internal monologue voiced by the narrator, three-way meeting, quiet moment
  with Sarah, confrontation with Stranger, mentor closing scene. Lines use a
  mix of `say` / `shout` / `whisper` so the line-type styling (bold uppercase
  / italic lowercase) also gets rendered.
- Each conversation uses a distinct `GameDateTime` so the dividers show
  meaningful progressing timestamps (day 1 morning → day 2 noon).
- Installs the dialog's keyboard shortcuts on the scene so
  <kbd>Space</kbd> / <kbd>Backspace</kbd> walk the cursor.

`build.gradle`:

- New `runDialogBlockTestScreen` `JavaExec` task in the `application` group.

## Smoke test

`src/test/java/com/eb/javafx/testscreen/DialogBlockTestScreenApplicationTest.java`:

- Headless JUnit test that initializes the JavaFX toolkit, reflectively
  invokes `seedTenConversations`, and asserts:
  - Exactly 10 conversations exist in the dialog history.
  - Every conversation is closed (`endedAt != null`) and has at least one
    mirrored message.
  - At least 3 distinct speaker ids are used (so the role palette is actually
    exercised).
  - The visible view contains 10 `ConversationStart` dividers, 10
    `ConversationEnd` dividers, and ≥20 spoken entries.

## Documentation

- `docs/USER_MANUAL.md` — added a "Launch the dialog block demo" section with
  the `runDialogBlockTestScreen` command and a brief description of what the
  window shows.
- `CLAUDE.md` — appended `runDialogBlockTestScreen` to the manual launcher
  table.

## Validation

- `./gradlew --no-daemon compileTestJava` — OK.
- `./gradlew --no-daemon test --tests com.eb.javafx.testscreen.DialogBlockTestScreenApplicationTest --tests com.eb.javafx.ui.DialogEntriesViewTest` — OK.
- The actual JavaFX window was not opened from this session (the demo is a
  manual launcher); the smoke test exercises the seed logic headlessly.

---

# Change summary — surface dialog block demo in `runTestScreen`

Wired the dialog block demo into the manual test screen
(`TestScreenApplication`, launched by `./gradlew runTestScreen`) so it shows
up in the example tree and can be launched with the "Run" button — not only
through its own Gradle task.

## Standalone discoverable shim

`examples/user-manual/06-ui-screens-and-themes/DialogBlockDemo.java`:

- New default-package file with a `public static void main(String[] args)` so
  it satisfies the test app's standalone-example discovery filter
  (`STANDALONE_JAVA_MAIN_PATTERN`). The main delegates to
  `com.eb.javafx.testscreen.DialogBlockTestScreenApplication.main(args)`,
  which the test runtime classpath provides — so the discoverable example
  and the `runDialogBlockTestScreen` Gradle task share one implementation
  rather than duplicating the seed logic.

## Test

`src/test/java/com/eb/javafx/testscreen/DialogBlockTestScreenApplicationTest.java`:

- New `testAppDiscoversDialogBlockDemoAsAStandaloneExample` test locks in
  three things:
  1. The shim file exists at the documented path.
  2. `TestScreenApplication.isStandaloneExampleFile(...)` classifies it as a
     runnable standalone example.
  3. The walk performed by
     `TestScreenApplication.standaloneExampleFiles(examplesRoot)` actually
     returns the shim, so the test app's tree will surface it.

## Documentation

`docs/USER_MANUAL.md`:

- Extended the "Launch the dialog block demo" section with a paragraph
  explaining that the same demo is also reachable from the manual test screen
  under `06-ui-screens-and-themes/DialogBlockDemo.java` — and that the shim
  delegates back to `DialogBlockTestScreenApplication` so both entry points
  share a single implementation.

## Validation

- `./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.*" --tests "com.eb.javafx.ui.DialogEntriesViewTest"` — OK (all
  `testscreen` package tests, including the new discovery test, pass).

---

# Change summary — dialog block opaque black background, fix theme wiring

The dialog-block demo was rendering as light text on a white background. Root
cause: `DialogEntriesView`'s CSS lives in the bundled
`src/main/resources/com/eb/javafx/ui/default.css`, but `UiTheme.stylesheet()`
returns a *generated* stylesheet built from `STYLESHEET_TEMPLATE` — and that
template did not contain any dialog block rules. Consumers like the demo only
load the generated stylesheet, so the dialog block fell back to JavaFX's
Modena defaults (white ScrollPane viewport) and the engine-provided light
label colour, giving the unreadable light-on-white result.

## Fix — dialog rules ship with every generated theme

`src/main/java/com/eb/javafx/ui/UiTheme.java`:

- Appended a block of dialog-specific CSS rules to `STYLESHEET_TEMPLATE`,
  using literal colours rather than palette placeholders so the dialog block
  stays readable on every palette. Background is **opaque black `#000000`**
  (per the user's request — drops the previous semi-transparent
  `rgba(0, 0, 0, 0.5)`); current entry is bright white at the larger font
  size; previous entries keep the smaller font (and the 0.5 opacity already
  applied programmatically); ScrollPane viewport, corner, and scrollbar
  styling are forced transparent / slim so the black surfaces cleanly.
- Reworded a doc comment that contained "50% opacity" — a literal `%` in a
  Java text block fed to `String.formatted(...)` is parsed as a format
  specifier and raised `MissingFormatArgumentException`. Now reads "half
  opacity".

## Fix — contract stylesheet stays in sync

`src/main/resources/com/eb/javafx/ui/default.css`:

- `.layout-main-app-dialog` and `.dialog-entries-view` backgrounds switched
  from `rgba(0, 0, 0, 0.5)` to `#000000` so the bundled contract stylesheet
  matches the runtime theme.

## Test

`src/test/java/com/eb/javafx/ui/UiThemeTest.java`:

- New `generatedStylesheetIncludesDialogBlockRulesWithOpaqueBlackBackground`
  test asserts that the runtime stylesheet returned by
  `UiTheme.stylesheetContent()` contains the dialog block style classes
  (`.dialog-entries-view`, `.layout-main-app-dialog`, viewport rule, current
  entry rule) AND that the background colour is opaque `#000000`. This pins
  the wiring so the next time someone refactors the theme template, the
  demo's readability is locked in.

## Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.ui.UiThemeTest --tests com.eb.javafx.ui.DialogEntriesViewTest --tests com.eb.javafx.ui.ScreenShellTest --tests com.eb.javafx.testscreen.DialogBlockTestScreenApplicationTest` — OK.
- The window itself was not re-launched from this session; the new
  `UiThemeTest` assertion is the headless evidence that the rules are now
  baked into every theme variant.

---

# Change summary — drop end divider, auto-skip start divider, multi-line demo messages

The dialog block now treats the start of a new conversation as the only
visual separator, and cursor navigation auto-skips dividers so the player
never has to "click through" them. The demo also exercises multi-line
messages now — both explicit `\n` line breaks and naturally-wrapping long
sentences.

## Widget contract

`src/main/java/com/eb/javafx/ui/DialogEntriesView.java`:

- `endConversation(...)` no longer appends a `ConversationEnd` divider entry
  to the visible list. The underlying `DialogHistory` still records
  `endedAt`, so persistence/history-screens keep their full lifecycle
  metadata — only the visible closing divider goes away. The
  `ConversationEnd` record itself is kept for backward compatibility; apps
  that want a custom closing marker can still construct and append one.
- `goBack()` / `goForward()` auto-skip any `ConversationStart` /
  `ConversationEnd` entry so the cursor only ever lands on a spoken or plain
  line. The dividers remain rendered above the cursor as section headers but
  are not reading positions.
- New private helpers `previousNonDividerIndex(int)`,
  `nextNonDividerIndex(int)`, and `isDivider(Entry)` underpin the skip
  behaviour.
- `canGoBackProperty()` / `canGoForwardProperty()` now reflect "is there a
  non-divider entry in that direction?" rather than the raw index distance,
  so footer back/forward affordances correctly grey out when only dividers
  stand between the cursor and the end of the list.

## Tests

`src/test/java/com/eb/javafx/ui/DialogEntriesViewTest.java`:

- Renamed `endConversationClosesHistoryAndAppendsEndDivider` →
  `endConversationClosesHistoryWithoutAppendingEndDivider`; now asserts no
  `ConversationEnd` is appended, the entry count is unchanged, the cursor
  stays on the last `SpokenEntry`, and the underlying history still closes.
- Renamed `dialogEntriesReturnsAllEntryKindsIncludingDividers` →
  `dialogEntriesReturnsStartDividerAndSpokenButNoEndDivider`; now expects
  exactly two entries (start + spoken).
- New `goBackSkipsConversationStartDividerToLandOnPreviousSpokenEntry` and
  `goForwardSkipsConversationStartDividerToLandOnNextSpokenEntry` lock in
  the auto-skip behaviour.
- New `canGoBackAndCanGoForwardIgnoreLeadingTrailingDividers` confirms the
  navigation flags treat divider-only neighbours as "nothing to go to".

`src/test/java/com/eb/javafx/testscreen/DialogBlockTestScreenApplicationTest.java`:

- Renamed `demoSeedsBothDividerEntriesAndSpokenEntriesIntoTheVisibleView` →
  `demoSeedsStartDividersAndSpokenEntriesButNoEndDividers`; asserts 10 start
  dividers, 0 end dividers, ≥20 spoken entries.
- New `demoIncludesAtLeastOneExplicitMultilineMessage` asserts the demo
  contains both an explicit `\n` multi-line message and a long line (≥120
  characters) that will wrap naturally — covering both multi-line rendering
  modes the dialog block has to handle.

## Demo updates

`src/test/java/com/eb/javafx/testscreen/DialogBlockTestScreenApplication.java`:

- The `library-open` narrator scene-set now uses a long natural-wrap line
  followed by an explicit three-stanza `\n` line.
- The `interlude-1` noon descriptive line is now a single long
  natural-wrap sentence (~250 chars).
- The `monologue` evening conversation pairs an explicit `\n` three-stanza
  message with a long natural-wrap follow-up paragraph.
- Class-level Javadoc updated to mention the new cursor-skip behaviour and
  to point at the multi-line messages so a manual tester knows where to look.

## Docs

`docs/USER_MANUAL.md`:

- Dialog entries widget section: `ConversationEnd` description rewritten to
  say it's no longer emitted automatically. New paragraph describes the
  cursor's auto-skip behaviour over dividers and the matching `canGoBack` /
  `canGoForward` semantics.

## Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest --tests com.eb.javafx.testscreen.DialogBlockTestScreenApplicationTest` — OK (all dialog widget and demo tests pass, including the new skip + multi-line ones).

---

# Change summary — reusable ErrorScreen

Added a reusable, dark-red themed error/exception screen for surfacing
failures to the player. The screen ships with a copyable details block, a
`Continue` button that only appears when the caller signals the failure is
recoverable, and an always-present `Exit` button.

## Widget

`src/main/java/com/eb/javafx/ui/ErrorScreen.java` (new):

- Static factory: `ErrorScreen.createScene(Options, width, height)` returns a
  `Scene`; `ErrorScreen.buildRoot(Options)` returns only the `Parent` for
  callers that already own a `Scene`.
- `ErrorScreen.Options` is a record carrying `title`, `message`, `details`,
  `continueAction`, and `exitAction`:
  - `title` falls back to `ErrorScreen.DEFAULT_TITLE` ("Something went
    wrong") when null/blank, so the heading is always meaningful.
  - `message` is rendered as a short label above the details and is omitted
    when null/blank.
  - `details` is rendered into a read-only, selectable monospace `TextArea`
    (so the player can `Ctrl+A` / `Ctrl+C`); the dedicated **Copy details**
    button copies the whole body to the system clipboard in one click.
  - `continueAction` controls Continue-button visibility — `null` means
    fatal (no Continue button at all), non-null means recoverable.
  - `exitAction` is required.
- Two convenience factories: `Options.ofException(Throwable, exitAction)`
  builds a fatal-error screen; `Options.ofException(Throwable, continueAction,
  exitAction)` builds a recoverable one. Both populate the title from the
  exception class name, the message from `getMessage()`, and the details
  from a full stack trace via `ErrorScreen.stackTraceText(Throwable)`.

## CSS — dark red theme

Dark-red styling added to **both** the runtime theme template and the
contract reference (so the colours apply through `UiTheme.stylesheet()` AND
the bundled `default.css` is in sync):

- `src/main/java/com/eb/javafx/ui/UiTheme.java` — appended a block to
  `STYLESHEET_TEMPLATE` with literal hex colours so the error screen looks
  the same regardless of the active palette.
- `src/main/resources/com/eb/javafx/ui/default.css` — same rules appended at
  the bottom.

Palette:
- `.error-screen` — `#1a0a0a` panel, `#7f1d1d` 2px border, 8px radius.
- `.error-screen-title` — `#ff6b6b` light-red, 28px bold.
- `.error-screen-message` — `#fecaca` cream-red, 14px.
- `.error-screen-details` — `#2a1212` text area, `#fecaca` monospace text,
  `#7f1d1d` border.
- `.error-screen-copy-button` — transparent with dark-red border (subtle).
- `.error-screen-continue-button` — outlined in `#fecaca` (secondary).
- `.error-screen-exit-button` — filled `#7f1d1d` with `#c41e3a` border
  (primary emphasis), hover/press cycle through `#c41e3a` → `#ff6b6b`.

## Tests

`src/test/java/com/eb/javafx/ui/ErrorScreenTest.java` (new):

- 13 headless tests covering: option validation (`exitAction` required),
  `details` null-normalisation, `effectiveTitle()` fallback, the
  `continueAvailable()` flag, structural rendering of title / message /
  details / actions for both fatal and recoverable error options, default
  title injection, Continue-button absence when no continueAction is
  supplied, action wiring (clicks invoke the right `Runnable`),
  `ofException` populating title + message + stack-trace details,
  `stackTraceText` containing class name + message, and `createScene`
  returning a `Scene` of the requested dimensions with the dark-red root.

## Manual demo

`src/test/java/com/eb/javafx/testscreen/ErrorScreenTestApplication.java`
(new):

- JavaFX `Application` that builds a synthetic `IllegalStateException`
  through three nested `failureLayer*` calls so the details `TextArea` has a
  real-looking stack trace to display, then opens an `ErrorScreen` Scene
  with both Continue and Exit actions wired up — Continue closes the demo
  window, Exit quits the JavaFX runtime.

`build.gradle`:

- New `runErrorScreenTestScreen` `JavaExec` task in the `application` group.

`examples/user-manual/06-ui-screens-and-themes/ErrorScreenDemo.java` (new):

- Default-package shim with a `main` method so the manual test screen
  (`runTestScreen`) discovers and lists it as a runnable example. Delegates
  to `ErrorScreenTestApplication.main(args)`.

## Documentation

- `docs/USER_MANUAL.md` — new "Launch the reusable error screen demo"
  paragraph next to the other demo launchers, and a full **Error screen
  (error and exception surface)** subsection with the Options field table,
  API entry-point table, two wiring examples (fatal startup error and
  recoverable failure), and the style-hook table.
- `CLAUDE.md` — appended `runErrorScreenTestScreen` to the manual launcher
  table.

## Validation

- `./gradlew --no-daemon compileJava testClasses` — OK.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.ErrorScreenTest --tests com.eb.javafx.ui.UiThemeTest --tests com.eb.javafx.ui.ScreenShellTest` — OK (all 13 new ErrorScreen tests plus the existing theme/shell tests pass).

## Speaker column right-alignment

- `DialogEntriesView.renderSpoken()` — `speakerLabel.setAlignment(Pos.TOP_LEFT)` →
  `Pos.TOP_RIGHT`. The speaker name and colon now sit flush against the body text
  rather than pushing away from it.
- `UiTheme.STYLESHEET_TEMPLATE` — `.dialog-entry-speaker` rule extended with
  `-fx-text-alignment: right; -fx-alignment: top-right;` so the CSS contract
  matches the Java-set property.
- `default.css` — same two declarations added to `.dialog-entry-speaker`.
- `DialogEntriesViewTest.speakerColumnHasFixedWidthSoMessagesAlign` — two new
  assertions verify both speaker labels have `Pos.TOP_RIGHT` alignment.
  Added `import javafx.geometry.Pos;`.
- `docs/USER_MANUAL.md` — `SpokenEntry` description updated to call out the
  right-aligned speaker column; `.dialog-entry-speaker` style table entry
  updated accordingly.

## Validation

- `./gradlew --no-daemon testClasses` — OK.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest` — OK.

## Footer added to dialog block test screen

### `DialogEntriesView` — `bindToFooter` enabled-state sync

`bindToFooter(Node)` previously only installed click handlers on the footer's
back/forward labels; the labels never showed a greyed-out state even when the
cursor was at the first or last entry.

Changes:
- For "back" and "forward" labels, immediately calls the new
  `applyFooterLabelEnabled(Label, boolean)` helper with the current
  `canGoBack()`/`canGoForward()` value.
- Adds `canGoBack` / `canGoForward` property listeners so the label's disabled
  CSS class and its stored `FooterOption.enabled()` flag update automatically
  every time the cursor moves.
- New private static `applyFooterLabelEnabled(Label, boolean)` toggles
  `.screen-footer-option-disabled` on the label and keeps `getUserData()`
  in sync so `ScreenShell.isFooterOptionEnabled(label)` stays consistent.
- Expanded Javadoc on `bindToFooter` to document the new automatic
  enabled/disabled sync.

### `DialogBlockTestScreenApplication`

- `showFooter` changed from `false` → `true` in the `MainAppLayoutPlan`.
- `dialog.bindToFooter(root)` called immediately after `MainAppLayoutRenderer.render`
  so the footer's ‹ / › labels are live before the scene is created.
- Javadoc updated to mention the footer bar controls and their auto-greyed state.

### `DialogEntriesViewTest`

- New test `bindToFooterSyncsEnabledStateImmediatelyAndOnNavigation` verifies:
  - Back starts enabled and forward starts disabled when cursor opens on the last entry.
  - After `goBack()` to the first entry, back becomes disabled and forward becomes enabled.

## Validation

- `./gradlew --no-daemon testClasses` — OK.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest` — OK
  (all existing tests plus the new sync test pass).

## Scrollbar bottom-pin, history-mode toggle, and conversation cap

### 1. Scrollbar always snaps to bottom on navigation

`DialogEntriesView.rebuild()`: `setVvalue(getVmax())` was called synchronously,
before the JavaFX layout pass had committed the new content height, so the
scroll fell short on cursor navigation. Changed to `Platform.runLater(() ->
setVvalue(getVmax()))` so the pin fires after the layout pass and correctly
reaches the bottom every time.

Added `import javafx.application.Platform` and `import javafx.beans.binding.DoubleBinding`
to `DialogEntriesView`.

Existing test `rebuildScrollsViewportToBottomToKeepCurrentEntryVisible` updated
to flush the FX event queue via a sentinel `CountDownLatch` before asserting the
scroll position (the deferred model makes the synchronous assertion stale).

### 2. History (◷) footer button expands dialog to full height

#### `DialogEntriesView`

New fields:
- `private boolean historyMode` — when true, `rebuild()` renders ALL entries
  (including those after the cursor) so the complete conversation record is visible.
- `private double savedDialogHeightShare` — remembered ratio used to restore the
  height binding when exiting history mode.

New public API:
- `isHistoryMode()` / `setHistoryMode(boolean)` — toggle history mode; `rebuild()`
  responds by rendering all entries vs. cursor-bounded entries.
- `bindHistoryToggle(Node footerOrAncestor, Node storyNode, double dialogHeightShare)` —
  wires the footer ◷ button to call `toggleHistoryLayout(storyNode)` on each click.
  Saves `dialogHeightShare` for binding restoration.

New private helper:
- `toggleHistoryLayout(Node storyNode)` — on enter: unbinds pref/min/max height,
  calls `centre.setCenter(this)` to move the dialog from `BorderPane.bottom` to
  `BorderPane.center` (storyNode is displaced from center; the caller's reference keeps it
  alive). On exit: calls `centre.setBottom(this)`, restores storyNode to center,
  and rebinds height to `centre.heightProperty().multiply(savedDialogHeightShare)`.

Added `import javafx.scene.layout.BorderPane` to `DialogEntriesView`.

#### `DialogBlockTestScreenApplication`

After `bindToFooter(root)`:
```java
double dialogHeightShare = 1.0 - MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO;
dialog.bindHistoryToggle(root, storyArea, dialogHeightShare);
```
Javadoc updated to describe the ◷ toggle and the scrollbar behaviour.

### 3. `DialogHistory` capped at 1 000 conversations

New public constant `DialogHistory.MAX_CONVERSATIONS = 1000`.

`beginDialog()` now trims `entries.remove(0)` when `entries.size() > MAX_CONVERSATIONS`
after adding the new entry, making the list a sliding window of the most recent
1 000 conversations. `openEntryIndex` is reset to `entries.size() - 1` afterwards,
so it always points to the new (last) entry correctly.

### Tests added

`DialogEntriesViewTest`:
- `historyModeShowsAllEntriesIncludingThoseAfterCursor` — with cursor at index 1 of
  3 entries and historyMode=true, all 3 nodes are rendered; index-1 is "current",
  0 and 2 are "previous".
- `historyModeOffRestoresNormalCursorBoundRendering` — toggling mode off restores
  the cursor-bounded view.
- `bindHistoryToggleWiresHistoryButtonToToggleMode` — clicking the ◷ label sets
  `isHistoryMode()` true; clicking again resets to false.

`DialogHistoryTest`:
- `beginDialogTrimsOldestConversationWhenOverMaxLimit` — after exactly MAX+1
  conversations, size stays at MAX and "dialog-0" is evicted.
- `beginDialogCanContinueAfterTrimWithoutCorruptingIndex` — after MAX+5 trims,
  the open/close cycle and message append still work correctly.

## Validation

- `./gradlew --no-daemon testClasses` — OK.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest --tests com.eb.javafx.text.DialogHistoryTest` — OK (all new + existing tests pass).

## History-mode toggle bug fixes

### Problem 1 — Dialog block disappeared on history-mode close

The previous `toggleHistoryLayout` moved the `DialogEntriesView` node between
`BorderPane.bottom` and `BorderPane.center`. The close sequence called
`centre.setBottom(this)` while `this` was still the `centre.center` child, causing a
duplicate-children conflict that lost the node from the layout.

**Fix**: Removed all node-moving. The dialog stays in `BorderPane.bottom` at all
times. History mode now works by toggling the story node visibility and rebinding
the dialog height:
- Enter: `storyNode.setManaged(false); storyNode.setVisible(false)` collapses the center
  slot; dialog height bound to `min(entriesContainer.getHeight(), centre.getHeight())`
- Exit: story node restored; dialog height rebound to `centre.height * savedShare`

Added early-return guard so the toggle still works in test contexts where the view
has no BorderPane parent (mode flag still flips; only layout side-effects are skipped).

### Problem 2 — History view always filled full window height

**Fix**: `createDoubleBinding(() -> Math.min(contentHeight, centreHeight), ...)` makes
short histories expand only as far as the rendered content needs, while long histories
fill the full available height. `Bindings.min` was tried first but returns `NumberBinding`
(not `DoubleBinding`) in JavaFX 17's module API; replaced with `createDoubleBinding`.

Added `import javafx.beans.binding.Bindings;` to `DialogEntriesView`.

### New test

`bindHistoryToggleHidesAndRestoresStoryNodeWhenEmbeddedInBorderPane` — creates a
`BorderPane` with the view as bottom and a storyNode as center, then verifies the
full hide/restore cycle and that the view remains a child of the pane throughout.

## Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest` — OK.
