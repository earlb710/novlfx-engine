package com.eb.javafx.scene;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.util.Validation;

import java.nio.file.Path;

/** Registers a JSON-authored conversation bundle as content definitions and scene definitions. */
public final class JsonConversationContentModule implements StaticContentModule, SceneModule {
    private final ConversationDefinition conversation;

    public JsonConversationContentModule(Path jsonPath) {
        this(ConversationDefinitionJson.load(jsonPath));
    }

    public JsonConversationContentModule(ConversationDefinition conversation) {
        this.conversation = Validation.requireNonNull(conversation, "Conversation definition is required.");
    }

    @Override
    public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        contentRegistry.registerDefinitions(conversation.definitions());
    }

    @Override
    public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        contentRegistry.definition(conversation.titleDefinition());
        conversation.scenes().forEach(scene -> scene.steps().forEach(step -> {
            if (step.textDefinition() != null) {
                contentRegistry.definition(step.textDefinition());
            }
            step.choices().forEach(choice -> contentRegistry.definition(choice.textDefinition()));
        }));
    }

    @Override
    public void registerScenes(SceneRegistry sceneRegistry) {
        conversation.scenes().forEach(sceneRegistry::register);
    }

    @Override
    public void validateScenes(SceneRegistry sceneRegistry) {
        conversation.scenes().forEach(scene -> sceneRegistry.requireScene(scene.id()));
    }

    public ConversationDefinition conversation() {
        return conversation;
    }
}
