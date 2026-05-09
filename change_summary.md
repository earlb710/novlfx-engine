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
