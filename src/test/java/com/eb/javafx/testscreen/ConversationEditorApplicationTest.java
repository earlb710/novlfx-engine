package com.eb.javafx.testscreen;

import com.eb.javafx.scene.ConversationDefinition;
import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinitionJson;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConversationEditorApplicationTest {
    @Test
    void resolvesConversationExamplesDirectoryFromRepository() {
        Path examplesDirectory = ConversationEditorApplication.conversationExamplesDirectory();

        assertTrue(Files.isDirectory(examplesDirectory));
        assertTrue(examplesDirectory.endsWith(Path.of("examples", "conversations")));
    }

    @Test
    void bundledConversationExamplesLoadAndValidate() throws IOException {
        try (var paths = Files.list(ConversationEditorApplication.conversationExamplesDirectory())) {
            List<Path> jsonFiles = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();

            assertEquals(List.of(ConversationEditorApplication.conversationExamplesDirectory().resolve("sample-conversation.json")), jsonFiles);
            for (Path jsonFile : jsonFiles) {
                ConversationDefinition conversation = ConversationDefinitionJson.load(jsonFile);
                assertTrue(ConversationEditorApplication.validationProblems(conversation).isEmpty(),
                        () -> "Invalid example: " + jsonFile + " -> "
                                + ConversationEditorApplication.validationProblems(conversation));
            }
        }
    }

    @Test
    void statusTextNamesSavedOrUnsavedConversationsAndValidationState() {
        assertEquals("Unsaved conversation | Conversation JSON is valid.",
                ConversationEditorApplication.statusText(null, List.of()));
        assertEquals("sample-conversation.json | Conversation JSON is valid.",
                ConversationEditorApplication.statusText(Path.of("sample-conversation.json"), List.of()));
        assertEquals("sample-conversation.json | 1 validation problem(s).",
                ConversationEditorApplication.statusText(Path.of("sample-conversation.json"), List.of("Missing title")));
    }

    @Test
    void navigationTreeShowsLr2AltConversationDocumentBlocksLinesAndVariants() {
        ConversationDefinition conversation = ConversationEditorApplication.sampleConversation();

        DefaultMutableTreeNode root = ConversationEditorApplication.buildNavigationTree(conversation);

        assertEquals("conversation document: schema 1 / en", root.getUserObject().toString());
        DefaultMutableTreeNode block = (DefaultMutableTreeNode) root.getChildAt(0);
        DefaultMutableTreeNode line = (DefaultMutableTreeNode) block.getChildAt(0);
        assertEquals("conversation: sample.conversation.opening.block_0001", block.getUserObject().toString());
        assertEquals("line: narrator", line.getUserObject().toString());
        assertEquals("variant: A reusable conversation document can ...",
                ((DefaultMutableTreeNode) line.getChildAt(0)).getUserObject().toString());
    }

    @Test
    void sampleConversationUsesLr2AltDocumentSchema() {
        ConversationDefinition conversation = ConversationEditorApplication.sampleConversation();
        ConversationBlock block = conversation.conversations().get(0);

        assertEquals(1, conversation.schemaVersion());
        assertEquals("en", conversation.language());
        assertEquals("sample.conversation.opening.block_0001", block.id());
        assertEquals("narrator", block.lines().get(0).speaker());
    }

    @Test
    void dirtyStateComparesSavedAndCurrentJsonSnapshots() {
        assertFalse(ConversationEditorApplication.hasUnsavedChanges("{}", "{}"));
        assertTrue(ConversationEditorApplication.hasUnsavedChanges("{}", "{\"id\":\"changed\"}"));
    }

    @Test
    void fileMenuLabelsContainFileActions() {
        assertEquals(List.of("New", "Load", "Save", "Save As"), ConversationEditorApplication.fileMenuActionLabels());
    }

    @Test
    void editorTabsExposeConversationDetailAndJsonAreas() {
        assertEquals(List.of("Conversations"), ConversationEditorApplication.conversationTabLabels());
        assertEquals(List.of("Detail", "JSON"), ConversationEditorApplication.detailTabLabels());
    }

    @Test
    void conversationFieldsCanUpdateDocumentAndSelectedConversation() {
        ConversationDefinition updated = ConversationEditorApplication.updateConversationBlock(
                ConversationEditorApplication.updateDocument(ConversationEditorApplication.sampleConversation(), 2, "fr"),
                0,
                "opening.changed",
                "Changed description");

        assertEquals(2, updated.schemaVersion());
        assertEquals("fr", updated.language());
        assertEquals("opening.changed", updated.conversations().get(0).id());
        assertEquals("Changed description", updated.conversations().get(0).description());
    }

    @Test
    void lineDetailFieldsCanUpdateSpeakerAndVariants() {
        ConversationDefinition updated = ConversationEditorApplication.updateLine(
                ConversationEditorApplication.sampleConversation(),
                0,
                0,
                "hero",
                List.of("First variant", "Second variant"));

        assertEquals("hero", updated.conversations().get(0).lines().get(0).speaker());
        assertEquals("First variant", updated.conversations().get(0).lines().get(0).variants().get(0).text());
        assertEquals("Second variant", updated.conversations().get(0).lines().get(0).variants().get(1).text());
    }

    @Test
    void addAndRemoveLineChangesSelectedConversationLines() {
        ConversationDefinition added = ConversationEditorApplication.addLine(ConversationEditorApplication.sampleConversation(), 0);

        assertEquals(3, added.conversations().get(0).lines().size());
        assertEquals("speaker", added.conversations().get(0).lines().get(2).speaker());

        ConversationDefinition removed = ConversationEditorApplication.removeLine(added, 0, 2);

        assertEquals(2, removed.conversations().get(0).lines().size());
        assertEquals("narrator", removed.conversations().get(0).lines().get(0).speaker());
    }
}
