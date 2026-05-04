package com.eb.javafx.input;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores reusable action definitions and trigger bindings in deterministic order. */
public final class InputMap {
    private final Map<String, InputAction> actions = new LinkedHashMap<>();
    private final List<InputBinding> bindings = new ArrayList<>();

    public void registerAction(InputAction action) {
        InputAction checkedAction = Validation.requireNonNull(action, "Input action is required.");
        if (actions.containsKey(checkedAction.id())) {
            throw new IllegalArgumentException("Input action already registered: " + checkedAction.id());
        }
        actions.put(checkedAction.id(), checkedAction);
    }

    public void bind(InputBinding binding) {
        InputBinding checkedBinding = Validation.requireNonNull(binding, "Input binding is required.");
        if (!actions.containsKey(checkedBinding.actionId())) {
            throw new IllegalArgumentException("Unknown input action: " + checkedBinding.actionId());
        }
        bindings.add(checkedBinding);
    }

    public Optional<InputAction> actionForTrigger(String contextId, InputTrigger trigger) {
        String checkedContextId = Validation.requireNonBlank(contextId, "Input context id is required.");
        InputTrigger checkedTrigger = Validation.requireNonNull(trigger, "Input trigger is required.");
        return bindings.stream()
                .filter(binding -> binding.trigger().equals(checkedTrigger))
                .map(binding -> actions.get(binding.actionId()))
                .filter(action -> action.contextId().equals(checkedContextId))
                .findFirst();
    }

    public List<InputAction> actions() {
        return Collections.unmodifiableList(new ArrayList<>(actions.values()));
    }

    public List<InputBinding> bindings() {
        return Collections.unmodifiableList(bindings);
    }

    public List<InputBinding> bindingsForAction(String actionId) {
        return bindings.stream().filter(binding -> binding.actionId().equals(actionId)).toList();
    }
}
