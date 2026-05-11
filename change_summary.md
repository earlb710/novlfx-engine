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
