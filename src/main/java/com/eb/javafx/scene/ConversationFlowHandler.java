package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.util.Validation;

import java.util.Objects;
import java.util.function.Function;

/**
 * Adapts a looked-up conversation object into UI-neutral presentation data and returns optional user input.
 */
public final class ConversationFlowHandler {
    private final ActionContext context;
    private final Function<SceneViewModel, String> displayHandler;
    private final ScenePresenter presenter;

    public ConversationFlowHandler(ActionContext context, Function<SceneViewModel, String> displayHandler) {
        this.context = Objects.requireNonNull(context, "context");
        this.displayHandler = Objects.requireNonNull(displayHandler, "displayHandler");
        this.presenter = new ScenePresenter();
    }

    public String display(SceneDefinition conversation) {
        SceneDefinition checkedConversation = Validation.requireNonNull(conversation, "Conversation scene is required.");
        SceneRegistry sceneRegistry = new SceneRegistry();
        sceneRegistry.register(checkedConversation);
        SceneExecutor executor = new SceneExecutor(sceneRegistry);
        SceneExecutionResult result = executor.advanceUntilPause(context, executor.start(checkedConversation.id()));
        return displayHandler.apply(presenter.present(context, result));
    }
}
