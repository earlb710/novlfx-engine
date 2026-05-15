# Change Summary

## Footer icon display preference (preferences screen poplist)

Added a new `FooterIconDisplay` preference with three options — **Icons only**, **Icons with text**, **Text only** — and wired it into the preferences screen and footer rendering pipeline.

### Files changed

- **`PreferencesService.java`** — new `FooterIconDisplay` enum (`ICONS_ONLY`, `ICONS_WITH_TEXT`, `TEXT_ONLY`), preference key `ui.footerIconDisplay`, field, getter `footerIconDisplay()`, save methods `saveFooterIconDisplay(FooterIconDisplay)` / `saveFooterIconDisplay(String)`, and `validatedFooterIconDisplay` fallback.
- **`SystemCodeTables.java`** — new constant `FOOTER_ICON_DISPLAY_TABLE_ID = "footer-icon-display"`.
- **`system-code-tables.en.json`** — new `footer-icon-display` code table with codes `icons-only` / `icons-with-text` / `text-only`.
- **`ScreenShell.java`** — new `DEFAULT_FOOTER_ICON_DISPLAY` constant; added `displayText(FooterShortcutDisplay, FooterIconDisplay)` overload on `FooterOption`; refactored private `applyFooterOption` to accept an explicit `ImageView` graphic parameter; added public `applyFooterOption(Label, FooterOption, FooterShortcutDisplay, FooterIconDisplay)`; updated `applyFooterPreferences` to apply both shortcut and icon display together.
- **`PreferencesSummaryScreen.java`** — new `footerIconDisplayRow(context)` combo-box row in the Visual settings block; footer icon display row added to `viewModel()` summary rows.
- **`preferences_text.json`** — new key `item.footer-icon-display.label` = "Footer icons".
- **`PreferencesServiceTest.java`** — added `ui.footerIconDisplay` fixture in load test; new `saveFooterIconDisplayAcceptsKnownValuesAndFallsBackForUnknownValues` test.
- **`SummaryViewModelTest.java`** — updated row count from 5 → 6 and added assertions for the new "Footer icons" row.
- **`ScreenShellTest.java`** — new `footerIconDisplayControlsIconAndTextVisibility` test covering all three `FooterIconDisplay` values with both shortcut display modes.

### Rendering behaviour

| `FooterIconDisplay` | Graphic (SVG image) | Label text |
|---|---|---|
| `ICONS_ONLY` | shown if available | stripped to empty (or emoji fallback if no image) |
| `ICONS_WITH_TEXT` | shown if available | label + optional shortcut per `FooterShortcutDisplay` |
| `TEXT_ONLY` | hidden | label + optional shortcut per `FooterShortcutDisplay` (no icon prefix) |

---

## Theme-driven footer icon color

Footer button SVG icons (source fill `#ffcc00`) are now re-rasterized with a theme-appropriate color so they remain readable on all theme variants.

### Color values

| Theme variant | Icon color | Rationale |
|---|---|---|
| All dark variants | `#ffe066` | Light warm yellow — good contrast on dark backgrounds |
| All pastel (light) variants | `#7a5800` | Dark amber — sufficient contrast on light/white backgrounds |
| High contrast | `#ffcc00` | Original yellow — preserved for maximum legibility |

### Files changed

- **`UiTheme.java`** — added `footerIconColor` field to `ThemePalette` record; populated in every `forSelection` and `highContrast` palette constructor; extracted in `initialize()` and exposed via `footerIconColor()` getter.
- **`ScreenShell.java`** — added `FOOTER_ICON_SOURCE_COLOR = "#ffcc00"` constant; `loadFooterIcon(resourcePath, iconColor)` applies `VectorImage.replaceFillColor` when a color is supplied; `footerGraphic(option, iconColor)` uses a composite cache key (`path:color`) so per-theme rasterizations are cached independently; new 5-arg `applyFooterOption` overload accepts `iconColor`; new `applyFooterPreferences(footer, prefs, uiTheme)` overload threads `uiTheme.footerIconColor()` through.
- **`PreferencesSummaryScreen.java`** — both `applyFooterPreferences` call sites updated to pass `context.uiTheme()`.

---

## Preferences screen scrollbar

The preferences screen content `VBox` is now wrapped in a `ScrollPane` (fit-to-width, vertical `AS_NEEDED`, horizontal `NEVER`, transparent background) so the screen scrolls gracefully when the window is too short to show all rows.

### Files changed

- **`PreferencesSummaryScreen.java`** — added `ScrollPane` import; in `createScene` the content `VBox` is wrapped in a transparent `ScrollPane` before being passed to `ScreenShell.titled()`.

---

## Crimson red theme

Added a new **Crimson** theme family with dark and light-pastel variants. It uses a pure, deep red accent (`#e83030` dark / `#c01515` pastel) that is distinctly more saturated and redder than the Sunset theme's coral/salmon tones.

### Files changed

- **`PreferencesService.java`** — added `CRIMSON("crimson")` to `ThemeFamily` enum.
- **`UiTheme.java`** — added `CRIMSON` cases in `ThemePalette.forSelection` with full dark and pastel palettes (dark `footerIconColor = "#ffe066"`, pastel `footerIconColor = "#7a5800"`).
- **`system-code-tables.en.json`** — added `{"id": "crimson", "title": "Crimson", "sortOrder": 50}` to the `theme-family` code table.
- **`PreferencesSummaryScreen.java`** — added `Crimson - Dark` and `Crimson - Light pastel` entries to `themeOptionLabels()` and `themeChoices()`.
- **`SummaryViewModelTest.java`** — updated `preferencesThemeOptionsListAllFamiliesAndVariants` to include the two new Crimson entries.
- **`UiThemeTest.java`** — new `crimsonThemeProducesRedAccentColors` test covering both dark and pastel Crimson variants.

---

## Per-screen accent color override

Screens can now override the global theme accent color by applying an additional stylesheet returned from `UiTheme.accentOverrideStylesheet(String accentColor)`.

### Usage

```java
String override = context.uiTheme().accentOverrideStylesheet("#ff4444");
if (override != null) {
    scene.getStylesheets().add(override);
}
```

The override stylesheet must be added **after** `context.uiTheme().stylesheet()` so its rules take precedence.

### Rules overridden

| Selector | Property |
|---|---|
| `.root` | `-fx-accent`, `-fx-default-button`, `-fx-focus-color`, `-fx-selection-bar`, `-fx-mark-color` |
| `.screen-title` | `-fx-text-fill` |
| `.screen-text-highlight`, `.layout-text-highlight` | `-fx-text-fill` |
| `.layout-section-title` | `-fx-text-fill` |

Passing `null` or blank returns `null` (no override applied).

### Files changed

- **`UiTheme.java`** — new `accentOverrideStylesheet(String accentColor)` public method.
- **`UiThemeTest.java`** — new `accentOverrideStylesheetGeneratesOverrideForSpecifiedColor` test.

---

## Screen snapshot command-line tool

Added `ScreenSnapshotApplication` — a headless JavaFX `Application` that renders a screen JSON design to a PNG, JPEG, or BMP image file without opening the interactive designer.

### Usage

```bash
./gradlew --no-daemon runScreenSnapshot \
    -Pscreen=<screen.json> -Pout=<output-image> [-Pwidth=N] [-Pheight=N]
```

Running without `-Pscreen` / `-Pout` prints usage help and exits cleanly.

### Rendering pipeline

The tool reuses the same pipeline as the designer's live preview:
1. `ScreenDesignJson.load(path)` — loads the JSON design and its `_text.json` sidecar
2. `UiTheme.initialize(preferencesService)` — applies the user's saved theme and installs `DisplayDefaults`
3. `ScreenDesignLayoutAdapter.toLayoutModel(design, true, DisplayDefaults.active())` — converts to layout model
4. `ScreenLayoutRenderer.createPreviewRoot(layoutModel, workingDir)` — renders to a JavaFX `Parent`
5. `Scene.snapshot(null)` — rasterizes (stage shown at opacity 0 to trigger layout)
6. `SwingFXUtils.fromFXImage` + `ImageIO.write` — saves PNG/JPEG/BMP

JPEG output composites the alpha channel on white before encoding since JPEG has no transparency support.

### Files changed

- **`ScreenSnapshotApplication.java`** (`src/test/java/com/eb/javafx/testscreen`) — new JavaFX `Application` with `main`.
- **`build.gradle`** — new `runScreenSnapshot` `JavaExec` task; args assembled from `-Pscreen`, `-Pout`, `-Pwidth`, `-Pheight` project properties.
- **`CLAUDE.md`** — `runScreenSnapshot` added to the manual-tools table.
- **`docs/USER_MANUAL.md`** — new "Screen snapshot tool" subsection with parameter table and examples.
- **`docs/PORT_JAVAFX_PLAN.md`** — `ScreenSnapshotApplication` noted in the current reusable scope section.

---

## Preferences screen "Main Menu" button with confirmation

Added an explicit **Main Menu** button to the bottom action bar of the preferences screen, alongside the existing **Close** button. Clicking it shows a JavaFX `CONFIRMATION` alert ("Are you sure you want to leave preferences and return to the main menu?"); confirming navigates to `SceneRouter.MAIN_MENU_ROUTE`, declining keeps the user on the preferences screen.

Because the library navigates via the well-known route id `SceneRouter.MAIN_MENU_ROUTE`, applications using the engine effectively "pass in" their main menu screen by registering a route factory under that id (already the convention, e.g. via `DefaultRouteModule` or an application-specific replacement).

### Files changed

- **`PreferencesSummaryScreen.java`** — imports `Alert`, `Alert.AlertType`, `ButtonType`, and `Optional`; new `MainMenuConfirmation` functional interface with `setMainMenuConfirmation` / `clearMainMenuConfirmation` hooks for tests; new package-private `confirmReturnToMainMenu()` that shows a `CONFIRMATION` `Alert` (or invokes the test override); bottom action bar now has both `mainMenuButton` (new) and `closeButton`, laid out in a single centered `HBox` with 12px spacing.
- **`preferences_text.json`** — new keys `item.main-menu.label` ("Main Menu"), `dialog.main-menu-confirm.title`, `dialog.main-menu-confirm.header`, and `dialog.main-menu-confirm.content` for the confirmation dialog.
- **`PreferencesSummaryScreenTest.java`** — new tests `mainMenuButtonStaysOnPreferencesWhenConfirmationDeclined` and `mainMenuButtonNavigatesToMainMenuWhenConfirmed` use the confirmation override hook to verify both branches without invoking a real modal dialog; added `findButton` recursive helper.

### Drive-by compile fixes

The test sources had pre-existing duplicate-variable compile errors blocking the whole `compileTestJava` task. Renamed the inner `initialRoot` local to `initialSceneRoot` so tests compile:

- **`PreferencesSummaryScreenTest.preferencesFooterClosesBackToMainMenu`**
- **`PreferencesFooterTestScreenTest.footerPreferencesLabelNavigatesToPreferencesRouteFromTestScreen`**

---

## Main app layout scaffolding

Added engine-level scaffolding for the typical visual-novel app frame so applications can compose a background, a story/dialog central split, an optional footer, and any number of HUD overlay screens through one screen-design JSON file rather than wiring the frame in JavaFX code.

### New types in `com.eb.javafx.ui`

- **`ScreenLayoutType.MAIN_APP_LAYOUT`** — new layout-type value naming the app-frame composition. The standard `ScreenLayoutRenderer.layoutContent(...)` now throws an `IllegalArgumentException` pointing callers at `MainAppLayoutRenderer` for this case.
- **`MainAppLayoutPlan`** — UI-neutral parsed plan with constants for every supported metadata key (background, story/dialog screen ids, ratio, orientation, footer flag, overlay list). `from(ScreenDesignModel)` validates the design and pulls structural parts out of screen and block metadata.
- **`MainAppLayoutOverlay`** — UI-neutral overlay record: id, screen id, placement mode/anchor, offsets, optional preferred size, opacity, visibility.
- **`MainAppLayoutPlacementMode`** — enum with three positioning modes: `ALIGNMENT`, `PIXELS`, `PERCENT`.
- **`MainAppLayoutAnchor`** — nine-grid anchor enum mapping to JavaFX `Pos`; the parser accepts JavaFX names (`TOP_CENTER`) and short aliases (`TOP`, `BOTTOM`, `LEFT`, `RIGHT`).
- **`MainAppLayoutOrientation`** — `VERTICAL` (default) or `HORIZONTAL` story/dialog split.
- **`MainAppScreenResolver`** — functional interface that resolves a screen id to a JavaFX `Node`; returning `null` leaves the slot empty.
- **`MainAppLayoutRenderer`** — renders the plan into a `StackPane` of background + central frame + HUD overlay layer. Overlays are placed via `StackPane.setAlignment` (with margin offsets), absolute pixel margins, or fractional coordinates that re-bind on parent resize.

### Supported metadata keys

Screen-level (on `ScreenDesignModel.metadata`):

| Key | Default | Meaning |
|---|---|---|
| `storyScreenId` | _required_ | Screen id rendered in the story area |
| `dialogScreenId` | (omitted) | Screen id rendered in the dialog area; when omitted, story fills the central frame |
| `storyDialogRatio` | `0.75` | Story-area share of the central frame, in `0.0`–`1.0` |
| `appLayoutOrientation` | `vertical` | `vertical` (story on top) or `horizontal` (side by side) |
| `showFooter` | `true` | Renders the standard `ScreenShell` footer below the central frame |
| `appLayoutBackgroundImage` | (none) | Background image path |
| `appLayoutBackgroundFit` | `STRETCH` | `STRETCH` or `CROP_CENTER` |
| `appLayoutBackgroundTransparency` | `0.0` | Background image transparency, `0.0`–`1.0` |
| `appLayoutBackgroundColor` | (none) | Background canvas color |

Per overlay block (one block per overlay):

| Key | Default | Meaning |
|---|---|---|
| `overlayScreenId` | _required_ | Screen id rendered inside the overlay |
| `overlayPlacement` | `alignment` | `alignment`, `pixels`, or `percent` |
| `overlayAnchor` | `TOP_LEFT` | Alignment-mode nine-grid anchor (`TOP_RIGHT`, `BOTTOM_CENTER`, `CENTER`, short aliases `TOP`/`BOTTOM`/`LEFT`/`RIGHT`) |
| `overlayOffsetX` / `overlayOffsetY` | `0.0` | Pixel offsets (alignment), absolute pixels, or fractions (percent) |
| `overlayWidth` / `overlayHeight` | (none) | Optional preferred overlay size in pixels |
| `overlayTransparency` | `0.0` | Overlay transparency, `0.0`–`1.0` |
| `overlayVisible` | `true` | Hides the overlay when `false` |

### Files changed

- **`src/main/java/com/eb/javafx/ui/ScreenLayoutType.java`** — added `MAIN_APP_LAYOUT` enum value with Javadoc pointing at `MainAppLayoutRenderer`.
- **`src/main/java/com/eb/javafx/ui/MainAppLayoutAnchor.java`** — new enum.
- **`src/main/java/com/eb/javafx/ui/MainAppLayoutOrientation.java`** — new enum.
- **`src/main/java/com/eb/javafx/ui/MainAppLayoutPlacementMode.java`** — new enum.
- **`src/main/java/com/eb/javafx/ui/MainAppLayoutOverlay.java`** — new record.
- **`src/main/java/com/eb/javafx/ui/MainAppLayoutPlan.java`** — new record with `from(ScreenDesignModel)` parser.
- **`src/main/java/com/eb/javafx/ui/MainAppScreenResolver.java`** — new functional interface.
- **`src/main/java/com/eb/javafx/ui/MainAppLayoutRenderer.java`** — new renderer.
- **`src/main/java/com/eb/javafx/ui/ScreenLayoutRenderer.java`** — added `MAIN_APP_LAYOUT` branch that renders a structural preview by parsing the layout model via `MainAppLayoutPlan.fromLayoutModel(...)` and routing through `MainAppLayoutRenderer` with a labelled-placeholder resolver. When required metadata (e.g. `storyScreenId`) is missing, a wrapped notice label explains the issue instead of throwing. This makes the screen designer's live preview work for `MAIN_APP_LAYOUT` designs.
- **`src/main/resources/com/eb/javafx/ui/layout-contract.json`** — added `main-app-layout` to `layoutTypes` and the new `layout-main-app*` style classes to `stableStyleClasses`.
- **`src/test/java/com/eb/javafx/ui/MainAppLayoutPlanTest.java`** — new tests covering default parsing, metadata overrides, omitted dialog screen, layout-type guard, ratio range, overlay placement modes (alignment/pixels/percent), missing-screen-id failure, percent-range validation, and anchor aliasing.
- **`src/test/java/com/eb/javafx/ui/ScreenLayoutRendererTest.java`** — two new tests covering the designer-preview path for `MAIN_APP_LAYOUT`: that a well-formed model renders placeholder labels for the story, dialog, and overlay screen ids, and that a model missing required metadata produces an explanatory notice rather than throwing.
- **`src/main/java/com/eb/javafx/ui/MainAppLayoutPlan.java`** — added `fromLayoutModel(ScreenLayoutModel)` so the post-adapter representation produced by `ScreenDesignLayoutAdapter` can be parsed back into a plan; existing `from(ScreenDesignModel)` refactored to share the parser.

### Story area scrolling and pinned dialog/footer

- **`MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO`** raised from `0.75` to `0.875` so the dialog area is half its previous height.
- **`MainAppLayoutRenderer.centerArea(...)`** now uses a `BorderPane` for the central area: the story slot is wrapped in a transparent `ScrollPane` (fit-to-width, vertical `AS_NEEDED`, horizontal `NEVER`) and placed in the centre, while the dialog slot is pinned to `BorderPane.bottom` (vertical orientation) or `BorderPane.right` (horizontal orientation) with its `prefSize`/`minSize`/`maxSize` bound to `(1 − storyDialogRatio) × parent.size` so the dialog keeps a proportional fixed band. The outer `BorderPane.bottom` continues to hold the standard footer, so the dialog and footer remain visible regardless of how small the story area becomes.
- **`ScreenLayoutRenderer.createPreviewRoot(...)` / `createScrollablePreviewRoot(...)`** now detect `MAIN_APP_LAYOUT` models and route directly to `MainAppLayoutRenderer.render(...)` via a labelled-placeholder resolver, bypassing the standard title + outer-`ScrollPane` frame. Without this, the docked designer preview scrolled the whole composition together — including dialog and footer — instead of just the story area. The `layoutContent` branch in the same class remains as a degraded fallback for callers that bypass the preview entry points.
- **`examples/resources/json/screens/main-app-layout-test-screen.json`** — removed the explicit `storyDialogRatio` so the sample exercises the new default; description updated.
- **`docs/USER_MANUAL.md`** — `storyDialogRatio` entry updated for the new default, scrollable-story behaviour, and pinned dialog/footer guarantee.
- **`src/test/java/com/eb/javafx/ui/MainAppLayoutSampleJsonTest.java`** — asserts against `MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO` instead of the hard-coded `0.75`.

### Designer preview prints stack traces

- **`src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java`** — both preview error paths (docked panel and popup stage) now print the full exception stack trace to `System.err` before showing the short message in the UI, so the original cause is visible on the launching console.

### Designer property fields for `MAIN_APP_LAYOUT`

When the selected screen's layout type is `MAIN_APP_LAYOUT`, the Properties panel now exposes typed editors for every supported metadata key instead of forcing the author to use the Advanced metadata text box. Theme fields (Default color theme, Overwrite color theme) remain available as before.

**Screen panel (new rows shown only for `MAIN_APP_LAYOUT`):**
- Story screen id (`storyScreenId`)
- Dialog screen id (`dialogScreenId`)
- Story/dialog ratio (`storyDialogRatio`)
- App layout orientation (`appLayoutOrientation`)
- Show footer (`showFooter`)
- App layout background image (`appLayoutBackgroundImage`) — file chooser
- App layout background fit (`appLayoutBackgroundFit`)
- App layout background transparency (`appLayoutBackgroundTransparency`)
- App layout background color (`appLayoutBackgroundColor`) — colour picker

**Block panel (new rows shown only when the parent screen's layout type is `MAIN_APP_LAYOUT`):**
- Overlay screen id (`overlayScreenId`)
- Overlay placement (`overlayPlacement`)
- Overlay anchor (`overlayAnchor`)
- Overlay offset X / Y (`overlayOffsetX`, `overlayOffsetY`)
- Overlay width / height (`overlayWidth`, `overlayHeight`)
- Overlay transparency (`overlayTransparency`)
- Overlay visible (`overlayVisible`)

The new keys are added to `SCREEN_EXPOSED_METADATA_KEYS` and `BLOCK_EXPOSED_METADATA_KEYS` so they no longer appear in the Advanced metadata text area when the typed editor is shown. Default values follow the engine defaults: `showFooter` and `overlayVisible` default to `true` (only persisted when the author opts out), all other keys default to blank.

#### Files changed
- **`src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java`** — imports `MainAppLayoutPlan`; new option arrays for the orientation/fit/placement/anchor combo boxes; new instance fields for every screen + overlay control; `screenPropertiesPanel()` / `blockPropertiesPanel()` populate and lay out the new rows conditionally; `screenMetadata(...)` / `blockMetadata(...)` write the new fields back into metadata; `propertyLabelsFor(NavigationNode, ScreenLayoutType)` overload appends the new labels when `MAIN_APP_LAYOUT` is selected; backward-compatible `propertyLabelsFor(NavigationNode)` delegates to the overload with `null`.
- **`src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java`** — new `mainAppLayoutScreenPanelExposesAppFrameAndOverlayFields` test asserts the full label list for both screen and block panels when `MAIN_APP_LAYOUT` is in play. Existing label tests continue to pass since they call the no-arg overload (FORM-style labels).

### Relative overlay placement

Added a fourth overlay placement mode, `relative`, so authors can pin one overlay to another — e.g. "left of the status block" — instead of using absolute coordinates.

- **`MainAppLayoutPlacementMode.RELATIVE`** — new enum value plus parser aliases `relative` / `rel`.
- **`MainAppLayoutAnchor.ABOVE` / `BELOW` / `LEFT` / `RIGHT`** — four new relative-anchor values; `toJavaFxPos()` returns `null` for these and `isRelative()` returns `true`. The old `LEFT`/`RIGHT` short-aliases for `CENTER_LEFT`/`CENTER_RIGHT` were removed (they're now real anchor values); the `TOP`/`BOTTOM` aliases for `TOP_CENTER`/`BOTTOM_CENTER` remain.
- **`MainAppLayoutOverlay.anchorBlockId`** — new field naming the sibling overlay this one is anchored to in `relative` mode. The record validates that `relative` mode supplies both a relative anchor and an `anchorBlockId`, that `alignment` mode never uses a relative anchor, and that an overlay cannot anchor to itself.
- **`MainAppLayoutPlan.OVERLAY_ANCHOR_FIELD_KEY`** — new metadata key `overlayAnchorField` parsed from each overlay block.
- **`MainAppLayoutRenderer`** — now collects every overlay wrapper by id during the first pass and runs a second pass that binds each `RELATIVE` overlay's `StackPane` margin to its anchor's `boundsInParent`. The binding recomputes whenever the anchor moves/resizes or the overlay's own size changes, so the relative position holds through window resizes. Missing anchors are tolerated (the overlay falls back to its top-left default rather than failing).
- **`ScreenDesignerApplication`** — placement combo gains `relative`; anchor combo gains `ABOVE`/`BELOW`/`LEFT`/`RIGHT`; a new "Overlay anchor field" combo lists every sibling block in the current design (current block excluded). The new combo, value, and metadata key are wired through `screenPropertiesPanel()` / `blockMetadata(...)` and added to `BLOCK_EXPOSED_METADATA_KEYS`. Label list `MAIN_APP_LAYOUT_BLOCK_LABELS` updated; the matching `propertyLabelsFor` test also asserts the new "Overlay anchor field" label.
- **`MainAppLayoutPlanTest`** — four new tests cover relative parsing with anchor field, missing anchor-field rejection, nine-grid-anchor-on-relative rejection, and relative-anchor-on-alignment rejection.
- **`docs/USER_MANUAL.md`** — overlay metadata table updated for the new placement mode, relative anchor values, and `overlayAnchorField` key.

### Story area fills available height + slot insets

- **`MainAppLayoutRenderer.scrollableStorySlot(...)`** — now calls `ScrollPane.setFitToHeight(true)` so the story area visually fills its allocated band when content is shorter than the viewport. Tall content still scrolls when it sets its own `minHeight = USE_PREF_SIZE` (the standard ScrollPane idiom).
- **`ScreenLayoutRenderer.previewPlaceholder(...)`** — placeholder `StackPane` now has `maxSize` unbounded so it can grow to fill the story / dialog slot in the designer preview. Previously the placeholder collapsed to the height of its single Label, making the story slot appear "only a few lines high".
- **`MainAppLayoutInsets`** — new UI-neutral record with four non-negative sides and a `parse(String, String)` helper that accepts CSS-style shorthand (1, 2, or 4 comma-separated numbers).
- **`MainAppLayoutPlan`** — new `storyInsets` / `dialogInsets` fields with corresponding `STORY_INSETS_KEY = "storyInsets"` and `DIALOG_INSETS_KEY = "dialogInsets"` metadata constants; both default to `MainAppLayoutInsets.EMPTY` when the keys are absent.
- **`MainAppLayoutRenderer.centerArea(...)`** — applies the parsed insets as `Region.setPadding(...)` on the story and dialog slot regions, giving authors a way to push content inside the slot away from the slot's edges without affecting the slot's outer dimensions.
- **`ScreenDesignerApplication`** — new "Story insets" and "Dialog insets" text fields appear in the screen panel when the screen's layout type is `MAIN_APP_LAYOUT`. The new keys are added to `SCREEN_EXPOSED_METADATA_KEYS`, the `MAIN_APP_LAYOUT_SCREEN_LABELS` list, and the `propertyLabelsFor(NavigationNode, ScreenLayoutType)` label test.
- **`MainAppLayoutPlanTest`** — three new tests cover shorthand parsing (one/two/four values), default-empty insets, and rejection of malformed values.
- **`docs/USER_MANUAL.md`** — `storyInsets` / `dialogInsets` entries added to the main-app metadata list.

### HUD overlays anchored to the story area (no longer overlap dialog/footer)

Previously HUD overlay wrappers were children of the root `StackPane`, which covered the entire layout — so a `BOTTOM_RIGHT`-anchored overlay landed at the bottom-right of the whole frame, sitting on top of (or past) the dialog and footer bands. The user-visible request was: "alignment bottom must be the bottom right of the story screen, never overlap with dialog".

**Fix**: nest the HUD overlay wrappers inside a new `storyArea` `StackPane` that occupies exactly the story slot. All four placement modes (alignment, pixels, percent, relative) resolve coordinates against this StackPane, so:

- `BOTTOM_LEFT` / `BOTTOM_CENTER` / `BOTTOM_RIGHT` alignments land at the bottom edge of the story slot, not the bottom of the whole frame.
- `percent` mode 100%-y lands at the bottom of the story slot.
- `pixels` mode (0, 0) is the top-left of the story slot, not of the layout root.
- `relative` overlay-to-overlay anchoring uses sibling bounds within the same storyArea, unchanged in semantics.

#### Files changed
- **`src/main/java/com/eb/javafx/ui/MainAppLayoutRenderer.java`** — `render(...)` now builds the overlay wrappers up front and passes them to `mainFrame(...)` / `centerArea(...)`. A new `buildStoryArea(...)` helper wraps the story `ScrollPane` and the overlay collection in a `StackPane` with style class `layout-main-app-story-area`. The story scroll is set to `maxSize(Double.MAX_VALUE, Double.MAX_VALUE)` so it fills the storyArea behind the overlays. The relative-placement second pass still works because both anchor and anchored wrappers share the storyArea as their parent.
- **`docs/USER_MANUAL.md`** — new paragraph in the Main app layout section noting that overlays are scoped to the story area.

### Main app layout user-manual rewrite

The "Main app layout" section of `docs/USER_MANUAL.md` accreted patches over several commits and had stale wording (e.g. "three overlay placement modes" after a fourth was added). Rewrote the section as a coherent unit:

- ASCII frame diagram showing background → story (with HUD overlay layer inside) → dialog → footer stacking.
- Tightened screen-metadata and HUD-overlay-metadata bullet lists, removing duplication.
- New "Placement modes" table comparing `alignment` / `pixels` / `percent` / `relative` side-by-side with what `overlayOffsetX/Y` mean in each mode.
- New paragraph on the screen resolver and how the designer preview short-circuits to a labelled-placeholder resolver.
- New "Example" subsection with a minimal main-app-layout JSON design (background, story/dialog split, three overlays covering alignment / percent / relative) plus a runtime Java snippet showing the `MainAppLayoutPlan.from(...)` + `MainAppLayoutRenderer.render(plan, resolver, ...)` wiring.
- Continued to link to the packaged `examples/resources/json/screens/main-app-layout-test-screen.json` sample as the full-featured reference.
- **`examples/resources/json/screens/main-app-layout-test-screen.json`** (+ `_text.json` sidecar) — sample main-app-layout design that exercises every supported screen metadata key, all four anchor/placement combinations (`alignment` `TOP_LEFT`, `alignment` `TOP_RIGHT` with size hints, `percent`, `pixels`), and the standard `_text.json` localization sidecar pattern.
- **`src/test/java/com/eb/javafx/ui/MainAppLayoutSampleJsonTest.java`** — round-trips the sample file through `ScreenDesignJson.load(...)` + `MainAppLayoutPlan.from(...)` so drift between the sample and the loader/parser surfaces immediately.
- **`docs/USER_MANUAL.md`** — new "Main app layout" section documenting metadata keys, overlay placement modes, and the resolver-based composition pattern; reference list updated to note the new layout type; sample-design link added at the end of the section.

