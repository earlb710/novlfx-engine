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

18. Added `BlockBackgroundImageTestScreen`, a manual JavaFX test screen that renders a 50% transparent `circle-background.svg` behind content and two block sections using 50% transparent `circle2-background.svg` backgrounds.
19. Added `BlockBackgroundImageTestScreenTest` to verify the layout model metadata and expose the new manual launcher entry from the test screen app.
20. Updated `BlockBackgroundImageTestScreen` so each block now includes preview buttons, uses white in-block text, and applies a thicker, rounder border treatment.
21. Expanded `BlockBackgroundImageTestScreenTest` to verify the richer block metadata and that the rendered layout now includes block action buttons.
22. Kept the block background image demo on the `circle-background.svg` screen backdrop and expanded each block with extra explanatory text lines.
23. Updated `ScreenLayoutRenderer` and `BlockBackgroundImageTestScreenTest` so block background images are clipped to the configured rounded border shape instead of drawing past the curved edges.
24. Updated `ScreenLayoutRenderer` so section background image clips are recalculated during layout, keeping rounded and pill border clipping aligned when blocks resize.
25. Expanded `BlockBackgroundImageTestScreenTest` to resize the rendered root and verify the block background clip keeps matching the block bounds after layout changes.
26. Moved block background image clipping onto the rendered section container so the clip follows the same node that owns the rounded/pill border during resize.
27. Made the block background image test screen root and content panel transparent so the configured `circle-background.svg` screen background remains visible behind the layout.
28. Reworked section background-image rendering so borders stay visible by drawing the styled border on an outer wrapper and clipping an inset interior surface instead of clipping the border-owning node.
29. Changed the block background image demo blocks from pill corners to rounded corners and expanded the focused test to verify unclipped border wrappers and non-pill rounded clips.
30. Added `backgroundImagePlacement` support for block background images in `ScreenLayoutRenderer` and the screen designer, with `fixed top left`, `fixed center`, `fixed bottom right`, and `stretch to fit` options.
31. Updated `docs/USER_MANUAL.md` with a screen-plus-block background image example and documented block background image transparency and placement metadata.
