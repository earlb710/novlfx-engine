# Change summary: remove "Back to main menu" button from conversation history view

## Motivation

The conversation history view rendered a redundant "Back to main menu" button at the bottom of the screen. Navigation away from the history view is handled by the host application's footer/shell, so the inline button was unnecessary clutter.

## Changes

- `src/main/java/com/eb/javafx/ui/ConversationHistoryScreen.java`
  - `viewModel(String, GameState)` now constructs `ConversationHistoryViewModel` with an empty `actions` list instead of a single `MAIN_MENU_ROUTE` back action.
  - Dropped the now-unused `com.eb.javafx.routing.SceneRouter` import.
  - The `ConversationHistoryViewModel.actions()` field is preserved (still rendered by `createContent`) so application code can supply its own actions if needed; the engine just no longer injects a default one.
- `src/main/resources/com/eb/javafx/ui/screens/conversation-history_text.json`
  - Removed the unused `item.back.label` text key.
- `src/test/java/com/eb/javafx/ui/ConversationHistoryScreenTest.java`
  - `showsEmptyConversationHistory` now asserts `viewModel.actions()` is empty instead of asserting on the back-button label.

## Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.ui.ConversationHistoryScreenTest --tests com.eb.javafx.ui.ConversationHistoryViewModelTest --tests com.eb.javafx.ui.test.ComplexFooterBarTestScreenTest` — BUILD SUCCESSFUL.

---

# Change summary: mouse-driven navigation on the dialog window

## Motivation

Players expect the dialog window to advance on a click. Add primary (left) click → forward and secondary (right) click → back to `DialogEntriesView`, matching the existing footer button and Space/Backspace keyboard shortcuts.

## Changes

- `src/main/java/com/eb/javafx/ui/DialogEntriesView.java`
  - Constructor now sets `pickOnBounds(true)` so empty space inside the widget receives mouse events, and registers a `MOUSE_CLICKED` handler that calls `goForward()` on `MouseButton.PRIMARY` and `goBack()` on `MouseButton.SECONDARY`. Both branches consume the event so the right-click does not bubble to a parent context-menu request.
  - Added the `javafx.scene.input.MouseButton` import.
  - Updated the class Javadoc to describe the click behavior.
- `src/test/java/com/eb/javafx/ui/DialogEntriesViewTest.java`
  - Generalized `syntheticClick(Node)` to take an optional `MouseButton` (kept the single-arg overload for the existing footer test).
  - New tests: `leftClickOnViewAdvancesAndRightClickRewinds`, `leftClickAtNewestEntryDoesNotMoveCursor`, `rightClickAtOldestEntryDoesNotMoveCursor`.

## Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.ui.DialogEntriesViewTest` — BUILD SUCCESSFUL.
