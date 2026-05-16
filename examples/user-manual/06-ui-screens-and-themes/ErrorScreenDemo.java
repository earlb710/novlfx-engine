import com.eb.javafx.testscreen.ErrorScreenTestApplication;

/**
 * Standalone launcher for the reusable {@code ErrorScreen} demo, surfaced through the manual test
 * screen ({@code runTestScreen}) as a discoverable example.
 *
 * <p>Opens a dark-red error window seeded from a synthetic {@code IllegalStateException}. The
 * heading shows the exception class name ({@code "IllegalStateException"}), the body shows the
 * exception's message, and a read-only monospace text area below shows the full stack trace —
 * selectable for {@code Ctrl+C} or one-click via the <b>Copy details</b> button.</p>
 *
 * <p>The button row exposes both <b>Continue</b> (which closes the demo window — illustrating the
 * recoverable-failure flow) and <b>Exit</b> (which quits the JavaFX runtime). Real applications
 * pass {@code null} as the {@code continueAction} for fatal errors and the Continue button
 * disappears entirely.</p>
 *
 * <p>The implementation lives in {@link ErrorScreenTestApplication} on the test classpath; this
 * file is the discoverable standalone shim so the demo appears in the manual test screen's
 * example tree.</p>
 */
public final class ErrorScreenDemo {
    private ErrorScreenDemo() {
    }

    public static void main(String[] args) {
        ErrorScreenTestApplication.main(args);
    }
}
