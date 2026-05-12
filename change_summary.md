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

---

## Startup error reporting also writes to console

`StartupErrorReporter.report` previously only opened a blocking `Alert` dialog. In headless runs, CI, or when stdout/stderr is being captured, the failure detail was invisible.

Fix: before showing the dialog, the reporter now writes `JavaFX startup failed [<category>]: <message>` followed by the full stack trace to `System.err`. The dialog behaviour is unchanged — both surfaces are populated from the same `category`/`message` so they stay in sync.

**Files changed:**
- `src/main/java/com/eb/javafx/ui/StartupErrorReporter.java`

---

## Theme-driven role colors for all nine ScreenDesignItemType values

The bundled `display-defaults.json` hard-codes role colors (`field.color = #ffffff`, `text.color = #ffffff`, `block.backgroundColor = #1d3a2a`, etc.) that were designed for a dark palette. When a light/pastel theme was active, those colors flowed in as inline JavaFX styles and overrode the theme stylesheet — light themes had unreadable text and dark green block surfaces.

This change makes role color/background defaults track the active palette so every ScreenDesignItemType (TEXT, TEXT_AREA, FIELD, MULTI_LINE_FIELD, BUTTON, POPLIST, COMBO_BOX, SLIDER, RADIO_GROUP) renders with theme-coherent colors. The field role explicitly satisfies the brief: dark themes use a darker-than-panel field background with white text; light/pastel themes use a near-white tinted field background with dark text.

**New types**
- `RoleColors` record with overrides for `text`, `heading`, `subheading`, `field`, `button`, `fieldLabel`, and `block` color/background.
- `DisplayDefaults.withRoleColors(RoleColors)` returns a new defaults instance with role and block color/background entries replaced.
- `DisplayDefaults.active()` / `installActive(...)` / `resetActive()` static accessors for the currently installed defaults (separate from the bundled `defaults()`).

**Palette additions**
- `ThemePalette` gains `fieldRoleColor` and `fieldRoleBackground` tokens, plus a `roleColors()` factory.
- Per-theme values:
  - OCEAN dark `#ffffff` on `#0a1426`; light `#1f3e50` on `#fbfeff`
  - FOREST dark `#ffffff` on `#0a140f`; light `#1d3427` on `#f5fdf8`
  - SUNSET dark `#ffffff` on `#160d12`; light `#4c322c` on `#fff8f4`
  - VIOLET dark `#ffffff` on `#110d1b`; light `#352c49` on `#fcf9ff`
  - HIGH_CONTRAST `#ffff66` on `#000000`
- Other roles overlay onto existing palette tokens (text→`labelText`, heading/subheading/fieldLabel→`accentColor`, button→`buttonText`/`buttonBackground`, block→`labelText`/`screenPanelBackground`/`sectionBorder`).

**Wiring**
- `UiTheme.initialize` builds themed defaults via `palette.roleColors()` and installs them as the active defaults.
- `ScreenDesignLayoutAdapter.toLayoutModel(...)` no-defaults overloads now read `DisplayDefaults.active()` instead of `defaults()` — so production rendering picks up theme-resolved role colors automatically.

**Test isolation**
- `UiThemeTest`, `BootstrapServiceTest`, `SceneRouterTest`, `GlobalApiAdapterTest` call `DisplayDefaults.resetActive()` in their `@AfterEach` teardown so the global active overlay doesn't leak between test classes.
- `ScreenDesignModelTest` resets active in both `@BeforeEach` and `@AfterEach`; its existing JSON-default assertions stay intact.

**Files changed/added:**
- `src/main/java/com/eb/javafx/ui/RoleColors.java` (new)
- `src/main/java/com/eb/javafx/ui/DisplayDefaults.java`
- `src/main/java/com/eb/javafx/ui/UiTheme.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignLayoutAdapter.java`
- `src/test/java/com/eb/javafx/ui/UiThemeTest.java` (new role-color tests)
- `src/test/java/com/eb/javafx/ui/ScreenDesignModelTest.java`
- `src/test/java/com/eb/javafx/bootstrap/BootstrapServiceTest.java`
- `src/test/java/com/eb/javafx/routing/SceneRouterTest.java`
- `src/test/java/com/eb/javafx/globalApi/GlobalApiAdapterTest.java`

---

## Remove two stale test methods

Two tests were failing on baseline `main` before this branch's work and were stale with respect to the current code:

- `ManagementApplicationTest.managementLauncherListsAuthoringScreens` — asserted an exact 6-label list against `ManagementApplication.managementActionLabels()`, but the launcher exposes 7 entries since the "All Items Test Screen" addition (commit `8530d7d`).
- `ScreenDesignerApplicationTest.bundledScreenDesignExamplesDemonstrateNewStylingMetadata` — asserted that every bundled `examples/resources/json/screens/*.json` carries `backgroundColor`/`transparency`/`borderStyle`/`borderColor` on every block and `backgroundColor`/`transparency` on every item, which is no longer true for newer example screens.

Both removed at the user's request rather than re-pinned to current values. The remaining tests in each class still exercise their respective behavior (`managementActionsHaveDescriptionsAndLaunchers`, plus the rest of `ScreenDesignerApplicationTest`).

**Files changed:**
- `src/test/java/com/eb/javafx/testscreen/ManagementApplicationTest.java`
- `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java`

---

## Fix four pre-existing test failures (preferences row lookup, block/json background placement, configured screen background)

Three production gaps and one test bug were causing four failing tests on baseline. None of these relate to the theme-color work earlier in the branch.

**1. `PreferencesSummaryScreenTest.preferencesRowsUseSemanticThemeTextAndValueClasses` — `findLabel` aborted on first leaf miss**

`findLabel(Pane pane, String text)` threw `AssertionError("Missing label: ...")` at the end of every recursive call, so as soon as it descended into the first child container that didn't contain the target, the throw propagated up and aborted the outer loop before it ever reached the next settings block (where the "Theme" label lives). It happened to work for "Master volume" only because that label is the first child of the first row visited.

Fix: split the helper. `searchLabel(...)` recurses and returns `null` when not found in a branch; `findLabel(...)` is now a thin wrapper that throws once after the full tree has been walked.

**2 & 3. `BlockBackgroundImageTestScreenTest.rendererSupportsFixedAndStretchBlockBackgroundImagePlacements` + `JsonBlockBackgroundImageTestScreenTest.rendererSupportsFixedAndStretchBlockBackgroundImagePlacementsFromJson` — `firstBlockBackgroundLayer` returned empty Optional**

Both tests build a section model with `backgroundImage` and `backgroundImagePlacement`, then look up a `StackPane` whose first child is a Region with one `ImageView` of opacity ≈ `BACKGROUND_OPACITY` (0.5). Neither test was setting `backgroundImageTransparency`, so the renderer defaulted the image opacity to 1.0 and the predicate never matched.

Fix: add `"backgroundImageTransparency"` to the inline section metadata (`BlockBackgroundImageTestScreenTest`) and the JSON template (`JsonBlockBackgroundImageTestScreenTest.placementDesignJson`), using `1.0 - BACKGROUND_OPACITY` so the rendered opacity hits 0.5.

**4. `JsonBlockBackgroundImageTestScreenTest.createRootLayersJsonScreenBackgroundBehindRenderedLayoutAndBlockBackgrounds` — three separate production gaps**

The test inspected the screen background layer produced by `ScreenLayoutRenderer.createPreviewRoot(...)` (which goes through `ScreenShell.withConfiguredBackground`, not `withBackgroundSvg`). It asserted:
- the layer carries `SCREEN_BACKGROUND_SVG_STYLE_CLASS` — only `SvgBackground` was adding that, not `ConfiguredBackgroundLayer`
- `background.getUserData()` equals the SVG resource path — neither layer was setting `userData`
- the wrapping `BorderPane` shell and its center body both have inline `transparent` background — `containerStyle(metadata, hideBackground=true)` was just omitting the `-fx-background-color` instead of forcing it to `transparent`, and the body region wasn't touched at all

Fix: in `ScreenShell`, plumb the original and resolved image references through to `ConfiguredBackgroundLayer`, which now sets `userData = originalImageReference` and adds `SCREEN_BACKGROUND_SVG_STYLE_CLASS` when either reference ends with `.svg`. In `ScreenLayoutRenderer`, `containerStyle(metadata, hideBackground=true)` now emits `-fx-background-color: transparent;` explicitly, and `createPreviewRoot` also writes `-fx-background-color: transparent;` onto the BorderPane's center body so the configured background actually shows through.

**Files changed:**
- `src/test/java/com/eb/javafx/ui/PreferencesSummaryScreenTest.java`
- `src/test/java/com/eb/javafx/ui/test/BlockBackgroundImageTestScreenTest.java`
- `src/test/java/com/eb/javafx/ui/test/JsonBlockBackgroundImageTestScreenTest.java`
- `src/main/java/com/eb/javafx/ui/ScreenShell.java`
- `src/main/java/com/eb/javafx/ui/ScreenLayoutRenderer.java`

---

## Improve screen-design JSON wire format

Three authoring improvements to `examples/resources/json/screens/*.json`, with full backward compatibility — the parser accepts both the legacy and the new shape.

**1. Items nest inside their parent block**

Previously `items[]` was a flat top-level array and each item carried a `blockId` foreign-key field. Authors had to scroll between block and item arrays to read or edit a single block. Now `items[]` is an optional array on each block; nested items omit `blockId` (it is derived from the containing block). The Java model is unchanged — `ScreenDesignModel.items()` still returns a flat list with each item's `blockId` populated.

Parser still accepts top-level `items[]` for legacy files. Serializer always writes nested form; if any items reference a block that doesn't exist in `design.blocks()`, those orphans are emitted at the top level for round-trip safety.

**2. Typed metadata primitives**

Known-typed metadata keys are now emitted as native JSON primitives by the serializer and accepted as primitives by the parser:

- **Booleans**: `showTicks`, `showLabels`, `dialog`, `dismissOnClickOutside`, `dismissOnEscape` → `true` / `false`
- **Numbers**: `transparency`, `backgroundImageTransparency`, `borderThickness`, `min`, `max`, `step` → `0.05`, `2`, etc.

Quoted-string forms remain valid on input and route through the same validator as before, so files like `"borderThickness": "2"` keep loading and `"dialog": "treu"` still errors. The change is one-way nicer: authors can now write `"transparency": 0.05` and the round-trip preserves it.

**3. `options` as a JSON array, not a CSV string**

`POPLIST`, `COMBO_BOX`, and `RADIO_GROUP` items used to encode their option list as a comma-separated string inside `metadata.options`, which made commas in option text impossible. The new form is a top-level `options` field on the item:

```json
{ "id": "lang", "type": "POPLIST", "options": ["English", "Français", "Deutsch, formal"] }
```

The CSV form is still accepted on input for legacy files and is normalized internally to the canonical JSON-array string the renderer and validator decode.

**New types**

- `OptionListEncoding` — `decode(String)` accepts either JSON-array or legacy CSV and returns a `List<String>`; `encode(List<String>)` produces the canonical JSON-array string.
- `ScreenDesignItem.options()` — instance method returning the decoded option list (`List.of()` when not set), so callers don't need to know the wire encoding.

**Wiring**

- `ScreenDesignJson.fromJson` walks `block.items[]` and `top.items[]`, tolerantly accepts string/boolean/number metadata values (canonicalized to strings), and rewrites both `options` arrays and legacy CSV `metadata.options` to the canonical JSON-array string in metadata.
- `ScreenDesignJson.toJson` groups items by block id, emits each block's `items[]` inline, lifts `options` out of metadata to a top-level array, and emits typed metadata keys as JSON primitives.
- `ScreenDesignValidator.validateOptionsMetadata` and `ScreenLayoutRenderer` (both COMBO_BOX and RADIO_GROUP paths) now consume options via `OptionListEncoding.decode` so they handle either form transparently.
- `examples/resources/json/screens/all-items-test-screen.json` migrated to the new format as the canonical reference example. The other five bundled example files keep the legacy shape and continue to load unchanged.

**Bonus: revert preview-root transparency emission from `containerStyle`**

The earlier fix to `JsonBlockBackgroundImageTestScreenTest.createRootLayersJsonScreenBackgroundBehindRenderedLayoutAndBlockBackgrounds` made `containerStyle(metadata, hideBackground=true)` emit `-fx-background-color: transparent;`, which regressed `ScreenLayoutRendererTest.rendererRemovesBackgroundFromLayoutOnlyContainerSections` (that test asserts hide-background sections leave the CSS to inherit). The transparency forcing now lives inline in `createPreviewRoot` where it is actually needed; `containerStyle(metadata, true)` simply omits `-fx-background-color`, satisfying both tests.

**Files changed/added:**
- `src/main/java/com/eb/javafx/ui/OptionListEncoding.java` (new)
- `src/main/java/com/eb/javafx/ui/ScreenDesignItem.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignJson.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignValidator.java`
- `src/main/java/com/eb/javafx/ui/ScreenLayoutRenderer.java`
- `src/test/java/com/eb/javafx/ui/ScreenDesignModelTest.java`
- `examples/resources/json/screens/all-items-test-screen.json`

## Update USER_MANUAL.md for new screen JSON wire format

Updated `docs/USER_MANUAL.md` section 6 to reflect the three JSON format improvements introduced in the previous change:

- **Nested items**: updated `ScreenDesignJson` overview and JSON example to show items nested inside their parent block's `items[]` array rather than a flat top-level `items[]`. `blockId` on each item is now noted as optional in the JSON wire format (derived from the containing block when absent).
- **Typed metadata**: JSON example and metadata keys table updated to show numeric keys (`borderThickness`, `transparency`, `min`, `max`, `step`) as JSON numbers and boolean keys (`dialog`, `dismissOnClickOutside`, `dismissOnEscape`, `showTicks`, `showLabels`) as JSON booleans. The editing-rules bullet list explains that both typed and quoted-string forms are accepted.
- **Options as JSON arrays**: `POPLIST`, `COMBO_BOX`, and `RADIO_GROUP` item descriptions updated from "comma-separated choices in `metadata.options`" to "JSON string array in top-level `options` field". Backward compatibility with legacy CSV strings noted.
- **`ScreenDesignItem.options()` accessor**: added bullet noting the `options()` method for reading decoded option lists from Java code.
- **Block background image example**: inline metadata updated to use typed numeric values (`borderThickness: 3`, `transparency: 0.15`, `backgroundImageTransparency: 0.5`) matching the new canonical format.

**Files changed:**
- `docs/USER_MANUAL.md`

## Add scrollbars to layout content area when items overflow

Items in a screen design could go off-screen when the total block content exceeded the window height.

**Fix:** Wrapped the `layoutContent` node (all blocks) in a `ScrollPane` inside `ScreenLayoutRenderer.createRoot`. The scroll pane is configured with:
- `setFitToWidth(true)` — content fills available width; no horizontal scrollbar
- `setVbarPolicy(AS_NEEDED)` — vertical scrollbar appears only when content overflows
- `setHbarPolicy(NEVER)` — no horizontal scrollbar
- `setMinHeight(0)` and `VBox.setVgrow(ALWAYS)` — lets the parent VBox distribute the bounded center height through the layout chain, so the scroll pane has a real max height and knows when to scroll
- Transparent inline style — block backgrounds and borders show through

Actions and footer labels remain outside the scroll pane so they stay anchored at the bottom of the screen regardless of scroll position.

`VBox.setVgrow(content, ALWAYS)` is also set on the outer content VBox so the body VBox created by `ScreenShell.titled` constrains it to the BorderPane center height (the window's available height). This is the constraint that ultimately triggers the scrollbar when blocks overflow.

**Test fix:** Updated `findNode` in `ScreenLayoutRendererTest` to check `ScrollPane.getContent()` directly. JavaFX control skins (including the ScrollPane viewport) may not be installed until the control is placed in a scene, so `getChildrenUnmodifiable()` can return an empty list on a freshly created ScrollPane without a scene. Checking `getContent()` bypasses the skin and keeps the recursive node lookup correct.

**Files changed:**
- `src/main/java/com/eb/javafx/ui/ScreenLayoutRenderer.java`
- `src/test/java/com/eb/javafx/ui/ScreenLayoutRendererTest.java`

---

## Fix ScrollPane layout test failures after content-area scrolling change

After wrapping layout content in a `ScrollPane`, two tests that inspect the rendered node tree without a scene failed:

- `BlockBackgroundImageTestScreenTest.createRootLayersScreenBackgroundBehindRenderedLayoutAndBlockBackgrounds`
- `JsonBlockBackgroundImageTestScreenTest.createRootLayersJsonScreenBackgroundBehindRenderedLayoutAndBlockBackgrounds`

Both failed with "Expected the sample blocks to use rounded corners instead of pill corners." The `usesRoundedInsteadOfPillClip` predicate checks `clip.getArcWidth() < stackPane.getWidth()` — with `stackPane.getWidth() == 0` the constant arc of 12 was not less than 0.

**Root cause:** The `ScrollPane`'s parent VBox (`LAYOUT_VBox`) runs `layout()` in dirty-branch mode after the root resize, which propagates the dirty flag without calling `layoutChildren()`. Its three children (subtitle Label, ScrollPane, footer Label) receive width 0 instead of the usable 576 px. The `layoutScrollableContent` test helper then finds `scrollPane.getWidth() == 0` and skips resizing the content, so block StackPanes remain at width 0.

**Fix:** In `layoutScrollableContent`, when `scrollPane.getWidth() <= 0`, fall back to the parent region's usable width (`parentRegion.getWidth() - insets.getLeft() - insets.getRight()`). The parent VBox has the correct width from the root layout pass even though it didn't call `layoutChildren`, so the content is sized and laid out correctly.

Also removed lingering diagnostic `System.err.println` calls that were left in from debugging.

**Files changed:**
- `src/test/java/com/eb/javafx/ui/test/BlockBackgroundImageTestScreenTest.java`
- `src/test/java/com/eb/javafx/ui/test/JsonBlockBackgroundImageTestScreenTest.java`
