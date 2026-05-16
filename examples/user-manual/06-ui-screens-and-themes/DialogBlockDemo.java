import com.eb.javafx.testscreen.DialogBlockTestScreenApplication;

/**
 * Standalone launcher for the {@code MAIN_APP_LAYOUT} dialog block demo, surfaced through the
 * manual test screen ({@code runTestScreen}) as a discoverable example.
 *
 * <p>Opens a JavaFX window where the dialog slot is a {@code DialogEntriesView} pre-seeded with
 * <strong>ten conversations</strong> between five characters (narrator, MC, book girl, random
 * girl, and the old mentor). Each character has its own role colour; the speaker column is
 * fixed-width so message bodies line up; the right-hand scrollbar reveals the full conversation
 * history. Left-click the dialog block to advance the cursor, right-click to rewind, or use
 * {@code Space} / {@code Backspace} for the same effect.</p>
 *
 * <p>Expected behaviour: a titled JavaFX window opens with the dialog block already populated.
 * Closing the window ends the demo. The implementation lives in
 * {@link DialogBlockTestScreenApplication} on the test classpath; this file is the discoverable
 * standalone shim so the launcher appears in the manual test screen's example list.</p>
 */
public final class DialogBlockDemo {
    private DialogBlockDemo() {
    }

    public static void main(String[] args) {
        DialogBlockTestScreenApplication.main(args);
    }
}
