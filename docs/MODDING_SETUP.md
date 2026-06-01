# novlfx Modding Guide — Setup Changes

> **Scope:** This guide covers **setup / configuration modding only** — changing engine
> behavior and look-and-feel by editing **data files, JSON, CSS, and stored preferences**.
> It does **not** cover code-level modding (writing new content modules, adding fonts, new
> routes, custom screens). Those are a separate topic and out of scope for this document.
>
> **Reference implementation:** examples use the bundled **AltLife** game. Paths under
> `AltLife/...` are game-specific; the *mechanisms* (classes, JSON keys, CSS classes,
> preference keys) are engine-generic and apply to any novlfx game.

---

## 0. The four setup surfaces

Everything in this guide is one of four kinds of change:

| # | Surface | What you edit | Affects |
|---|---------|---------------|---------|
| 1 | **App config JSON** (`config.json`) | resource roots, resource id→path map, default backgrounds | which assets load, fallback backgrounds |
| 2 | **Theme & CSS** | `ui.themeFamily` / `ui.themeVariant` preferences + the game's `*-theme.css` | colours, fonts, panel/dialog styling |
| 3 | **Content/text JSON** | `default-application-values.json`, screen `*_text.json`, code-tables JSON, screen layout `*.json` | titles, labels, screen layout/metadata |
| 4 | **Preferences** | per-user stored values (window, audio, theme, save behavior…) | runtime behavior, set via in-game screens |

> ✅ **Adding a new font** and **pointing the app at a different CSS file** are now both pure
> setup — done through entries in the `config.json` `resources` map (§2.2, §2.6). No Java edit
> and no font-whitelist change required.

---

## 1. App config JSON (`config.json`)

**Engine class:** `com.eb.javafx.bootstrap.ApplicationResourceConfig`
**AltLife file:** `AltLife/config.json` — at the **application root** (the parent of `javafx/`),
**not** under `src/main/resources`.

**How it loads:** at boot, `GameApplication.createBootstrapOptions(applicationRoot)` calls
`BootstrapOptions.fromConfig(applicationRoot.resolve("config.json"))`. The engine layers this
app config **over** its own bundled `/config.json` to build the resource registry. The path
constant is `AltLifeCodeTableContentModule.RESOURCE_CONFIG_PATH = "config.json"`.

### 1.1 Full schema

```json
{
  "debug": true,

  "resourceRoots": {
    "images":  ["classpath:/com/altlife/javafx/images", "classpath:/com/altlife/javafx/json/display"],
    "support": ["javafx/json/code-tables", "javafx/json/config", "javafx/json/location",
                "classpath:/com/altlife/javafx/json/conversations", "classpath:/com/altlife/javafx/json/scenes"],
    "ui": ["classpath:/com/altlife/javafx/json/screens"]
  },

  "defaultAppBackgroundColor": "#000000",
  "defaultAppBackgroundImage": "com/altlife/javafx/images/background/circle-background.svg",
  "defaultAppBackgroundImageTransparency": "0.6",

  "defaultPreferencesScreenBackgroundColor": "#000000",
  "defaultPreferencesScreenBackgroundImage": "com/altlife/javafx/images/background/circle-background.svg",
  "defaultPreferencesScreenBackgroundImageTransparency": "0.6",

  "defaultSaveLoadScreenBackgroundColor": "#000000",
  "defaultSaveLoadScreenBackgroundImage": "com/altlife/javafx/images/background/circle-background.svg",
  "defaultSaveLoadScreenBackgroundImageTransparency": "0.6",

  "resources": {
    "uiTheme": "javafx/src/main/resources/com/altlife/javafx/altlife-theme.css",
    "altlife.defaultApplicationValues": "javafx/json/config/default-application-values.json",
    "altlife.locationContent": "javafx/json/location/location-content.json"
  }
}
```

### 1.2 Key reference

| Key | Type | Meaning |
|-----|------|---------|
| `debug` | bool | Engine debug flag. |
| `defaultAppBackgroundColor` | hex string | Fallback background colour for the main app shell. |
| `defaultAppBackgroundImage` | resource path | Fallback background image (classpath-resolved, no leading slash). `.svg` is rasterised at 1920×1080; `.png`/`.jpg` load directly. |
| `defaultAppBackgroundImageTransparency` | `"0.0"`–`"1.0"` | Image opacity (string). Out-of-range falls back to `0.6`. |
| `defaultPreferencesScreenBackground*` | same | Same three fields for the **Preferences** screen. |
| `defaultSaveLoadScreenBackground*` | same | Same three fields for the **Save/Load** screen. |
| `resources` | id→path map | Named overrides. Paths resolved **relative to `config.json`** (or `classpath:`-prefixed). Reserved id conventions: **`uiTheme`** = theme CSS file (§2.2); **`themePalette`** = custom colour-palette JSON (§2.7); any **`font.*`** id = a font to register (§2.4); **`assetOverrideRoot`** = icon/image override directory (§2.6); **`windowTitle`** = window title string (§1.4); **`appIcon`** = window/taskbar icon image (§1.4); **`screenBackground.<route>.<field>`** = per-screen background (authored via the top-level `screenBackgrounds` map, §1.6). |
| `resourceRoots` | category→path list | Asset search roots, layered **before** the engine's bundled roots. Categories: `images`, `support`, `ui`. Specs are filesystem-relative (`javafx/json/config`) or `classpath:/...`. |

### 1.3 Common mods

- **Change the app/menu background** → edit `defaultAppBackgroundColor` / `…Image` / `…ImageTransparency`. Drop your image somewhere on a configured `images` root and reference it by classpath path.
- **Swap a data file without touching code** → repoint a `resources` id (e.g. `altlife.locationContent`) at a different JSON file.
- **Override bundled JSON/images** → prepend a directory to the matching `resourceRoots` category; first match wins.

### 1.4 Window title & app icon

Both come from `config.json` `resources` and are applied generically at boot
(`BootstrapService.applyWindowChrome`), so any host's primary window picks them up:

```json
"resources": {
  "windowTitle": "My Game",
  "appIcon": "com/mygame/images/app-icon.png"
}
```

- **`windowTitle`** — the stage title string. Falls back to the engine default (`eb JavaFX`) when unset.
- **`appIcon`** — the window / taskbar icon. Resolves as `classpath:<path>`, an on-disk file under the application root, or a bare classpath resource. Any JavaFX-decodable image (`.png`/`.jpg`/`.gif`). Skipped silently if missing.

### 1.5 First-class field shortcuts

The reserved `resources` ids can also be written as **top-level `config.json` fields** — cleaner
and validated at parse time. They're folded into the `resources` map (an explicit top-level value
overrides a same-id `resources` entry), so the map convention still works for back-compat.

```json
{
  "windowTitle": "My Game",
  "appIcon": "com/mygame/images/app-icon.png",
  "assetOverrideRoot": "mods/assets",
  "uiTheme": "mods/theme.css",
  "themePalette": "mods/palette.json",
  "fonts": ["mods/fonts/Title.ttf", "classpath:/com/mygame/fonts/Body.otf"]
}
```

The `fonts` **array** is equivalent to a set of `resources.font.*` entries (§2.4). Everything
else maps 1:1 to its same-named `resources` id.

### 1.6 Per-screen backgrounds (any system screen)

Beyond the three fixed defaults in §1.1 (app / preferences / save-load), **any system screen can
get its own background** via a top-level `screenBackgrounds` map keyed by **route id**, each with
`color` / `image` / `transparency`. Unset fields fall back to that screen's existing default
(preferences/save-load use their dedicated defaults; everything else uses the app default).

```json
"screenBackgrounds": {
  "main-menu":   { "color": "#0b0f18", "image": "mods/menu-bg.png", "transparency": "0.5" },
  "preferences": { "color": "#101418" },
  "save-load":   { "image": "mods/save-bg.jpg", "transparency": "0.4" }
}
```

- **Keys are route ids** — `main-menu`, `preferences`, `save-load`, `conversation-history`,
  `display-bindings`, `gallery`, … (the constants in `SceneRouter`). Any screen built through
  `RouteContext.themedScene` is covered automatically — the engine resolves the background for the
  route being constructed.
- **Fields:** `color` (hex), `image` (resource path), `transparency` (`0.0`–`1.0` string). Long
  forms (`backgroundColor` / `backgroundImage` / `backgroundImageTransparency`) are also accepted.
  Any subset; omitted fields use the fallback.
- **Precedence** per field: `screenBackgrounds.<route>` → the screen's dedicated default
  (`defaultPreferencesScreen*` / `defaultSaveLoadScreen*`) → the app default.
- Wiring: `RouteContext.themedScene/themedPreferencesScene/themedSaveLoadScene` →
  `ApplicationResourceConfig.screenBackground{Color,Image,ImageTransparency}(routeId)`.

> Game-specific gameplay screens that paint their own background (e.g. AltLife's scene screens via
> `AltLifeSceneStyles`) are governed by their own background system (§2.5 / screen-layout JSON
> §3.3), not this engine map.

---

## 2. Theme & look-and-feel

### 2.1 Theme model — `UiTheme`

**Class:** `com.eb.javafx.ui.UiTheme`. The theme is **not** a file — it is generated at runtime
from two preference axes plus an accessibility flag, and emitted as a temporary CSS file
(`novlfx-theme-*.css`) exposed via `UiTheme.stylesheet()`.

| Axis | Preference key | Built-in values |
|------|----------------|-----------------|
| Family | `ui.themeFamily` | `ocean`, `forest`, `sunset`, `violet`, `crimson` |
| Variant | `ui.themeVariant` | `dark`, `light-pastel` |
| High contrast | `accessibility.highContrast` | `true` / `false` (replaces the palette) |

`UiTheme.initialize(PreferencesService)` reads `fontFamily`, `fontScale`, `highContrast`,
`reducedMotion`, `themeFamily`, `themeVariant` and picks a palette. Palette hex values live in
`UiTheme.ThemePalette.forSelection(...)` (e.g. forest/dark accent `#8dd7a8`, ocean/dark `#66c1e0`).

**To change the palette (no code):** set `ui.themeFamily` / `ui.themeVariant` /
`accessibility.highContrast`. The intended path is the in-game **Startup Options / Preferences**
screen, which persists these. A game may seed defaults at boot — AltLife's
`applyAltLifeDefaultThemePreferences()` writes `forest` / `dark` **only if unset** (it never
overrides a player's choice).

### 2.2 CSS layer stack

Three stylesheets stack on every scene, in order (later wins):

1. **Engine generated theme** — `uiTheme.stylesheet()` (palette-driven temp CSS), added in `RouteContext.themedScene`.
2. **Game theme override** — AltLife's `altlife-theme.css`, added by `AltLifeSceneStyles.addAltLifeStylesheet(Scene)`.
   File: `AltLife/javafx/src/main/resources/com/altlife/javafx/altlife-theme.css`.
3. **Generated panel-gradient CSS** — a small derived temp sheet for button/panel gradients.

The game theme CSS aliases the engine palette into its own variables in its `.root` block, e.g.:

```css
.root {
    -altlife-accent: -fx-accent;
    -altlife-panel-background: -fx-control-inner-background-alt;
    -altlife-panel-border: -fx-focus-color;
    -altlife-text-primary: -fx-text-background-color;
}
```

So **restyling the game = editing `altlife-theme.css`** (its `-altlife-*` variables and the style
classes below), **or** pointing the app at a different CSS file entirely via config — see below.

**Swap the theme CSS via config (`resources.uiTheme`).** At boot the engine resolves the
`resources` entry **`uiTheme`** against the application root; if it resolves to a **readable file
on disk**, that file is used as the game's stylesheet layer instead of the bundled
`altlife-theme.css`. Otherwise the bundled stylesheet is used. So:

```json
"resources": {
  "uiTheme": "mods/my-theme.css"
}
```

drops a fully custom theme in `AltLife/mods/my-theme.css` (path relative to `config.json`) with
**no rebuild** — edit the file and relaunch.

- Wiring: `GameApplication.applyConfiguredThemeStylesheet(BootContext)` →
  `AltLifeSceneStyles.setOverrideStylesheetUrl(...)`; a missing/unreadable path silently falls
  back to the bundled CSS.
- AltLife's shipped config points `uiTheme` at the in-repo source CSS, so during development
  edits to that file apply on the next launch.

### 2.3 Safe CSS style-class hooks

These classes are defined by the engine template and the game CSS and are safe to restyle:

- **Screens:** `.screen-root`, `.screen-panel`, `.screen-title`, `.screen-subtitle`, `.screen-text`, `.screen-text-highlight`, `.screen-value`, `.screen-footer-bar`, `.screen-footer-option`, `.screen-footer-option-disabled`, `.screen-footer-option-active`, `.screen-footer-compact`
- **Scene panels:** `.scene-status-panel`, `.scene-dialogue-panel`, `.scene-choices-panel`, `.scene-effects-panel`, `.scene-choice-button`, `.scene-choice-state`, `.scene-effect-preview`
- **Layout system:** `.layout-content`, `.layout-main-content`, `.layout-sidebar`, `.layout-card`, `.layout-section`, `.layout-section-title`, `.layout-section-row`, `.layout-titled-panel`, `.layout-hud-overlay`, `.layout-dialogue`, `.layout-menu`, `.layout-form`, `.layout-two-column`, `.layout-column`, `.layout-subtitle`, `.layout-footer`, `.layout-action-row`, `.layout-primary-action`, `.layout-secondary-action`, `.layout-title`, `.layout-value`, `.layout-text-highlight`
- **Dialog block:** `.dialog-entries-view`, `.dialog-entries-container`, `.dialog-entry`, `.dialog-entry-current`, `.dialog-entry-previous`, `.dialog-entry-speaker`, `.dialog-entry-body`, `.dialog-entry-say`, `.dialog-entry-shout`, `.dialog-entry-whisper`, `.dialog-entry-think`, `.dialog-entry-comment`, `.dialog-entry-divider`, `.dialog-entry-divider-line`, `.dialog-entry-divider-label`
- **Conversation history:** `.conversation-history-rows`, `.conversation-history-speaker`, `.conversation-history-message`
- **Save screen / scrollbars:** `.save-screen-tabs` (+ `.tab`, `.tab:selected`, `.tab-label`), `.engine-slim-scrollbar`
- **Error screen** (literal colours, theme-independent): `.error-screen`, `.error-screen-title`, `.error-screen-message`, `.error-screen-details`, `.error-screen-*-button`
- **Tooltip:** `.tooltip` (global `-fx-show-delay`)
- **Game-specific (AltLife):** `.altlife-bevel-button`, `.altlife-button-row`, `.stat-budget-row`, `.status-log-panel`, `.main-character-section-title`, `.startup-option-heading`

> 💡 **Specificity tip for the dialog block:** the message *body* is a `Text` node inside a
> `TextFlow`, which takes its size from the **container** class (`.dialog-entry-body` /
> `.dialog-entry-say`), not from a `.text` selector. To resize message bodies, style the
> container, not `.text`.

### 2.4 Fonts

**Class:** `com.eb.javafx.util.FontResources`. Bundled fonts live at the classpath root
`/com/eb/javafx/fonts` (e.g. `Nasalization Rg.otf`, `Dommage.ttf`, `Crimson-Bold.ttf`,
`OpenDyslexic3-Regular.ttf`). CSS references them by **family name**, not filename:

```css
.screen-title { -fx-font-family: "Nasalization"; }
```

- The base UI font family comes from the `ui.fontFamily` preference (default `System`); `ui.fontScale` (0.75–2.0) scales all text.
- **Use a different bundled font:** set `ui.fontFamily`, or reference a packaged family by name in the game CSS.

**Add a brand-new font via config (`resources.font.*`).** Any entry in the `resources` map
whose id starts with **`font.`** is registered at boot (engine `ConfiguredFonts`, called from
`BootstrapService.boot`), so a game/mod can ship its own fonts with no Java change and no
whitelist edit:

```json
"resources": {
  "font.title": "mods/fonts/MyTitleFont.ttf",
  "font.body":  "classpath:/com/mygame/fonts/Body.otf"
}
```

Each value is resolved in order: a `classpath:`-prefixed spec loads from the classpath; a path
that resolves to an existing file under the application root loads from disk (drop a real
`.ttf`/`.otf` next to `config.json`); otherwise it's tried as a bare classpath resource. Once
registered, reference the font's **family name** (not the filename) in CSS via
`-fx-font-family`. Loading is best-effort — a bad entry logs and is skipped, never aborting boot.

### 2.5 Per-scene gameplay backgrounds

In-game scene backgrounds are separate from the §1 defaults: they come from each screen's
**layout JSON metadata** (`screenBackgroundImage`, `screenBackgroundImageTransparency`,
`backgroundColor`) under `…/json/screens/*.json` (see §3.3), falling back to the default
background when unset.

### 2.6 Replacing icons & images (asset override root)

**Class:** `com.eb.javafx.util.ResourceOverrides`. Set the **`resources.assetOverrideRoot`**
entry to a directory; any file you place there whose path **mirrors a bundled resource path**
is used instead of the bundled copy — no rebuild. Wired generically at boot
(`BootstrapService.boot` → `ResourceOverrides.setOverrideRoot`).

```json
"resources": { "assetOverrideRoot": "mods/assets" }
```

Then mirror the original path under that folder:

| To replace… | Drop a file at… |
|-------------|-----------------|
| The energy stat icon | `mods/assets/com/altlife/javafx/images/stats/stat-energy-positive.svg` |
| The save footer icon | `mods/assets/<footer-icon-dir>/footer-save.svg` |
| A background image | `mods/assets/com/altlife/javafx/images/background/<name>.svg` |

- **Any image type — not just SVG.** Overrides are format-aware: replace a bundled `.svg` icon
  with a **`.png` / `.jpg` / `.jpeg` / `.gif` / `.bmp`** (or another SVG). You may keep the
  original base name and just change the extension — e.g. drop `…/stat-energy-positive.png` to
  override the bundled `…/stat-energy-positive.svg`. The loader resolves by base name across the
  known image extensions and picks the right decoder (SVG → Batik rasteriser, bitmaps → JavaFX
  image loader). An exact-extension match wins over an alternate one.
  - Footer icons keep auto-recolour only for the **vector (SVG)** source; a raster footer
    override is shown as-authored (no tint).
- **Button shape & artwork** — the engine button SVGs `/com/eb/javafx/images/svg/button-pill-long.svg`
  and `button-bevel.svg` (`ButtonVisuals`) are override-aware too, so a mod can change the button
  outline/skin. These must stay **SVG** (the shape path + gradient are parsed from the markup) and
  are resolved once at startup, so an override takes effect on next launch. Their colour follows
  the theme palette (`buttonGradientStart/Mid/End`, §2.7) — override the SVG only to change *shape*.
- **Coverage:** the engine footer/screen resolver (`ScreenShell.loadFooterIcon` /
  `resolveResource`); the AltLife loaders (`AltLifeSceneStyles.loadIcon` — header stat icons,
  status-log icons, stat-icon badges — and `loadBackgroundImage`); and the **town-map artwork**
  (`AltLifeMapScreen`: the base map `…/images/map/map1.png`, the grid overlay
  `…/images/map/map1-grid.png`, the building-hex icons, and character icons) all consult the
  override root first, then fall back to the classpath. Partial override folders only replace the
  files they contain.
  - The map base + grid + character icons accept **any image type** (alternate extension OK); the
    building-hex icons must stay **SVG** (their colours are recoloured by rewriting SVG markup).
- This **replaces existing** icons/images. *Adding a brand-new* icon still needs a referencing
  call site (a new footer option, a new stat row, …), which is content/code — out of scope here.
- Path-escape (`../`) out of the override root is rejected.

**Repoint a single icon without mirroring its path** — a `resources` entry keyed
`icon:<originalPath>` maps one resource to a replacement (an override-root file *or* a bundled
classpath resource), so you don't have to recreate the full directory tree for a one-off swap:

```json
"resources": {
  "icon:com/altlife/javafx/images/stats/stat-energy-positive.svg": "mods/energy.png"
}
```

The replacement may change format (the loader dispatches by the replacement's extension). All the
same loaders in the Coverage list honor these repoints.

> **Display/sprite images** (character art referenced by the display-content system) have a
> second, independent override path: they resolve through the config-layered `ResourceRegistry`
> (`resourceRoots.images`), where an app root listed before the library roots wins by
> same-relative-name. Use whichever fits — `assetOverrideRoot` for a drop-in mirror folder,
> `resourceRoots.images` for a whole alternate image tree.

### 2.7 Custom theme palette (colours)

The engine ships 5 theme families × 2 variants (§2.1) with fixed colours. To define your **own
colours** — retint the existing palette or build a new look — point `resources.themePalette` at a
JSON file. It's loaded before the theme initialises (`UiTheme.loadCustomPalette`) and feeds the
engine's generated stylesheet, so it affects the real `-fx-*` tokens (not just AltLife CSS).

```json
"resources": { "themePalette": "mods/my-palette.json" }
```

`mods/my-palette.json` (everything optional):

```json
{
  "baseFamily": "forest",
  "baseVariant": "dark",
  "colors": {
    "accentColor": "#ff8800",
    "labelText": "#ffffff",
    "screenPanelBackground": "rgba(20,20,28,0.9)",
    "buttonGradientStart": "#3a3a55",
    "footerIconColor": "#ffd54a"
  }
}
```

- **`baseFamily` / `baseVariant`** — the starting palette the overrides apply on top of. Omit to
  start from the player's currently selected family/variant.
- **`colors`** — override individual palette fields by name. Any subset; unknown keys are ignored.
  Field names: `rootBackground, screenGradientStart, screenGradientEnd, accentColor,
  screenPanelBackground, screenPanelBorder, footerBackground, footerText, sectionBorder,
  sectionText, textHighlight, textHighlightHover, valueText, layoutPanelBackground, labelText,
  buttonBackground, buttonText, buttonBorder, buttonHoverBackground, buttonHoverText,
  buttonHoverBorder, buttonPressedBackground, buttonPressedText, buttonPressedBorder,
  svgButtonText, svgButtonHoverText, buttonGradientStart, buttonGradientMid, buttonGradientEnd,
  fieldRoleColor, fieldRoleBackground, footerIconColor`.
- **High-contrast accessibility mode still wins** over a custom palette.
- Values are any JavaFX-CSS colour (`#rrggbb`, `rgba(...)`). A parse error logs and leaves the
  built-in palettes in effect.

This is the strongest "skin my game" hook — it changes the generated theme tokens that every
screen uses. Combine with §2.2 (custom CSS) for finer per-class tweaks.

---

## 3. Content & text JSON

### 3.1 Default application values (screen/scene titles)

**Class:** `com.altlife.javafx.AltLifeDefaultApplicationValues`
(resource id `altlife.defaultApplicationValues`).
**File:** `AltLife/javafx/json/config/default-application-values.json`.

Shape — JSON `values` override hardcoded defaults:

```json
{ "values": {
  "altlife.gameStart.title": "Alternative Life",
  "altlife.startupOptions.title": "Startup Options",
  "altlife.scene.contentWarning.title": "Content Warning",
  "altlife.mainCharacterGeneration.title": "Main Character Generation",
  "altlife.hud.status.title": "AltLife Status"
}}
```

Edit a value to retitle that screen/scene.

### 3.2 UI label text (`*_text.json` sidecars)

**Class:** `com.eb.javafx.ui.ScreenDesignJson`. Every screen design `foo.json` has a paired
`foo_text.json`; text refs in the layout resolve from it. Shape:

```json
{ "language": "en", "texts": { "screen.title": "Preferences", "item.close.label": "Close" } }
```

- Engine example: `com/eb/javafx/ui/screens/preferences_text.json` (`screen.title`, `item.master-volume.label`, `item.theme.label`, `item.text-speed.label`, `item.close.label`, …).
- AltLife examples under `…/json/screens/*_text.json` (e.g. `main-app-gameplay-layout_text.json` → `label.location`, `label.event`).

Override a label by editing its `texts` entry.

### 3.3 Screen layout JSON

**Class:** `com.eb.javafx.ui.ScreenDesignJson` → `ScreenDesignModel`. Files under
`…/json/screens/*.json`. Top-level shape:

```json
{ "id": "altlife-main-app-gameplay", "title": "AltLife Gameplay",
  "layoutType": "MAIN_APP_LAYOUT",
  "metadata": { "screenBackgroundImage": "…", "screenBackgroundImageTransparency": "0.6",
                "backgroundColor": "#000000", "storyDialogRatio": "0.80",
                "appLayoutOrientation": "vertical", "showFooter": "false",
                "storyInsets": "12, 16, 8, 16", "dialogInsets": "8, 16, 12, 16" },
  "blocks": [ … ] }
```

Configurable per screen without code: `title`, the `metadata` map (backgrounds, transparency,
layout ratios, orientation, footer visibility, insets, nested screen ids), and the `blocks`
array of UI items. Each screen has a matching `_text.json` (§3.2).

### 3.4 Code tables (enum labels)

**Class:** `com.eb.javafx.gamesupport.SystemCodeTables`. The engine bundles
`com/eb/javafx/gamesupport/system-code-tables.en.json`; games ship their own (AltLife:
`javafx/json/code-tables/category-code-tables.en.json`). Shape:

```json
{ "language": "en", "tables": [
  { "id": "theme-family", "title": "Theme Family",
    "codes": [ { "id": "forest", "title": "Forest", "sortOrder": 2, "tags": [] } ] } ] }
```

System table ids supply the human-readable labels for preference enums, e.g. `time-of-day`,
`theme-family`, `theme-variant`, `text-speed`, `volume-level`, `footer-shortcut-display`,
`footer-icon-display`, `conversation-line-types`, `scene-step-types`. Override a label by
editing a code's `title`.

---

## 4. Preferences (runtime behavior)

Per-user values stored via Java `Preferences` (not files) — `com.eb.javafx.prefs.PreferencesService`.
Players change these through the in-game **Preferences / Startup Options** screens; a game may
seed defaults at boot. Full set:

| Key | Controls | Default |
|-----|----------|---------|
| `window.width` / `window.height` | Window size | 1280 × 720 (clamped) |
| `window.fullscreen` | Fullscreen at launch | false |
| `ui.hudAlpha` / `ui.sayWindowAlpha` | HUD / say-window opacity | 1.0 |
| `ui.showPortrait` | Show character portrait | true |
| `ui.cheatsVisible` | Show cheat affordances | true |
| `ui.logStatChanges` | Log stat changes | false |
| `ui.footerShortcutDisplay` | Footer shortcut hint mode | `tooltip-only` |
| `ui.footerIconDisplay` | Footer icon/text mode | `icons-with-text` |
| `ui.fontFamily` / `ui.fontScale` | Base font + scale | `System` / 1.0 |
| `ui.themeFamily` / `ui.themeVariant` | Theme palette (§2.1) | `ocean` / `dark` |
| `ui.language` | UI language | `en` |
| `ui.textSpeed` | Text reveal speed | `normal` (slow 800ms / normal 400 / fast 200) |
| `accessibility.highContrast` | High-contrast palette | false |
| `accessibility.reducedMotion` | Reduce animation | false |
| `input.mode` | Input mode | `mouse` |
| `audio.masterVolume` / `musicVolume` / `soundVolume` / `voiceVolume` | Volumes | 1.0 |
| `audio.muteAll` | Master mute | false |
| `audio.voiceEnabled` | Voice playback | true |
| `audio.autoAdvanceOnVoiceEnd` | Auto-advance when voice ends | false |
| `save.autoSaveDaily` | End-of-day auto save (§5) | **true** |
| `save.viewMode` | Save screen grid/list | `grid` |
| `save.pageCount` | Save screen page count (persisted) | 1 |
| `save.selectedPage` | Last-viewed save page (persisted) | 1 |

> A game seeds boot defaults in its `start()` (e.g. AltLife sets theme family/variant only
> when unset). To change a default for a fresh profile, change that seed — but note that's a
> Java edit. Editing live values is done in-game.

---

## 5. Save / Quick-save / Auto-save behavior

The generic save engine lives in `com.eb.javafx.ui.QuickSaveActions` and the Save screen
(`SaveScreen`). The **setup knobs** are:

| Behavior | Setup knob |
|----------|-----------|
| End-of-day auto save on/off | `save.autoSaveDaily` preference (default **on** for new profiles); toggled by the **Preferences screen** (Save block) or the checkbox on the **Auto** tab of the Save screen. |
| Save-screen pages | `save.pageCount` (added via the "+" page chip, persisted) and `save.selectedPage` (last page viewed, persisted). |
| Save-screen view | `save.viewMode` (grid / list). |
| Save/Preferences/Save-Load backgrounds | `config.json` `defaultSaveLoadScreenBackground*` / `defaultPreferencesScreenBackground*` (§1). |

> Quick-save (15-slot rotating buffer) and auto-save (15-slot rotating buffer, fired by the
> host at end of day) are driven by code, not config; only the preference toggle and page
> counts above are setup-tunable.

---

## 6. Launch flags

| Property | Effect |
|----------|--------|
| `-Daltlife.startupRoute=<routeId>` | Override the startup route (e.g. `altlife-admin`, `altlife-story-editor`). Used by the `runAdminApp` / `runStoryEditor` / `run3dModelViewer` Gradle tasks. |

The engine itself reads only standard JVM properties (`user.home` for the save directory,
`java.io.tmpdir` for generated theme CSS). There are no `novlfx.*` system properties.

---

## 7. Quick recipes

| Goal | Do this |
|------|---------|
| Different menu background image | `config.json` → `defaultAppBackgroundImage` + `…Transparency` |
| **Background for a specific system screen** | `config.json` → `screenBackgrounds.<routeId>` = `{ color, image, transparency }` (§1.6) |
| Different colour theme (preset) | Preferences screen → Theme family/variant (writes `ui.themeFamily` / `ui.themeVariant`) |
| **Custom theme colours** | `config.json` → `resources.themePalette` = a palette JSON (§2.7) |
| **Set window title / app icon** | `config.json` → `resources.windowTitle` / `resources.appIcon` (§1.4) |
| High-contrast / reduced motion | Preferences → accessibility toggles |
| Restyle dialog text / panels | Edit `altlife-theme.css` (`-altlife-*` vars + the §2.3 classes) |
| Use a **completely custom theme CSS** | `config.json` → `resources.uiTheme` = path to your `.css` file |
| **Add a new font** | `config.json` → `resources.font.<name>` = path to your `.ttf`/`.otf`, then use its family name in CSS |
| **Replace an icon / image** | `config.json` → `resources.assetOverrideRoot` = a folder, then mirror the resource path under it (§2.6) |
| **Change the town-map background / grid** | asset override root → `…/images/map/map1.png` (base) and/or `…/images/map/map1-grid.png` (grid overlay). *Grid geometry/placement is code, not setup.* |
| **Change button shape / artwork** | asset override root → `com/eb/javafx/images/svg/button-pill-long.svg` and/or `button-bevel.svg` (must stay SVG). Button *colour* = theme palette (§2.7). |
| Bigger base font | `ui.fontScale`, or `ui.fontFamily` for a different bundled font |
| Retitle a screen | `default-application-values.json` → the screen's title value |
| Rename a button/label | the screen's `*_text.json` `texts` entry |
| Relabel a preference enum option | `…/json/code-tables/*.json` → the code's `title` |
| Adjust a screen's layout/background/insets | the screen's `…/json/screens/*.json` `metadata` |
| Turn end-of-day autosave off | Save screen → Auto tab → uncheck "Auto save daily" |
| Launch a dev tool surface | `-Daltlife.startupRoute=altlife-admin` |
