# Change Summary

## Dialog entries widget for `MAIN_APP_LAYOUT` dialog slot

Added `DialogEntriesView`, a reusable JavaFX widget the engine can plug into the dialog area of a
`MAIN_APP_LAYOUT` (via `MainAppScreenResolver`). The widget displays a stack of dialog entries with
the **newest entry at the bottom at full opacity** and **previous entries stacked above it at 50%
opacity** so the player sees the active line clearly with recent context fading away. The standard
footer's back / forward labels (and the matching `Backspace` / `Space` shortcuts) drive navigation
through the entries.

### New widget: `com.eb.javafx.ui.DialogEntriesView` (extends `VBox`)

State:
- Internal `List<String> entries` and a cursor pointing at the newest visible entry.
- `ReadOnlyBooleanProperty canGoBack` / `canGoForward` for greying out footer affordances.

API:
- `addEntry(String)` — append a new line and move the cursor to it (becomes the newest visible).
- `setEntries(Collection<String>)` — replace the history and park the cursor at the last entry.
- `goBack()` / `goForward()` — move the cursor one step into the past / toward the latest; both
  clamp at the ends.
- `clear()` — empty the history and reset the cursor.
- `setMaxVisibleEntries(int)` — cap how many entries (current + previous) render at once. Default
  is 5; older entries stay in the history but are hidden until the cursor returns to them.
- `bindToFooter(Node)` — wire the standard `ScreenShell.footerBar()` back / forward labels to drive
  `goBack` / `goForward`. Accepts either the footer `HBox` itself or any ancestor that contains it
  (so callers can hand it the `MainAppLayoutRenderer` root directly); a recursive scan finds the
  footer by its `screen-footer-bar` style class. Click handlers respect the footer option's
  enabled state via `ScreenShell.isFooterOptionEnabled(...)`.
- `installKeyboardShortcuts(Scene)` — install `Backspace` / `Space` event filters that mirror the
  footer back / forward shortcuts.

Rendering: `rebuild()` clears the `VBox` and re-adds visible entry labels in chronological order
(oldest first, newest at the bottom). Previous entries get style class
`dialog-entry-previous` plus `setOpacity(0.5)`; the current entry gets style class
`dialog-entry-current` and stays at full opacity. Labels use `setWrapText(true)` and
`setMaxWidth(Double.MAX_VALUE)` so dialog lines wrap to the slot width.

### Style hooks (in `default.css`)

| Style class | Applied to |
|---|---|
| `.dialog-entries-view` | The `VBox` container — `-fx-alignment: bottom-left`, `-fx-spacing: 4px` |
| `.dialog-entry` | Every entry label — `-fx-text-fill: #dbeafe`, `-fx-font-size: 14px`, `-fx-wrap-text: true` |
| `.dialog-entry-current` | The newest visible (bottom) entry — `-fx-font-weight: bold` |
| `.dialog-entry-previous` | Earlier entries (faded) |

### Tests

- **`DialogEntriesViewTest`** — 12 tests covering: empty view, append + cursor, three-entry layout
  with full opacity on current and `PREVIOUS_ENTRY_OPACITY` on previous, `goBack` and `goForward`
  cursor moves with redraw, clamping at both ends, `setMaxVisibleEntries` capping, `setEntries`
  replacement, `clear`, null-text rejection, footer binding driving navigation through click
  events, and footer binding accepting an ancestor container. Tests initialize the JavaFX toolkit
  once via `Platform.startup` (the same pattern used by `ScreenShellTest`) because `Label` extends
  `Control`, which has a static initializer that requires the toolkit.

### Documentation

- **`docs/USER_MANUAL.md`** — new "Dialog entries widget" subsection under the existing "Main app
  layout" section, documenting the API, a typical wiring snippet using `bindToFooter(root)` and
  `installKeyboardShortcuts(scene)`, and the four style hooks.

### Files changed

- **`src/main/java/com/eb/javafx/ui/DialogEntriesView.java`** — new widget (added).
- **`src/main/resources/com/eb/javafx/ui/default.css`** — `.dialog-entries-view`, `.dialog-entry`,
  `.dialog-entry-current` style rules added.
- **`src/test/java/com/eb/javafx/ui/DialogEntriesViewTest.java`** — new test class.
- **`docs/USER_MANUAL.md`** — new "Dialog entries widget" subsection inside "Main app layout".

### Validation

- `./gradlew --no-daemon compileJava` — `BUILD SUCCESSFUL`.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest` — `BUILD SUCCESSFUL`,
  12 tests pass.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.MainAppLayoutPlanTest --tests com.eb.javafx.ui.MainAppLayoutSampleJsonTest --tests com.eb.javafx.ui.ScreenLayoutRendererTest --tests com.eb.javafx.ui.ScreenShellTest`
  — `BUILD SUCCESSFUL`, no regressions in the adjacent layout / shell tests.

---

## Dialog helpers: `say` / `shout` / `whisper` / conversation lifecycle

Promoted `DialogEntriesView` from a bare list-of-strings widget into a fully-featured dialog
surface. The widget now exposes a sealed `Entry` model, owns a `DialogHistory`, renders rich text
via `TextFlow`, and offers verb-style helpers that mirror writes into both the visible stack and
the persisted history.

### Sealed `Entry` model (nested public types)

- `PlainEntry(text)` — bare narration string. Rendered as a `Label` (unchanged backward-compatible
  shape used by `addEntry(String)`).
- `SpokenEntry(LineType type, DialogSpeaker speaker, String text)` — say / shout / whisper line
  with optional speaker. Body is transformed per `LineType` (`SHOUT` → uppercase, `WHISPER` →
  lowercase, `SAY` / `CHOICE` → unchanged). Rendered as a `TextFlow`: speaker prefix uses
  `DialogSpeaker.textColor()` when supplied; shout body is bold; whisper body is italic; say body
  is plain.
- `ConversationStart(List<DialogSpeaker> participants, GameDateTime startedAt)` — divider entry
  inserted at the start of a conversation. Rendered as an `HBox` showing
  `── Conversation: Alice, Bob (day 2 morning) ──` with horizontal lines on either side.
- `ConversationEnd(GameDateTime endedAt)` — closing divider, also rendered as an `HBox`.

The new sealed `Entry` interface and its four record permits are exposed publicly so callers can
inspect the structured model via `dialogEntries()` (in addition to the existing `entries()` which
returns plain text representations).

### New verbs on `DialogEntriesView`

- `say(DialogSpeaker speaker, String text)` / `say(String text)`
- `shout(DialogSpeaker speaker, String text)` / `shout(String text)`
- `whisper(DialogSpeaker speaker, String text)` / `whisper(String text)`
- `startConversation(DialogSpeaker... participants)` (auto id + default `GameDateTime`)
- `startConversation(GameClock clock, DialogSpeaker... participants)`
- `startConversation(String dialogId, GameDateTime startedAt, DialogSpeaker... participants)`
- `endConversation()` / `endConversation(GameClock)` / `endConversation(GameDateTime)`

History side-effects:

- `startConversation(...)` calls `history.beginDialog(dialogId, startedAt)`.
- `say` / `shout` / `whisper` call `history.addMessage(DialogMessage.speakerMessage(speaker, formattedBody))`
  when a conversation is open AND a speaker is supplied; otherwise the history step is skipped and
  only the visible stack updates.
- `endConversation(...)` calls `history.endDialog(endedAt)` if a conversation is open; otherwise
  it's a no-op.

The body text persisted into `DialogHistory` is the **post-transform** body (uppercased for shout,
lowercased for whisper), so the saved log matches what the player saw on screen.

### Ownership of `DialogHistory`

- Default constructor `new DialogEntriesView()` allocates a fresh `DialogHistory`.
- New constructor `new DialogEntriesView(DialogHistory)` accepts an externally-owned history so
  callers can share it with save/load and the existing `ConversationHistoryScreen`.
- `history()` getter exposes the underlying `DialogHistory` for direct inspection / mutation.
- `clear()` only resets the visible stack and cursor; the `DialogHistory` keeps its accumulated
  entries (so the canonical log survives a screen reset).

### Rendering changes

- Switched from `Label`-only to mixed rendering: `PlainEntry` keeps the previous `Label` shape
  (existing tests untouched), but `SpokenEntry` renders as a `TextFlow` with two `Text` children
  (speaker + body) so bold/italic and per-speaker text colour are honoured inline. The previous
  `0.5` opacity treatment for non-current entries continues to apply at the entry-node level.
- Divider entries render as an `HBox` with a thin `Region` line on either side of a centred
  `Label` showing participants (start) or "End" (end).

### Style hooks added in `default.css`

| New class | Purpose |
|---|---|
| `.dialog-entry-speaker` | Speaker prefix `Text` inside a `SpokenEntry` |
| `.dialog-entry-say` | `SAY` body `Text` |
| `.dialog-entry-shout` | `SHOUT` body `Text` (default fill is the existing icon yellow) |
| `.dialog-entry-whisper` | `WHISPER` body `Text` |
| `.dialog-entry-divider` | Divider `HBox` |
| `.dialog-entry-divider-line` | The thin horizontal lines on either side of a divider |
| `.dialog-entry-divider-label` | The centred label inside a divider |

### Tests

`DialogEntriesViewTest` grew from 12 to 25 tests. New coverage:

- `sayRendersTextFlowWithSpeakerAndPlainBody`
- `shoutRendersBodyAsUppercase`
- `whisperRendersBodyAsLowercase`
- `sayWithoutSpeakerRendersBodyOnly`
- `spokenEntryExposesStructuredEntryThroughDialogEntries`
- `startConversationOpensDialogHistoryAndAppendsStartDivider`
- `sayDuringOpenConversationMirrorsIntoHistory` (asserts post-transform body lands in `DialogHistory`)
- `sayWithoutOpenConversationStillRendersButSkipsHistory`
- `endConversationClosesHistoryAndAppendsEndDivider`
- `endConversationIsNoOpWhenNoConversationOpen`
- `conversationDividersRenderAsHBoxWithDividerStyleClass`
- `widgetCanShareExternalDialogHistory`
- `dialogEntriesReturnsAllEntryKindsIncludingDividers`

The original 12 tests (plain `addEntry` flow, opacity, navigation, footer binding) continue to
pass because `PlainEntry` keeps the `Label` rendering shape they assert against.

### Documentation

`docs/USER_MANUAL.md` "Dialog entries widget" subsection rewritten to document the sealed `Entry`
model, the new verbs, history side-effects, and the expanded style-hook table.

### Files changed

- **`src/main/java/com/eb/javafx/ui/DialogEntriesView.java`** — rewritten: sealed `Entry` model;
  `DialogHistory` ownership; say/shout/whisper/start/end helpers; `TextFlow`-based rich-text
  rendering for `SpokenEntry`; `HBox` divider rendering for conversation markers; new public
  records `PlainEntry`, `SpokenEntry`, `ConversationStart`, `ConversationEnd`. Imports
  `ConversationDefinition.LineType` / `DialogHistory` / `DialogMessage` / `DialogSpeaker` /
  `GameClock` / `GameDateTime` from the engine.
- **`src/main/resources/com/eb/javafx/ui/default.css`** — added `.dialog-entry-speaker`,
  `.dialog-entry-say`, `.dialog-entry-shout`, `.dialog-entry-whisper`, `.dialog-entry-divider`,
  `.dialog-entry-divider-line`, `.dialog-entry-divider-label` rules.
- **`src/test/java/com/eb/javafx/ui/DialogEntriesViewTest.java`** — 13 new tests for the helpers
  and history side-effects; comparisons against `GameDateTime` use field-by-field assertions since
  `GameDateTime` doesn't override `equals`.
- **`docs/USER_MANUAL.md`** — rewrote the "Dialog entries widget" subsection.

### Validation

- `./gradlew --no-daemon compileJava` — `BUILD SUCCESSFUL`.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest` — `BUILD SUCCESSFUL`,
  25 tests pass.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest --tests com.eb.javafx.ui.MainAppLayoutPlanTest --tests com.eb.javafx.ui.MainAppLayoutSampleJsonTest --tests com.eb.javafx.ui.ScreenLayoutRendererTest --tests com.eb.javafx.ui.ScreenShellTest --tests com.eb.javafx.text.DialogHistoryTest --tests com.eb.javafx.scene.ConversationDefinitionJsonTest`
  — `BUILD SUCCESSFUL`, no regressions.
