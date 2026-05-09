Added configurable SVG background opacity and canvas fill support to `ScreenShell.backgroundSvg(...)` while preserving the existing default overload.
Updated `SvgBackgroundTestScreen` to expose transparency and canvas color controls for manual verification.
Expanded `ScreenShellTest` and `SvgBackgroundTestScreenTest`, including refreshing the packaged circle background assertions to match the current SVG asset.
Documented the new SVG background opacity and canvas color support in `docs/USER_MANUAL.md`, including the updated manual test screen behavior.

Added screen designer support for editing block/item `styleClass` values plus extra non-exposed metadata entries through the properties panel and block/item dialogs.
Added block background image and background image transparency metadata support to the designer, with file browsing support that stores chosen files as file URIs.
Extended `ScreenLayoutRenderer` to render block background images from classpath resources, filesystem paths, and file/URL sources, including SVG backgrounds rasterized through `VectorImage`.
Updated tests, sample screen JSON, and the user manual to cover the new metadata editing and block background image capabilities.

Simplified `PreferencesSummaryScreen` so it only shows editable preferences: master/music/sound audio, theme color, and footer display mode.
Removed inline save buttons from the preferences route and switched those controls to immediate persistence/application, leaving a single Close button at the bottom.
Updated preferences-related tests, manual footer-test copy, and the user manual to reflect the narrower editable preferences surface.

Updated `PreferencesFooterTestScreenTest` to handle an already-initialized JavaFX toolkit when launched from the manual test app.
Added shared `TestUiScreenSize` limits and capped JavaFX test/support scenes to 800x600.
Capped Swing-based test/support windows and the screen-designer preview/test harness sizing so test UI windows stay within 800x600.

Added default app, preferences-screen, and save/load-screen background color/image/transparency fields to `ApplicationResourceConfig`, including JSON parsing/serialization, immutable update helpers, and bundled config defaults.
Updated the Default App Values management screen coverage so the new config fields appear in the editable Application Values list.
Documented the new config JSON fields and refreshed the startup demo config to show route-level background defaults.
Polished the Default App Values Application Values tab with friendly field labels, browse buttons for file/path fields, color picker buttons for color fields, and transparency labels that show the supported 0-1 range.
Removed the “changes apply only to this management screen” wording from the tab intro text in the Default App Values editor.
Wired routed screen scene creation to actually use the new configured background defaults: generic routed scenes now use the app background settings, while the preferences and save/load generators use their dedicated background color/image/transparency properties.
Added focused coverage for configured screen background wrapping and route-context background selection.
