import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.ui.FooterController;
import com.eb.javafx.ui.ScreenShell;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;

/**
 * Reference snippet for {@link FooterController}, the generic footer router that turns the static
 * bar produced by {@link ScreenShell#footerBar()} into a live control (hover highlight, id-based
 * click router, and scene-level keyboard shortcuts).
 *
 * <p>Use this when you build a custom screen shell or any non-{@code MAIN_APP_LAYOUT} container and
 * want the standard footer behaviour. In a {@code MAIN_APP_LAYOUT} screen whose dialog slot is a
 * {@code DialogEntriesView}, {@code MainAppLayoutRenderer} already performs this wiring for you.</p>
 *
 * <p>This file is a reference snippet (it lives outside {@code src/main}/{@code src/test} and is not
 * compiled by Gradle). The {@code routeContext}, {@code storyNode}, and navigation callbacks shown
 * below are placeholders supplied by the host application.</p>
 */
public final class FooterControlDemo {
    private FooterControlDemo() {
    }

    /**
     * Wires the standard footer onto a caller-built {@link BorderPane} shell and installs the
     * footer keyboard shortcuts (Backspace / Space mirror the footer back / forward options, while
     * F5 / F9 trigger quick-save / quick-load — neither has a footer button).
     */
    public static Scene buildShell(RouteContext routeContext, Node storyNode) {
        BorderPane root = new BorderPane(storyNode);
        // The standard footer: back / history (◷) / forward / save / load / preferences.
        root.setBottom(ScreenShell.footerBar());

        FooterController.wireFooter(
                root,
                routeContext,
                ScreenShell.defaultFooterOptions(),  // or your own List<ScreenShell.FooterOption>
                () -> { /* back    */ },       // FooterController.BACK_ID
                () -> { /* forward */ },       // FooterController.FORWARD_ID
                () -> { /* skip    */ },       // FooterController.SKIP_MODE_ID
                null);                         // history -> SceneRouter.CONVERSATION_HISTORY_ROUTE

        // Save / load / preferences / quick-save options are handled internally by the router, so
        // they need no callback here.

        Scene scene = new Scene(root, 1280, 720);
        FooterController.installKeyboardShortcuts(scene);
        return scene;
    }
}
