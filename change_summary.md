Added configurable SVG background opacity and canvas fill support to `ScreenShell.backgroundSvg(...)` while preserving the existing default overload.
Updated `SvgBackgroundTestScreen` to expose transparency and canvas color controls for manual verification.
Expanded `ScreenShellTest` and `SvgBackgroundTestScreenTest`, including refreshing the packaged circle background assertions to match the current SVG asset.
Documented the new SVG background opacity and canvas color support in `docs/USER_MANUAL.md`, including the updated manual test screen behavior.
