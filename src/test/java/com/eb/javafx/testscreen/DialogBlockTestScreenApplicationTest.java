package com.eb.javafx.testscreen;

import com.eb.javafx.text.DialogHistoryEntry;
import com.eb.javafx.ui.DialogEntriesView;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless smoke test for {@link DialogBlockTestScreenApplication}.
 *
 * <p>The actual {@code start(Stage)} method needs a real JavaFX stage, so this test reaches into
 * the demo's private {@code seedTenConversations} via reflection — that's the part we actually
 * care about, because it's what every demo run displays. The assertions lock in the contract for
 * the demo: ten distinct conversations are opened and closed, the history records all of them,
 * and entries are spread across the expected set of characters so the per-role colour rendering is
 * actually exercised.</p>
 */
final class DialogBlockTestScreenApplicationTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @BeforeAll
    static void initializeJavaFxToolkit() throws InterruptedException {
        // DialogEntriesView's constructor instantiates JavaFX nodes, so the toolkit must be up.
        CountDownLatch started = new CountDownLatch(1);
        if (JAVAFX_STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    started.countDown();
                });
            } catch (IllegalStateException exception) {
                Platform.setImplicitExit(false);
                started.countDown();
            }
        } else {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
    }

    @Test
    void demoSeedsExactlyTenConversationsAcrossMultipleCharacters() throws Exception {
        DialogEntriesView dialog = invokeSeed();

        List<DialogHistoryEntry> historyEntries = dialog.history().entries();
        assertEquals(10, historyEntries.size(),
                "Demo should populate exactly ten conversations into the dialog history.");

        // Every conversation should have been closed — the closing endedAt is what the assertion
        // proves, since DialogHistoryEntry.endedAt() is only set by endDialog().
        for (DialogHistoryEntry entry : historyEntries) {
            assertNotNull(entry.endedAt(),
                    "Conversation '" + entry.dialogId() + "' should be closed in the demo seed.");
            assertFalse(entry.messages().isEmpty(),
                    "Conversation '" + entry.dialogId() + "' should contain at least one message.");
        }

        // Speaker-coverage check: at least three distinct speaker ids should appear across the demo.
        // The five role colours in the application are NARRATOR / MC / BOOK_GIRL / RANDOM_GIRL /
        // MENTOR, and a healthy demo touches several of them so the colour palette is actually
        // exercised — three is the floor for "between different character" being meaningful.
        Set<String> speakerIds = historyEntries.stream()
                .flatMap(e -> e.messages().stream())
                .filter(m -> m.speaker() != null)
                .map(m -> m.speaker().id())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        assertTrue(speakerIds.size() >= 3,
                "Demo should exercise at least three distinct speakers, found: " + speakerIds);
    }

    @Test
    void testAppDiscoversDialogBlockDemoAsAStandaloneExample() {
        // The manual test screen (runTestScreen → TestScreenApplication) discovers standalone
        // examples by walking the user-manual examples root and filtering for files that contain a
        // `public static void main(String[] args)` declaration. This test locks in two things:
        //   1. The DialogBlockDemo.java shim file is present in the expected location.
        //   2. The standalone-example filter actually classifies it as a runnable example, so it
        //      will show up in the test app's tree and be runnable via the "Run" button.
        Path repoRoot = TestScreenApplication.repositoryRoot();
        Path demoPath = repoRoot.resolve(Path.of(
                "examples", "user-manual", "06-ui-screens-and-themes", "DialogBlockDemo.java"));
        assertTrue(java.nio.file.Files.isRegularFile(demoPath),
                "Standalone shim DialogBlockDemo.java should exist at " + demoPath);
        assertTrue(TestScreenApplication.isStandaloneExampleFile(demoPath),
                "Test app should classify DialogBlockDemo.java as a runnable standalone example.");

        Path examplesRoot = repoRoot.resolve(Path.of("examples", "user-manual"));
        List<Path> discovered = TestScreenApplication.standaloneExampleFiles(examplesRoot);
        assertTrue(discovered.contains(demoPath.toAbsolutePath().normalize()),
                "Standalone discovery should include DialogBlockDemo.java; found: " + discovered);
    }

    @Test
    void demoSeedsStartDividersAndSpokenEntriesButNoEndDividers() throws Exception {
        DialogEntriesView dialog = invokeSeed();

        // The widget no longer emits ConversationEnd dividers — only the start of each
        // conversation produces a visible divider line. The demo seed exercises this contract.
        long starts = dialog.dialogEntries().stream()
                .filter(e -> e instanceof DialogEntriesView.ConversationStart)
                .count();
        long ends = dialog.dialogEntries().stream()
                .filter(e -> e instanceof DialogEntriesView.ConversationEnd)
                .count();
        long spoken = dialog.dialogEntries().stream()
                .filter(e -> e instanceof DialogEntriesView.SpokenEntry)
                .count();

        assertEquals(10, starts, "Should append a ConversationStart divider for each of the 10 conversations.");
        assertEquals(0, ends, "endConversation should NOT append a ConversationEnd divider; only the start divider remains.");
        assertTrue(spoken >= 20, "Demo should produce at least ~2 spoken lines per conversation, found: " + spoken);
    }

    @Test
    void demoIncludesAtLeastOneExplicitMultilineMessage() throws Exception {
        DialogEntriesView dialog = invokeSeed();

        // At least one spoken line should contain an embedded newline so the demo visibly
        // exercises multi-paragraph rendering (Label wraps long lines naturally; explicit \n
        // lets the demo show a hard line break too).
        boolean hasExplicitMultiline = dialog.dialogEntries().stream()
                .filter(e -> e instanceof DialogEntriesView.SpokenEntry)
                .map(e -> ((DialogEntriesView.SpokenEntry) e).text())
                .anyMatch(text -> text.contains("\n"));
        assertTrue(hasExplicitMultiline,
                "Demo should include at least one multi-line message (containing \\n) to show off "
                        + "hard line breaks in the dialog block.");

        // At least one line should also be long enough that a normal dialog block width will wrap
        // it onto multiple lines naturally. We use 120 chars as a conservative threshold —
        // anything beyond that will wrap inside the 160px-speaker-column + body layout.
        boolean hasLongWrappingLine = dialog.dialogEntries().stream()
                .filter(e -> e instanceof DialogEntriesView.SpokenEntry)
                .map(e -> ((DialogEntriesView.SpokenEntry) e).text())
                .anyMatch(text -> text.length() >= 120);
        assertTrue(hasLongWrappingLine,
                "Demo should include at least one long line (>=120 chars) to exercise natural "
                        + "wrap rendering in the dialog block.");
    }

    /** Invokes {@code DialogBlockTestScreenApplication.seedTenConversations(...)} via reflection. */
    private static DialogEntriesView invokeSeed() throws Exception {
        // Build the speakers the same way the application does so the test stays a true
        // reflection of what the demo run displays. We don't need real colours here — the colour
        // rendering is covered by DialogEntriesViewTest — we only need *identifiable* speakers.
        var speakerClass = Class.forName("com.eb.javafx.text.DialogSpeaker");
        var ctor = speakerClass.getConstructor(String.class, String.class, String.class, String.class);

        Object narrator = ctor.newInstance("narrator", "Narrator", null, "#a0b0c0");
        Object mc = ctor.newInstance("mc", "Hero", null, "#88ddff");
        Object bookGirl = ctor.newInstance("book-girl", "Sarah", null, "#ffaaff");
        Object randomGirl = ctor.newInstance("random-girl", "Stranger", null, "#aaffaa");
        Object mentor = ctor.newInstance("mentor", "Old Mentor", null, "#ddcc88");

        DialogEntriesView dialog = new DialogEntriesView();

        Method method = DialogBlockTestScreenApplication.class.getDeclaredMethod(
                "seedTenConversations",
                DialogEntriesView.class,
                speakerClass, speakerClass, speakerClass, speakerClass, speakerClass);
        method.setAccessible(true);
        method.invoke(null, dialog, narrator, mc, bookGirl, randomGirl, mentor);

        // Sanity: at least one structured entry per conversation was actually added.
        List<DialogEntriesView.Entry> raw = new ArrayList<>(dialog.dialogEntries());
        assertFalse(raw.isEmpty(), "Seed method should append entries to the dialog view.");
        return dialog;
    }
}
