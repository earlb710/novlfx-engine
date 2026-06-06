# Change summary

## Document `FooterController` in the user manual

The `com.eb.javafx.ui.FooterController` class (generic footer router) was previously undocumented —
the manual only covered the footer *concept* (the `ScreenShell.footerBar()` slot, footer styling
knobs, and the auto-wiring done by `MainAppLayoutRenderer` for `DialogEntriesView`), but not the
class itself, its `wireFooter` / `installKeyboardShortcuts` entry points, the public option-id
constants, or the `F5` / `F9` quick-save / quick-load shortcuts.

### Changes

- `docs/USER_MANUAL.md`: added a new `#### Footer control (FooterController)` subsection under
  section 6 (UI screens and themes), placed between the dialog-entries widget and the error-screen
  subsections. It documents:
  - the class's role and why it sits *above* `ScreenShell` (depends on `SceneRouter`, `SaveScreen`,
    `QuickSaveActions`);
  - a note pointing `MAIN_APP_LAYOUT` users back to the auto-wiring path;
  - both static methods (`wireFooter`, `installKeyboardShortcuts`) in a method table;
  - the option-id constant contract (`BACK_ID`, `FORWARD_ID`, `SKIP_MODE_ID`, `HISTORY_ID`,
    `SAVE_ID`, `LOAD_ID`, `PREFERENCES_ID`, `QUICK_SAVE_ID`) and which options are handled
    internally vs. via caller callbacks (incl. the null-history fallback to
    `CONVERSATION_HISTORY_ROUTE`);
  - a wiring code example using the public `ScreenShell.defaultFooterOptions()` accessor.
- `examples/user-manual/06-ui-screens-and-themes/FooterControlDemo.java`: new reference snippet
  mirroring the manual's wiring example (`wireFooter` + `installKeyboardShortcuts`). Not compiled by
  Gradle (consistent with the other `examples/user-manual` snippets).
- `examples/user-manual/README.md`: added the index row for `FooterControlDemo.java`.

### Notes

- Verified public API used in the docs/example: `ScreenShell.defaultFooterOptions()` is public
  (the `FOOTER_OPTIONS` field itself is private, so the doc/example use the accessor),
  `SceneRouter.CONVERSATION_HISTORY_ROUTE` exists, and the `FooterController` constants match the
  source.
- Docs/reference-snippet only — no engine code changed, no Gradle validation required (the example
  is not on the compiled classpath).

## Extend the dialog block's hover style to the footer bar

Previously, hovering the footer bar did not affect the dialog block: with the (opt-in) hover fade
enabled, the dialog block faded back out the instant the pointer left its own bounds — including when
the player moved down onto the footer to click back / forward / save. The `addHoverCompanion(Node)`
hook that was built for exactly this ("extend hover-on to the footer bar") existed on
`DialogEntriesView` but was never wired up by the renderer.

### Changes

- `MainAppLayoutRenderer.autoWireDialogEntriesView(...)`: after `bindToFooter(root)`, locate the
  footer via `ScreenShell.findFooterBar(root)` and register it with
  `dialog.addHoverCompanion(footer)`. Now hovering the footer keeps the dialog block in its hovered
  (lifted) style, as if the pointer were over the dialog block itself. No effect unless the host has
  opted into the hover fade via `DialogEntriesView.setHoverFadeEnabled(true)`.
- `DialogEntriesView.addHoverCompanion(Node)`: made idempotent (tracks registered companions in a
  new `wiredHoverCompanions` set and skips duplicates), matching the renderer's "all wiring helpers
  are idempotent" contract and the existing `bindToFooter` / `bindHistoryToggle` pattern. Updated
  the javadoc accordingly.
- `MainAppLayoutRendererDialogAutoWireTest`: added
  `rendererWiresFooterAsHoverCompanionSoFooterHoverKeepsDialogLifted` — enables the hover fade,
  fires synthetic `MOUSE_ENTERED` / `MOUSE_EXITED` on the footer, and asserts the dialog opacity
  lifts to `1.0` then drops back to `0.2`. Added a `syntheticMouse(...)` helper.
- `docs/USER_MANUAL.md`: documented the (previously undocumented) hover-fade API on the dialog
  entries widget — `setHoverFadeEnabled` / `setHoverFadeOpacities` / `addHoverCompanion` table rows
  plus a "Hover fade" prose paragraph explaining the footer-companion auto-wiring.

### Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.ui.MainAppLayoutRendererDialogAutoWireTest` —
  BUILD SUCCESSFUL (all tests pass, including the new hover-companion test).

## Preferences screen: two-column block grid + right-aligned labels / left-aligned fields

The preferences screen (`PreferencesSummaryScreen`) previously stacked its settings blocks in a
single vertical column, and each "label + field" row placed the label flush-left immediately before
its control, so fields did not line up.

### Changes

- `PreferencesSummaryScreen.createScene(...)`: the settings blocks (Audio, Visual, Text, Save,
  Language) are now laid out by a new `twoColumnBlocks(VBox...)` helper into a two-column `GridPane`
  (equal 50% columns, filled left-to-right / top-to-bottom, blocks stretch to fill their cell)
  instead of a single-column `VBox`.
- New `labeledRow(String, Node, Node...)` helper builds every "label + field" row as two columns:
  the label sits in a fixed-width (`LABEL_COLUMN_WIDTH = 150`), right-aligned column, and the field
  (plus any trailing node such as the volume `%` value) starts left-aligned immediately after it, so
  fields line up across rows. The row itself is `CENTER_LEFT`.
- Refactored the label-bearing rows to use it: `volumeRow` (slider now grows to fill, value label
  trails), `textSizeRow`, `hudOpacityRow`, `themeSelectionRow`, `footerDisplayRow`,
  `footerIconDisplayRow`, `textSpeedRow`. Checkbox-only rows (mute-all, auto-save, fullscreen) and
  the language radio row are left unchanged — they have no separate label column.
- Label text and the semantic style classes (`SCREEN_TEXT_STYLE_CLASS` /
  `SCREEN_VALUE_STYLE_CLASS`) are preserved, so existing label/value lookups and theme styling keep
  working.
- `PreferencesSummaryScreenTest`: added `twoColumnBlocksPlacesBlocksAcrossTwoColumns` (grid has two
  50% columns and blocks land at the expected row/column) and
  `labelFieldRowsRightAlignLabelsAndLeftAlignFields` (theme row label is `CENTER_RIGHT` with a fixed
  width, the row is `CENTER_LEFT`, and the combo follows the label).

### Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.ui.PreferencesSummaryScreenTest` — BUILD
  SUCCESSFUL (all tests pass, including the two new layout tests).

## Preferences screen: keep Main Menu / Close buttons pinned bottom-centre

Follow-up to the two-column change: the shell wraps the preferences content area in a top-aligned
`VBox`, which sizes the content area to its preferred height. Once the settings grid became shorter
(two columns instead of one), the content area shrank and the Main Menu / Close button bar floated
up just beneath the grid instead of staying at the bottom of the screen.

### Changes

- `PreferencesSummaryScreen.createScene(...)`: anchor the button bar with
  `BorderPane.setAlignment(closeBox, Pos.BOTTOM_CENTER)`, and grow the content area to fill the
  available height (`contentArea.setMaxHeight(Double.MAX_VALUE)` + `VBox.setVgrow(contentArea,
  Priority.ALWAYS)`) so its bottom slot — the Main Menu / Close buttons — stays pinned to the
  bottom-centre of the screen regardless of how tall the settings grid is.
- `PreferencesSummaryScreenTest`: added `mainMenuAndCloseButtonsStayBottomCentre` — verifies the
  button bar is a horizontally-centred `HBox` occupying the content area's bottom slot, anchored
  `BOTTOM_CENTER`, with the content area set to `VGROW = ALWAYS`.

### Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.ui.PreferencesSummaryScreenTest` — BUILD
  SUCCESSFUL.

## Background music repeats when finished

The engine already supported looping (the `music` channel is `loopingAllowed`, and
`JavaFxAudioPlaybackAdapter` cycles a `MediaPlayer` indefinitely for `command.loop()`), but the
generic play helpers built background-music requests with `loop=false`, so music stopped at the end
of the track instead of repeating.

### Changes

- `SoundRequest.music(String audioRef)`: new factory producing a **looping** request on
  `AudioService.MUSIC_CHANNEL` at full volume — the canonical "background music repeats" request.
- `GlobalApiAdapter.playSound(channelId, sourcePath)`: now loops when the channel is the music
  channel (`AudioService.MUSIC_CHANNEL`); all other channels stay one-shot. So background music
  played through the existing generic path repeats automatically.
- `GlobalApiAdapter.playMusic(String sourcePath)`: new convenience that starts looping background
  music via `SoundRequest.music(...)`.
- `docs/USER_MANUAL.md` (section 8, Audio support): documented that background music loops by
  default, the `SoundRequest.music` / `playMusic` helpers, and how to opt out (explicit non-looping
  `SoundRequest`).
- Tests:
  - `AudioQueueTest`: `musicFactoryBuildsLoopingRequestOnMusicChannel` and
    `musicRequestPlaysAsLoopingCommand` (the request loops and produces a looping playback command).
  - `GlobalApiAdapterTest`: `backgroundMusicLoopsWhileOtherChannelsPlayOnce` (music channel +
    `playMusic` loop; the sound channel plays once).

### Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.audio.AudioQueueTest --tests
  com.eb.javafx.globalApi.GlobalApiAdapterTest` — BUILD SUCCESSFUL.
