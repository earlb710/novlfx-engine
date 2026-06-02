# novlfx Engine — Setup Modding TODO

_Tracks the config-driven "setup modding" capability surface — letting games / mods change
behavior and look-and-feel through data files (no Java edits). Reference guide:
[MODDING_SETUP.md](MODDING_SETUP.md)._

This is **scoped to setup/config modding only** (see `MODDING_SETUP.md` for the boundary).
Content/code-level modding (new routes, new content modules, scripting) is out of scope here.

---

## Done

| Area | What | Key types |
|------|------|-----------|
| Theme CSS swap | `config.json` `resources.uiTheme` → external CSS file overrides the bundled stylesheet | `GameApplication.resolveConfiguredThemeStylesheetUrl`, `AltLifeSceneStyles.setOverrideStylesheetUrl` |
| Custom theme palette (colours) | `resources.themePalette` → JSON of colour-field overrides on a base family/variant; feeds the generated theme tokens | `UiTheme.loadCustomPalette` / `ThemePalette.withColorOverrides` |
| Window title & app icon | `resources.windowTitle` / `resources.appIcon` applied at boot | `BootstrapService.applyWindowChrome` |
| Custom fonts | `resources.font.*` entries register fonts at boot (classpath or on-disk), no whitelist edit | `ConfiguredFonts`, `FontResources.loadResource/loadFile` |
| Asset (icon/image) overrides | `resources.assetOverrideRoot` → a folder mirroring resource paths replaces bundled icons/images; any image type, extension-flexible | `ResourceOverrides`, `AltLifeSceneStyles.loadIcon` |
| Footer-icon overrides | Footer icons resolve through the override root; SVG keeps recolour, raster supported | `ScreenShell.loadFooterIcon` |
| Map artwork overrides | Town-map base (`map1.png`) + grid overlay (`map1-grid.png`) + building-hex/character icons override-aware | `AltLifeMapScreen.openMapImage` |
| Button shape/artwork overrides | Engine button SVGs (`button-pill-long.svg` / `button-bevel.svg`) resolve through the override root / repoint; colour stays theme-palette-driven | `ButtonVisuals.resolveArtworkResource` |
| Footer styling | Top-level `footer` object — font / text colour / select (highlight) colour / background / transparency — layered over the theme | `FooterStyle`, `ApplicationResourceConfig.footerStyle` |
| Footer keybindings & glyphs | `footerOptions.<id>` — remap shortcut and/or icon per footer option | `ScreenShell.setFooterOptionOverrides` |
| Tooltip delay | `tooltipDelayMs` — global tooltip show-delay (engine + generated `.tooltip` CSS) | `ScreenShell.setTooltipShowDelayMillis`, `FooterStyle` |
| Text-speed durations | `textSpeed: { slow, normal, fast }` ms tune what each speed preset means | `PreferencesService.textSpeedMillis`, `setTextSpeedDurations` |
| Audio channels | `audioChannels.<id>` — priority / volume / ducking policy / duck percent | `GameApplication.audioChannelConfig`, `ApplicationResourceConfig.audioChannelField` |
| Auto-advance cadence | `autoAdvance: { scrollFraction, minScrollMs, readPauseMultiplier }` | `AutoSkipController.setAutoAdvanceTuning` |
| Map building colours | `mapBuildingColors` JSON file → per-building hex gradient overrides | `AltLifeMapScreen.loadBuildingColors` |
| HUD opacity (preference) | Preferences → HUD opacity (Solid/Subtle/Faint) wires + applies `ui.hudAlpha` to the gameplay HUD | `PreferencesSummaryScreen.hudOpacityRow`, `AltLifeMainAppLayout` |
| HUD panel opacities | `hud: { dialogIdleAlpha, dialogActiveAlpha, locationRestAlpha, locationHoverAlpha, statusLogAlpha, panelAlpha }` — dialog block / location / status-log / generic panels | `AltLifeMainAppLayout.setHudAlphas`, `ApplicationResourceConfig.hudField` |
| Global text size (accessibility) | Preferences → Text size (Smaller/Normal/Bigger) rewrites every theme + game font size | `FontScaling`, `UiTheme`, `PreferencesSummaryScreen` |
| Backgrounds | App / preferences / save-load default backgrounds via `config.json` keys; per-scene via screen-layout JSON metadata | `ApplicationResourceConfig`, `ScreenDesignJson` |
| Per-screen backgrounds (any system screen) | `screenBackgrounds.<routeId>` = `{ color, image, transparency }`, resolved by the route being built | `RouteContext.themedScene*`, `ApplicationResourceConfig.screenBackground*` |
| Save behavior | End-of-day auto-save default-on + Auto-tab checkbox; persisted save page count + last-selected page | `QuickSaveActions`, `PreferencesService` (`save.*`) |
| Resource roots / named resources | `resourceRoots` (layered image/support/ui roots) and `resources` id→path map | `ApplicationResourceConfig`, `ResourceRegistry` |
| Auto-save toggle on Preferences | "Auto save daily" checkbox added to the Preferences screen (Save block), not only the Save screen | `PreferencesSummaryScreen.autoSaveDailyRow` |
| First-class config fields | `fonts: [...]`, `assetOverrideRoot`, `windowTitle`, `appIcon`, `uiTheme`, `themePalette` as top-level `config.json` fields (folded into `resources`; map convention still works) | `ApplicationResourceConfig.promoteFirstClassFields` |
| Per-icon repoint ids | `resources` entries keyed `icon:<originalPath>` repoint a single icon/image to a replacement (override-root or classpath) without mirroring the full path | `ResourceOverrides` aliases, `BootstrapService.collectIconAliases` |
| Dialog popup / save-screen fonts | `DialogMessages` (confirm/info/error title/header/content/button) + `SaveScreen` mode-title / page-label font sizes moved from inline Java to theme CSS (`.dialog-message-*`, `.save-screen-mode-title`, `.save-screen-page-label`) — now scale with the global Text-size accessibility setting and are mod-overridable via custom theme CSS | `UiTheme` stylesheet template, `DialogMessages`, `SaveScreen` |
| Dialog popup card width | `ui: { dialog: { minWidth, maxWidth } }` overrides the confirm/info/error popup card width (default 360 / 520) | `DialogMessages.setCardWidth`, `ApplicationResourceConfig.uiDialogField` |
| Dialog-block previous-entry fade | `ui.dialog.previousEntryOpacity` (0–1) tunes the fade on entries above the cursor (default 0.5) | `DialogEntriesView.setPreviousEntryOpacity` |
| Conversation-history cap | `save.maxHistoryEntries` sets the sliding-window size for retained conversations (default 1000) | `DialogHistory.setMaxConversations`, `ApplicationResourceConfig.saveField` |
| Startup window size + bounds | `window: { defaultWidth, defaultHeight, minWidth, maxWidth, minHeight, maxHeight }` set the initial window size and the clamp range (defaults 1280×720 within 640–3840 × 480–2160) | `PreferencesService.setWindowSizeBounds`, `ApplicationResourceConfig.windowField` |
| Font-scale clamp range | `ui.fontScaleMin` / `ui.fontScaleMax` widen/narrow the global Text-size accessibility range (default 0.75–2.0) | `PreferencesService.setFontScaleBounds`, `ApplicationResourceConfig.uiField` |
| SVG background min raster | `display.svgBackgroundMinRaster.{width,height}` sets the minimum rasterisation size for SVG backgrounds (default 1920×1080) | `ScreenShell.setBackgroundSvgRasterMinSize`, `ApplicationResourceConfig.displayField` |
| Dialog / save corner radii | Popup card/input/button + save block/tile/page-chip/thumbnail corner radii moved from inline Java to theme CSS (`.dialog-message-card/-input/-button`, `.save-grid-block/-tile/-page-chip/-thumb-placeholder`) — mod-overridable via custom theme CSS | `UiTheme` stylesheet template, `DialogMessages`, `SaveScreen` |
| Save tile sizes | `save.gridThumbnailWidth/Height` + `save.listThumbnailWidth/Height` size the grid tiles (default 350×197) and list-row thumbnails (default 96×54) | `SaveScreen.setThumbnailSizes`, `ApplicationResourceConfig.saveField` |
| Persisted thumbnail encoding | `save.thumbnailWidth/Height` (default ~1.4× the grid tile size, so it tracks `gridThumbnail*`) + `save.thumbnailJpegQuality` (default 0.85) control the saved JPEG resolution/quality | `SaveLoadService.setThumbnailEncoding`, `SaveScreen.gridThumbnailWidth/Height`, `BootstrapService` (supersample derive) |
| Screen spacing / insets | `ui.spacing.{body,outer,panel,footer}` retune the body gap / outer margin / panel padding / footer gap (defaults 12 / 16 / 16 / 14) | `ScreenShell.setSpacing`, `ApplicationResourceConfig.uiSpacingField` |
| Footer rest/hover opacity | `footer.restOpacity` / `footer.hoverOpacity` set the footer opacity at rest vs hover (defaults 0.5 / 1.0) | `ScreenShell.setFooterOpacity`, `ApplicationResourceConfig.footerStyle` |
| Kinetic text-effect durations | `text.kineticEffects.{pulse,float,shake}` set the inline `[kinetic=…]` animation durations in ms (defaults 650 / 900 / 120) | `JavaFxRichTextRenderer.setKineticEffectDurations`, `ApplicationResourceConfig.textKineticField` |

Tests: `ConfiguredFontsTest`, `ResourceOverridesTest`, `FontResourcesTest`, `UiThemeTest`
(custom-palette cases), `ApplicationResourceConfigTest` (`uiDialogFieldsParse`,
`saveMaxHistoryEntriesParses`, `uiFontScaleBoundsParse`, `windowSizingParses`,
`displaySvgBackgroundMinRasterParses`, `saveFieldsParse`, `uiSpacingParses`,
`footerOpacityFieldsParseAsNumbers`, `textKineticEffectDurationsParse`), `DialogHistoryTest`
(`configuredCapTrimsToTheConfiguredSlidingWindow`), `PreferencesServiceTest`
(`configuredWindowBoundsDriveDefaultsAndClamps`, `configuredFontScaleBoundsChangeClamping`)
(engine); `GameApplicationThemeStylesheetTest` (AltLife).

---

## Backlog

| # | Item | Priority | Notes |
|---|------|----------|-------|
| 1 | [Config-driven map grid geometry](#1-config-driven-map-grid-geometry) | Medium | Hex cell size/orientation are code constants today |
| 2 | [Config-driven building placement](#2-config-driven-building-placement) | Medium | Placement ranges/bounds are code; layout is runtime-generated |
| 3 | [Raster building-hex icons](#3-raster-building-hex-icons) | Low | Building-hex overrides must stay SVG (recolour rewrites markup) |
| 4 | [Generalize the hex/grid map into the engine](#4-generalize-grid-map-into-the-engine) | Large | Whole town-map (hex grid, placement, movement) is AltLife-only; engine only has hotspot maps |

_(Items 4–6 — auto-save toggle on Preferences, first-class config fields, per-icon repoint ids — are complete; see the Done table above.)_

---

### 1. Config-driven map grid geometry

**What:** Move the hex-grid cell size / orientation (currently constants in `AltLifeMapScreen`,
derived from the authoring SVG) into a `map.json`-style setup file so a mod can re-grid a custom
map background without code.

**Acceptance:** hex width/height fractions, orientation, and the reserved bottom-strip ratio read
from config with the current constants as defaults; existing map renders unchanged when no config
is supplied.

> Likely folds into #4 (the engine grid-map primitive) — do it there if #4 is taken on.

---

### 2. Config-driven building placement

**What:** Expose `AltLifeMapLayout`'s placement bounds + "near" ranges (`DORM_NEAR_UNIVERSITY_RANGE`,
random col/row bounds, etc.) as setup data so a mod can change the town layout rules.

**Acceptance:** placement rules read from config with current constants as defaults; save format
unchanged; new-game generation honours the configured ranges.

> Likely folds into #4 (the engine grid-map primitive) — do it there if #4 is taken on.

---

### 3. Raster building-hex icons

**What:** Let a building-hex icon override be a raster image (png/jpg) in addition to SVG. Today
the recolour pipeline rewrites SVG markup, so a raster override would break.

**Acceptance:** if the resolved building-hex override is a bitmap, skip recolour and render it
as-authored; SVG overrides keep recolour. Mirrors the footer-icon SVG/raster split.

---

### 4. Generalize the grid map into the engine

**What:** Today novlfx's only map primitive is the **hotspot map** (`com.eb.javafx.scene` —
`HotspotMapDefinition` / `HotspotDefinition` / `HotspotMapRegistry`: a background image with
condition-gated, fractional-bounds clickable regions that jump scenes, plus
`MapTextDefinition` for per-map text). The entire **hex / grid town map** — `AltLifeHexGrid`
(hex math), `AltLifeMapLayout` (building placement, save-persisted), `AltLifeMapScreen`
(rendering, hover cursor, movement, room/people panels) — lives in AltLife.

Promote a reusable **grid-map** primitive into the engine alongside hotspot maps so any game gets
a data-defined tiled/hex map for free, with the geometry (#1) and placement rules (#2) as its
config surface. This subsumes backlog #1 and #2 (they become the config schema of the engine
primitive rather than AltLife-local externalizations).

**What's needed (sketch):**
- `GridMapDefinition` — grid kind (square/hex + orientation), cell-size model, bounds, reserved
  margins; loaded from JSON via a registry like `HotspotMapRegistry`.
- `GridCoord` / grid-math helper (generalize `AltLifeHexGrid`: offset/axial coords, distance,
  neighbours).
- `GridPlacementRules` — declarative "near X within N", random bounds (generalize
  `AltLifeMapLayout`'s `*_NEAR_*` ranges) with a seeded, save-snapshotted layout.
- A reusable render/interaction view-model (background + grid overlay + per-cell entities +
  cursor) that a host screen draws — mirroring how `HotspotMapViewModel` is host-rendered.
- AltLife's map screen reduced to: register a `GridMapDefinition`, supply building entities,
  render via the engine view-model.

**Acceptance:** an engine grid-map can be registered + rendered by a host with no AltLife code;
AltLife's town map runs on the engine primitive with unchanged behaviour and save compatibility;
geometry + placement are pure config (closing #1/#2).

**Priority:** Large / longer-term — this is an engine feature, not a setup-only change, so it sits
beyond the "setup modding" scope of this doc but is recorded here as the natural home for #1/#2.
