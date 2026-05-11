## Fix ScreenDesignItem label handling and screen metadata boolean validation

**Bug 1 — Labels silently dropped for TEXT and TEXT_AREA items**

`ScreenDesignItem.supportsLabel()` previously returned `false` for `TEXT` and `TEXT_AREA`, causing the constructor to null out any label supplied for those types. All example screen JSON files (main-menu, quest-log, gallery-preview, sample) use labels on TEXT items, and the corresponding `_text.json` sidecars had live text entries for those label keys — all of which were being discarded on load.

Fix: `supportsLabel` now returns `true` for all item types. Labels on TEXT and TEXT_AREA items are preserved in the model and survive JSON round-trips. Rendering behaviour is unchanged (TEXT/TEXT_AREA `itemLine` still uses `text`, not `label`).

**Bug 2 — Screen metadata boolean keys not validated**

`validateScreenMetadata` checked `borderStyle` and `borderCorner` but left `dialog`, `dismissOnClickOutside`, and `dismissOnEscape` unvalidated. Typos (e.g. `"dialog": "treu"`) were accepted silently and resolved to `false` at runtime, masking authoring errors.

Fix: added `validateBooleanValue` helper and `BOOLEAN_VALUES` constant to `ScreenDesignValidator`. All three dialog keys are now validated; values must be one of: `true`, `false`, `1`, `0`, `yes`, `no`, `y`, `n`, `on`, `off` (case-insensitive).

**Files changed:**
- `src/main/java/com/eb/javafx/ui/ScreenDesignItem.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignValidator.java`
- `src/test/java/com/eb/javafx/ui/ScreenDesignModelTest.java`

---

## Add POPLIST, COMBO_BOX, SLIDER, and RADIO_GROUP item types

Four new `ScreenDesignItemType` values added with full stack support: JSON round-trip, layout adapter, JavaFX renderer, and validation.

**POPLIST** — non-editable dropdown; options supplied as `metadata.options` (comma-separated string). Rendered as a JavaFX `ComboBox` with `setEditable(false)`.

**COMBO_BOX** — dropdown that can optionally accept user input; `editable: true` enables typing. Rendered as a JavaFX `ComboBox` with `setEditable` driven by the item flag.

**SLIDER** — range slider; `metadata.min`, `metadata.max`, and `metadata.step` configure the range. `editable: true` makes the slider interactive (draggable); `false` disables it. `metadata.showTicks` and `metadata.showLabels` enable tick marks/labels. Validated: `max > min`, `step > 0`, numeric format.

**RADIO_GROUP** — mutually exclusive radio buttons; options from `metadata.options`. `metadata.orientation` (`horizontal`/`vertical`) controls layout. `editable: true` makes buttons clickable.

All new types:
- map to `ROLE_FIELD` in display defaults
- support the `label` field (rendered above the control), `value`, `defaultValue`, `sequence`, and `editable`
- carry `options`/`min`/`max`/`step` metadata through the layout adapter to the renderer
- validate type-specific metadata in `ScreenDesignValidator`

**Files changed:**
- `src/main/java/com/eb/javafx/ui/ScreenDesignItemType.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignItem.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignLayoutAdapter.java`
- `src/main/java/com/eb/javafx/ui/ScreenLayoutRenderer.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignValidator.java`
- `src/test/java/com/eb/javafx/ui/ScreenDesignModelTest.java`
- `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java`
- `docs/USER_MANUAL.md`

---

## Add all-items test screen covering every supported item type

Created a manual test screen that exercises all nine `ScreenDesignItemType` values in one place: TEXT, TEXT_AREA, FIELD, MULTI_LINE_FIELD, BUTTON, POPLIST, COMBO_BOX, RADIO_GROUP, and SLIDER.

The screen is split into five thematic blocks: Display Items, Text Inputs, Selection Inputs, Range Inputs, and Actions. Each item is backed by a key in the paired `_text.json` sidecar.

An "All Items Test Screen" button was added to `ManagementApplication` and a `showAllItemsFromManagement(Path)` entry point was added to `JsonScreenDesignTestScreen`.

**Files changed:**
- `examples/resources/json/screens/all-items-test-screen.json` (new)
- `examples/resources/json/screens/all-items-test-screen_text.json` (new)
- `src/main/java/com/eb/javafx/ui/test/JsonScreenDesignTestScreen.java`
- `src/test/java/com/eb/javafx/testscreen/ManagementApplication.java`
- `src/test/java/com/eb/javafx/ui/test/JsonScreenDesignTestScreenTest.java` — added `@ManualTest runAllItemsTestScreenFromTestApp` so the screen is also accessible from the `runTestScreen` Gradle task

---

## Add fullscreen, mute-all, and language controls to preferences screen

Extended `PreferencesService` with three new persisted preferences and added matching controls to `PreferencesSummaryScreen`.

**PreferencesService additions:**
- `fullscreen` (boolean, default `false`) with `saveFullscreen(boolean)`; persisted under `window.fullscreen`.
- `muteAll` (boolean, default `false`) with `saveMuteAll(boolean)`; persisted under `audio.muteAll`. `AudioService.initialize` now reads this value so playback respects the saved mute state at startup instead of hard-coding `muted = false`.
- `language` (new `Language` enum, default `ENGLISH`) with `saveLanguage(Language)` and validating `saveLanguage(String)`; persisted under `ui.language`. The enum exposes `enabled()` so disabled placeholder locales (Spanish, French, Japanese) can be rendered but blocked from selection.

**PreferencesSummaryScreen additions:**
- Audio block now includes a "Mute all" checkbox. Toggling it persists the preference and calls `audioService.setMuted(...)` when the audio service is initialized.
- Visual block now includes a "Fullscreen" checkbox. Toggling it persists the preference and calls `primaryStage.setFullScreen(...)`.
- A new "Language" block contains a `ToggleGroup` of radio buttons — one per `Language` value. Only English is enabled; the other entries are visible but disabled. Selection persists via `saveLanguage`.

Screen text keys added to `preferences_text.json`: `block.language.title`, `item.mute-all.label`, `item.fullscreen.label`.

**Files changed:**
- `src/main/java/com/eb/javafx/prefs/PreferencesService.java`
- `src/main/java/com/eb/javafx/audio/AudioService.java`
- `src/main/java/com/eb/javafx/ui/PreferencesSummaryScreen.java`
- `src/main/resources/com/eb/javafx/ui/screens/preferences_text.json`
- `src/test/java/com/eb/javafx/prefs/PreferencesServiceTest.java`
- `src/test/java/com/eb/javafx/ui/PreferencesSummaryScreenTest.java`

---

## Fix PreferencesSummaryScreenTest test helpers

Two test reliability fixes in `PreferencesSummaryScreenTest`:

**`themeChangesPreserveCurrentPreferencesSceneSize`** — replaced `stage.show()` + `stage.setWidth(910)` with a direct call to the package-private `PreferencesSummaryScreen.createScene(context, 910.0, 650.0)`. The old approach relied on `scene.getWidth()` updating synchronously after a stage resize, which does not happen within the same JavaFX pulse on Windows; the new approach creates a scene with exact dimensions so both the captured `currentSceneWidth` and `applyTheme`'s internal read are reliably 910. Extracted `createTestContext` helper (returns `RouteContext`) and refactored `createManualRouter` to delegate to it.

**`findLabel`** — split into `findLabel` (throws if absent) + `findLabelRecursive` (returns `null`). The original single-method form threw on every recursive miss, aborting the search as soon as the first child pane did not contain the target label. Now mirrors the existing `findCheckBox`/`findCheckBoxRecursive` pattern.

**Files changed:**
- `src/test/java/com/eb/javafx/ui/PreferencesSummaryScreenTest.java`
