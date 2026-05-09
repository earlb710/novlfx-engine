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
Updated `PreferencesFooterTestScreenTest`'s manual launcher to build a real `SceneRouter` context so the footer preferences action opens the actual preferences route.
Added a focused preferences-footer routing test that verifies the footer preferences control navigates to the routed preferences screen when a display is available.
Reworked preferences theme changes to rebuild the preferences scene after saving a new theme so refreshed controls and SVG button artwork use the newly selected palette immediately.
Extended the generated/default JavaFX theme stylesheet with explicit combo-box and popup list colors plus core selection/looked-up color tokens to avoid the popup CSS warnings during preference changes.
Expanded `UiThemeTest` to cover the new themed selection and combo-box stylesheet rules.
23. Updated `PreferencesSummaryScreen` so the preferences footer keeps only the preferences icon enabled, clicking it closes back to the main menu, and Ctrl+P now mirrors that close behavior while the preferences screen is open.
24. Added `PreferencesSummaryScreenTest` coverage for the new preferences-screen footer enablement, tooltip, shortcut, and close-navigation behavior.
25. Grouped the preferences audio controls into a dedicated `Audio` card block and the theme/footer display controls into a dedicated `Visual` card block.
26. Extended `PreferencesSummaryScreenTest` to verify the new grouped card block structure while keeping the existing preferences footer behavior coverage.
27. Updated footer tooltip installation so footer labels keep real JavaFX tooltips without relying on disabled label controls, allowing disabled-looking footer options to remain hoverable.
28. Updated preferences theme changes to rebuild the preferences scene at the current scene size so changing theme colors does not resize the window.
29. Changed default theme text colors so pastel variants use black label text and dark variants use white label text, and expanded tests to cover the new tooltip, sizing, and text color behavior.
30. Added a default-on `new` checkbox before the manual filter on `TestScreenApplication` so the test tree shows only new tests until the filter is cleared.
31. Added shared test-screen filtering logic to combine the new-test and manual-only filters, and expanded `TestScreenApplicationTest` to cover the filter combinations and checkbox defaults.
32. Added a shared JavaFX tooltip factory with a shorter show delay and switched scene choice tooltips plus footer/preferences tooltips to use it.

