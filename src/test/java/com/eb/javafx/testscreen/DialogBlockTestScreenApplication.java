package com.eb.javafx.testscreen;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.text.DialogSpeaker;
import com.eb.javafx.ui.DialogEntriesView;
import com.eb.javafx.ui.MainAppLayoutInsets;
import com.eb.javafx.ui.MainAppLayoutOrientation;
import com.eb.javafx.ui.MainAppLayoutPlan;
import com.eb.javafx.ui.MainAppLayoutRenderer;
import com.eb.javafx.ui.MainAppScreenResolver;
import com.eb.javafx.ui.ScreenBackgroundFit;
import com.eb.javafx.ui.UiTheme;
import com.eb.javafx.ui.test.TestUiScreenSize;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.List;

/**
 * Manual demo screen for the {@code MAIN_APP_LAYOUT} dialog block.
 *
 * <p>Boots a {@code MAIN_APP_LAYOUT} where the dialog slot is a {@link DialogEntriesView} pre-seeded
 * with <strong>ten conversations</strong> between five distinct characters (narrator, MC, book
 * girl, random girl, and an old mentor). Each character has its own role colour, so the demo
 * exercises both the column-aligned speaker layout and the per-role tinting added to the dialog
 * widget.</p>
 *
 * <p>Once the window opens:</p>
 * <ul>
 *   <li>The newest line sits at the bottom of the dialog block in bright white and a larger font.</li>
 *   <li>Earlier lines stack above it at 50% opacity and a smaller font.</li>
 *   <li>The right-hand vertical scrollbar lets you scroll back through every conversation; the
 *       mouse wheel scrolls too. The scrollbar always resets to the bottom on each cursor step
 *       so the active line stays in view.</li>
 *   <li><b>Left click</b> on the dialog block advances the cursor (greying out one more line);
 *       <b>right click</b> rewinds. <b>Space</b> / <b>Backspace</b> mirror the same actions.
 *       The <b>footer bar</b> at the bottom of the window provides the same back (‹) / forward (›)
 *       controls and greys them out automatically when the cursor reaches either end.
 *       Cursor navigation auto-skips the {@code ── Conversation: ... ──} start dividers so the
 *       cursor always lands on a spoken/plain line — the dividers act as visual separators, not
 *       reading positions.</li>
 *   <li>The <b>footer history (◷) button</b> toggles a full-height expanded view: the story slot
 *       collapses and the dialog block grows to fill the whole window, revealing the complete
 *       conversation record in one scrollable panel. Click ◷ again to restore the split view.</li>
 *   <li>Several conversations include <b>multi-line messages</b> — some written as
 *       multi-paragraph stanzas with explicit {@code \n} line breaks (e.g. the library-open
 *       narrator stanza and the day-1-evening internal monologue), others as long single-line
 *       sentences that wrap naturally inside the dialog block (e.g. the noon interlude).</li>
 * </ul>
 *
 * <p>Launch via:</p>
 * <pre>./gradlew --no-daemon runDialogBlockTestScreen</pre>
 */
public final class DialogBlockTestScreenApplication extends Application {

    /** Role colours used to tint each speaker's name and message body. */
    private static final String NARRATOR_COLOR = "#a0b0c0";
    private static final String MC_COLOR = "#88ddff";
    private static final String BOOK_GIRL_COLOR = "#ffaaff";
    private static final String RANDOM_GIRL_COLOR = "#aaffaa";
    private static final String MENTOR_COLOR = "#ddcc88";

    @Override
    public void start(Stage primaryStage) throws Exception {
        DialogSpeaker narrator = new DialogSpeaker("narrator", "Narrator", null, NARRATOR_COLOR);
        DialogSpeaker mc = new DialogSpeaker("mc", "Hero", null, MC_COLOR);
        DialogSpeaker bookGirl = new DialogSpeaker("book-girl", "Sarah", null, BOOK_GIRL_COLOR);
        DialogSpeaker randomGirl = new DialogSpeaker("random-girl", "Stranger", null, RANDOM_GIRL_COLOR);
        DialogSpeaker mentor = new DialogSpeaker("mentor", "Old Mentor", null, MENTOR_COLOR);

        DialogEntriesView dialog = new DialogEntriesView();
        seedTenConversations(dialog, narrator, mc, bookGirl, randomGirl, mentor);
        // Park the cursor on the newest line so the dialog block opens showing the latest exchange;
        // the player can left/right click or use Space/Backspace to walk the cursor through history.

        Node storyArea = buildStoryArea();

        MainAppLayoutPlan plan = new MainAppLayoutPlan(
                "Dialog Block Demo",
                /* backgroundImage */ null,
                ScreenBackgroundFit.STRETCH,
                /* backgroundOpacity */ 1.0,
                /* backgroundColor */ "#0d1b2d",
                /* storyScreenId   */ "story",
                /* dialogScreenId  */ "dialog",
                /* storyDialogRatio */ MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO,
                MainAppLayoutOrientation.VERTICAL,
                /* showFooter */ true,
                MainAppLayoutInsets.EMPTY,
                MainAppLayoutInsets.EMPTY,
                List.of());

        MainAppScreenResolver resolver = screenId -> switch (screenId) {
            case "story" -> storyArea;
            case "dialog" -> dialog;
            default -> null;
        };

        StackPane root = MainAppLayoutRenderer.render(plan, resolver, null);
        // Wire the footer back (‹) / forward (›) labels to drive dialog navigation.
        // bindToFooter also immediately applies the correct greyed-out state and keeps it in
        // sync as the cursor moves — forward starts greyed out because the cursor opens on the
        // last entry, back starts active because there is a full conversation history above it.
        dialog.bindToFooter(root);
        // Wire the footer history (◷) button to toggle the full-height expanded view.
        // dialogHeightShare = 1.0 - storyDialogRatio: the fraction of centre height the dialog
        // block occupies in normal split mode.
        double dialogHeightShare = 1.0 - MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO;
        dialog.bindHistoryToggle(root, storyArea, dialogHeightShare);

        PreferencesService preferences = new PreferencesService();
        preferences.load();
        UiTheme theme = new UiTheme();
        theme.initialize(preferences);

        int width = TestUiScreenSize.sceneWidth(preferences);
        int height = TestUiScreenSize.sceneHeight(preferences);
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(theme.stylesheet());

        // Backspace / Space mirror left/right click on the dialog block.
        dialog.installKeyboardShortcuts(scene);

        primaryStage.setTitle("Dialog Block Test — 10 conversations");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static Node buildStoryArea() {
        Label placeholder = new Label("[ Story area ]\n\nDialog block below shows 10 conversations between\n"
                + "Narrator, Hero, Sarah, Stranger, and the Old Mentor.\n\n"
                + "Scroll the dialog block — or left-click to advance, right-click to rewind.");
        placeholder.setStyle("-fx-text-fill: #dbeafe; -fx-font-size: 16px;");
        placeholder.setWrapText(true);
        placeholder.setMaxWidth(Double.MAX_VALUE);
        placeholder.setAlignment(Pos.CENTER);
        StackPane storyArea = new StackPane(placeholder);
        storyArea.setAlignment(Pos.CENTER);
        storyArea.setStyle("-fx-background-color: rgba(20, 56, 105, 0.35);");
        return storyArea;
    }

    /**
     * Populates the supplied dialog view with ten short conversations between five characters.
     * Each conversation is opened with {@link DialogEntriesView#startConversation} (which appends
     * the divider entry and opens the underlying {@code DialogHistory}) and closed with
     * {@link DialogEntriesView#endConversation}, so the resulting history mirrors what a real game
     * would record.
     */
    private static void seedTenConversations(
            DialogEntriesView d,
            DialogSpeaker narrator,
            DialogSpeaker mc,
            DialogSpeaker bookGirl,
            DialogSpeaker randomGirl,
            DialogSpeaker mentor) {

        // 1. Narrator opens the scene. Long descriptive line wraps naturally inside the dialog block.
        d.startConversation("library-open", new GameDateTime(1, "morning"), narrator);
        d.say(narrator, "It was a cold morning at the old library — the kind of morning where the radiators "
                + "clanked twice an hour and the dust in the slanted light moved like slow snow, undisturbed "
                + "by anyone who had not yet learned to be careful with quiet rooms.");
        // Explicit hard line breaks via \n — this exercises the Label's native multi-line rendering.
        d.say(narrator, "Sarah was already there.\n"
                + "Same chair, same window.\n"
                + "Half-hidden behind a tower of books.");
        d.endConversation(new GameDateTime(1, "morning"));

        // 2. MC meets the book girl.
        d.startConversation("first-meet", new GameDateTime(1, "morning"), mc, bookGirl);
        d.say(mc, "Mind if I sit here?");
        d.say(bookGirl, "Only if you promise not to talk for the next four hours.");
        d.say(mc, "I'll do my best. No promises about hour five.");
        d.whisper(bookGirl, "Idiot.");
        d.endConversation(new GameDateTime(1, "morning"));

        // 3. Narrator interlude — a long natural-wrap descriptive line.
        d.startConversation("interlude-1", new GameDateTime(1, "noon"), narrator);
        d.say(narrator, "Outside, the city moved through its noon routine — buses pulling away with that hydraulic "
                + "sigh they only make at midday, footsteps falling into the rhythm of people who already know "
                + "where they are going, and the smell of bread that turns one block of a city into a memory of "
                + "another city entirely.");
        d.endConversation(new GameDateTime(1, "noon"));

        // 4. MC bumps into a stranger.
        d.startConversation("first-stranger", new GameDateTime(1, "noon"), mc, randomGirl);
        d.shout(randomGirl, "Watch where you're going!");
        d.say(mc, "Sorry — I didn't see you.");
        d.say(randomGirl, "Obviously. You owe me a coffee.");
        d.endConversation(new GameDateTime(1, "noon"));

        // 5. The two girls cross paths.
        d.startConversation("girls-cross", new GameDateTime(1, "afternoon"), bookGirl, randomGirl);
        d.say(randomGirl, "You're that quiet one from the library, right?");
        d.say(bookGirl, "And you're the one who shouts at strangers in the street.");
        d.say(randomGirl, "Only the ones who deserve it.");
        d.whisper(bookGirl, "Charming.");
        d.endConversation(new GameDateTime(1, "afternoon"));

        // 6. MC's internal monologue voiced by the narrator. Multi-paragraph entry built with
        //    explicit hard line breaks plus a long natural-wrap follow-up so the dialog block
        //    shows both styles of multi-line text.
        d.startConversation("monologue", new GameDateTime(1, "evening"), narrator);
        d.say(narrator, "Two girls in one day.\n"
                + "Both completely different.\n"
                + "Both impossible to ignore.");
        d.say(narrator, "Sarah felt safe — a book you keep re-reading because the ending is exactly where you left "
                + "it last time. The stranger felt like a window someone left open in winter; you noticed it across "
                + "the room and you had to decide whether to close it or just stand near it and pretend you weren't "
                + "cold.");
        d.endConversation(new GameDateTime(1, "evening"));

        // 7. The three meet by accident.
        d.startConversation("three-way", new GameDateTime(1, "evening"), mc, bookGirl, randomGirl);
        d.say(mc, "Oh. Hi. Uh — you two have met?");
        d.say(bookGirl, "Briefly.");
        d.say(randomGirl, "I shouted at him this morning.");
        d.say(bookGirl, "Naturally.");
        d.endConversation(new GameDateTime(1, "evening"));

        // 8. Quiet moment with the book girl.
        d.startConversation("library-quiet", new GameDateTime(1, "night"), mc, bookGirl);
        d.whisper(bookGirl, "Don't tell anyone, but I like that you keep coming back.");
        d.say(mc, "I like that you're always here.");
        d.whisper(bookGirl, "Then don't stop coming.");
        d.endConversation(new GameDateTime(1, "night"));

        // 9. Confrontation with the stranger.
        d.startConversation("stranger-clash", new GameDateTime(2, "morning"), mc, randomGirl);
        d.shout(randomGirl, "You ghosted me!");
        d.say(mc, "I — that's not what happened.");
        d.shout(randomGirl, "Then what did happen?");
        d.say(mc, "I got scared. That's what happened.");
        d.endConversation(new GameDateTime(2, "morning"));

        // 10. The mentor steps in, narrator closes.
        d.startConversation("mentor-close", new GameDateTime(2, "noon"), mc, mentor, narrator);
        d.say(mentor, "Boy. You can't outrun a complicated heart with a simpler one.");
        d.say(mc, "I know. I'm learning.");
        d.say(mentor, "Good. Learn faster.");
        d.say(narrator, "And just like that, the second day was already older than the first.");
        d.endConversation(new GameDateTime(2, "noon"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
