package com.eb.javafx.gamesupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Registry for generic actions before authored eb action content is migrated. */
public final class ActionRegistry {
    private final Map<String, GameAction> actions = new LinkedHashMap<>();

    /** Registers a generic action definition. */
    public void register(GameAction action) {
        if (actions.containsKey(action.id())) {
            throw new IllegalArgumentException("Action already registered: " + action.id());
        }
        actions.put(action.id(), action);
    }

    /** Returns an action by ID, if one is registered. */
    public Optional<GameAction> action(String id) {
        return Optional.ofNullable(actions.get(id));
    }

    /** Returns registered actions in registration order. */
    public List<GameAction> actions() {
        return Collections.unmodifiableList(new ArrayList<>(actions.values()));
    }

    /** Returns registered actions grouped by category in registration order. */
    public Map<String, List<GameAction>> actionsByCategory() {
        Map<String, List<GameAction>> grouped = actions.values().stream()
                .collect(Collectors.groupingBy(GameAction::category, LinkedHashMap::new, Collectors.toList()));
        grouped.replaceAll((category, categoryActions) -> Collections.unmodifiableList(categoryActions));
        return Collections.unmodifiableMap(grouped);
    }

    /** Returns whether no authored or migrated actions have been registered yet. */
    public boolean isEmpty() {
        return actions.isEmpty();
    }
}
