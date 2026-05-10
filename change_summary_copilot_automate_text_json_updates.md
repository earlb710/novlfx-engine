Runtime UI localization update

- Added bundled runtime screen JSON and sibling `_text.json` resources for reusable route screens.
- Added `ScreenTextResources` to load screen titles/text and resolve simple text bindings from sidecar resources.
- Added `sys_message`, footer shortcut display, theme family, theme variant, and volume level system code tables.
- Updated runtime UI screens and footer helpers to read player-facing labels/messages from screen text resources or system code tables.
- Documented the screen-text/code-table/sys-message localization split and added focused tests for screen text and system code table behavior.
