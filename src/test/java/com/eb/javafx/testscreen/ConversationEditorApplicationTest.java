package com.eb.javafx.testscreen;

import com.eb.javafx.scene.ConversationDefinition;
import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinition.LineType;
import com.eb.javafx.scene.ConversationDefinitionJson;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void conversationJsonFilesListsJsonFilesByName() {
        List<Path> jsonFiles = ConversationEditorApplication.conversationJsonFiles(
                ConversationEditorApplication.conversationExamplesDirectory());

        assertEquals(List.of(ConversationEditorApplication.conversationExamplesDirectory().resolve("sample-conversation.json")), jsonFiles);
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

        assertEquals("conversation document: Sample Conversation / en", root.getUserObject().toString());
        DefaultMutableTreeNode block = (DefaultMutableTreeNode) root.getChildAt(0);
        DefaultMutableTreeNode line = (DefaultMutableTreeNode) block.getChildAt(0);
        assertEquals("conversation: sample.conversation.opening.block_0001", block.getUserObject().toString());
        assertEquals("line: narrator (say)", line.getUserObject().toString());
        assertEquals("variant: A reusable conversation document can ...",
                ((DefaultMutableTreeNode) line.getChildAt(0)).getUserObject().toString());
    }

    @Test
    void sampleConversationUsesLr2AltDocumentSchema() {
        ConversationDefinition conversation = ConversationEditorApplication.sampleConversation();
        ConversationBlock block = conversation.conversations().get(0);

        assertEquals("Sample Conversation", conversation.name());
        assertEquals("en", conversation.language());
        assertEquals("sample.conversation.opening.block_0001", block.id());
        assertEquals("narrator", block.lines().get(0).speaker());
        assertEquals("", block.lines().get(0).listener());
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
        assertEquals(List.of("Files", "Conversations", "Detail"), ConversationEditorApplication.editorBlockLabels());
        assertEquals(List.of("Conversations"), ConversationEditorApplication.conversationTabLabels());
        assertEquals(List.of("Detail", "JSON"), ConversationEditorApplication.detailTabLabels());
    }

    @Test
    void conversationFieldsCanUpdateDocumentAndSelectedConversation() {
        ConversationDefinition updated = ConversationEditorApplication.updateConversationBlock(
                ConversationEditorApplication.updateDocument(ConversationEditorApplication.sampleConversation(), "French Sample", "fr"),
                0,
                "opening.changed",
                "Changed description");

        assertEquals("French Sample", updated.name());
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
                "guide",
                LineType.WHISPER);

        assertEquals("hero", updated.conversations().get(0).lines().get(0).speaker());
        assertEquals("guide", updated.conversations().get(0).lines().get(0).listener());
        assertEquals(LineType.WHISPER, updated.conversations().get(0).lines().get(0).type());
        assertEquals("A reusable conversation document can hold narration.",
                updated.conversations().get(0).lines().get(0).variants().get(0).text());
    }

    @Test
    void speakerAndListenerChoicesIncludeKnownParticipantsAndCurrentValue() {
        ConversationDefinition conversation = ConversationEditorApplication.sampleConversation();

        assertEquals(List.of("narrator", "guide", "hero"),
                ConversationEditorApplication.speakerChoices(conversation, "hero"));
        assertEquals(List.of("", "narrator", "guide", "hero"),
                ConversationEditorApplication.listenerChoices(conversation, "hero"));
    }

    @Test
    void variantDetailFieldsCanUpdateTextWeightAndConditions() {
        ConversationDefinition updated = ConversationEditorApplication.updateVariantKeepingTooltipAndValue(
                ConversationEditorApplication.sampleConversation(),
                0,
                0,
                0,
                "Updated variant",
                2.5,
                List.of("context=met_guide", "context=has_key"));

        assertEquals("Updated variant", updated.conversations().get(0).lines().get(0).variants().get(0).text());
        assertEquals(2.5, updated.conversations().get(0).lines().get(0).variants().get(0).weight());
        assertEquals(List.of("context=met_guide", "context=has_key"),
                updated.conversations().get(0).lines().get(0).variants().get(0).conditions());
    }

    @Test
    void variantDetailFieldsCanUpdateTooltipText() {
        ConversationDefinition updated = ConversationEditorApplication.updateVariant(
                ConversationEditorApplication.sampleConversation(),
                0,
                0,
                0,
                "Updated variant",
                "Tooltip details",
                "",
                1.0,
                List.of());

        assertEquals("Tooltip details", updated.conversations().get(0).lines().get(0).variants().get(0).tooltipText());
    }

    @Test
    void conditionTextCombinesTypeOperandAndValue() {
        assertEquals("context=met_guide",
                ConversationEditorApplication.conditionText("context", "=", " met_guide "));
        assertEquals("", ConversationEditorApplication.conditionText("context", "=", " "));
    }

    @Test
    void conditionTextsOmitBlankValuesAndKeepOnlyThreeRows() {
        assertEquals(List.of("context=first", "context=second", "context=third"),
                ConversationEditorApplication.conditionTexts(List.of(
                        ConversationEditorApplication.conditionText("context", "=", "first"),
                        "",
                        ConversationEditorApplication.conditionText("context", "=", "second"),
                        " ",
                        ConversationEditorApplication.conditionText("context", "=", "third"),
                        ConversationEditorApplication.conditionText("context", "=", "fourth"))));
    }

    @Test
    void conditionValueChoicesIncludeContextAndTimeOfDayValues() {
        ConversationDefinition conversation = ConversationEditorApplication.updateVariantKeepingTooltipAndValue(
                ConversationEditorApplication.sampleConversation(),
                0,
                0,
                0,
                "Updated variant",
                1.0,
                List.of("context=has_key", "time of day=evening"));

        assertEquals(List.of("has_key"),
                ConversationEditorApplication.conditionValueChoices(conversation, "context"));
        assertEquals(List.of("morning", "afternoon", "evening", "night"),
                ConversationEditorApplication.conditionValueChoices(conversation, "time of day"));
    }

    @Test
    void addLineChangesSelectedConversationLines() {
        ConversationDefinition added = ConversationEditorApplication.addLine(ConversationEditorApplication.sampleConversation(), 0);

        assertEquals(3, added.conversations().get(0).lines().size());
        assertEquals("new_speaker", added.conversations().get(0).lines().get(2).speaker());
        assertEquals("", added.conversations().get(0).lines().get(2).listener());
    }

    @Test
    void addVariantChangesSelectedLineVariants() {
        ConversationDefinition added = ConversationEditorApplication.addVariant(ConversationEditorApplication.sampleConversation(), 0, 0);

        assertEquals(2, added.conversations().get(0).lines().get(0).variants().size());
        assertEquals("", added.conversations().get(0).lines().get(0).variants().get(1).text());
        assertEquals(1.0, added.conversations().get(0).lines().get(0).variants().get(1).weight());
    }

    @Test
    void removeLineRemovesSelectedLineWhenConversationHasMultipleLines() {
        ConversationDefinition removed = ConversationEditorApplication.removeLine(ConversationEditorApplication.sampleConversation(), 0, 0);

        assertEquals(1, removed.conversations().get(0).lines().size());
        assertEquals("guide", removed.conversations().get(0).lines().get(0).speaker());
    }

    @Test
    void removeLineRejectsRemovingOnlyLineInConversation() {
        ConversationDefinition oneLine = ConversationEditorApplication.removeLine(ConversationEditorApplication.sampleConversation(), 0, 0);

        assertThrows(IllegalArgumentException.class, () -> ConversationEditorApplication.removeLine(oneLine, 0, 0));
    }
}
