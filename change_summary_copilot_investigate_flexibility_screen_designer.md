## Screen designer workflow implementation

- Added a docked JavaFX live preview to the Swing screen designer and refreshed it automatically after property application, create/load/save actions, and preview default edits.
- Exposed block background image placement and item action event/value metadata as typed editor fields while preserving advanced metadata editing.
- Added generic starter templates, field guidance, duplicate/move/copy/paste context actions, scoped validation display, and a toolbar action to jump to the first validation issue.
- Updated focused screen designer tests and user manual documentation for the new workflow.

- Moved the docked screen designer preview into a dedicated right-side third column instead of a bottom split.
- Increased the default screen designer window to 1560x988 so the right-docked preview has more working space.
- Added inline screen designer validation for invalid visual metadata, field-level hints, and tree node issue counts/tooltips.
- Promoted item hover/pressed background metadata into typed screen designer controls and renamed raw metadata rows to Advanced metadata with guidance.
- Let screen designer property inputs shrink and grow with available width by applying a 10-character minimum size to text-based inputs and editable combo boxes.
