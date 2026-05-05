package com.eb.javafx.testscreen;

import com.eb.javafx.scene.ConversationDefinition;
import com.eb.javafx.scene.ConversationDefinitionJson;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

            assertFalse(jsonFiles.isEmpty());
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
    void navigationTreeShowsDefinitionsScenesStepsAndChoices() {
        ConversationDefinition conversation = ConversationEditorApplication.sampleConversation();

        DefaultMutableTreeNode root = ConversationEditorApplication.buildNavigationTree(conversation);

        assertEquals("conversation: sample.conversation", root.getUserObject().toString());
        DefaultMutableTreeNode definitions = (DefaultMutableTreeNode) root.getChildAt(0);
        DefaultMutableTreeNode scenes = (DefaultMutableTreeNode) root.getChildAt(1);
        assertEquals("definitions: 4", definitions.getUserObject().toString());
        assertEquals("definition: sample.conversation.title", ((DefaultMutableTreeNode) definitions.getChildAt(0)).getUserObject().toString());
        DefaultMutableTreeNode startScene = (DefaultMutableTreeNode) scenes.getChildAt(0);
        assertEquals("scene: sample.conversation.start", startScene.getUserObject().toString());
        assertEquals("step: branch (CHOICE)", ((DefaultMutableTreeNode) startScene.getChildAt(1)).getUserObject().toString());
        assertEquals("choice: continue", ((DefaultMutableTreeNode) ((DefaultMutableTreeNode) startScene.getChildAt(1)).getChildAt(0)).getUserObject().toString());
    }

    @Test
    void validationReportsMissingContentDefinitions() {
        ConversationDefinition conversation = new ConversationDefinition(
                "missing.content",
                "missing.title",
                Map.of(),
                ConversationEditorApplication.sampleConversation().scenes(),
                Map.of());

        List<String> problems = ConversationEditorApplication.validationProblems(conversation);

        assertTrue(problems.stream().anyMatch(problem -> problem.contains("Missing title definition")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("Missing step text definition")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("Missing choice text definition")));
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
}
