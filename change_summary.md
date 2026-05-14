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
