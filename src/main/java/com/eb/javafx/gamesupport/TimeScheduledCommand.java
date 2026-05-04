package com.eb.javafx.gamesupport;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Data-only command scheduled for a generic game date/time. */
public record TimeScheduledCommand(
        String id,
        GameDateTime runAt,
        String commandType,
        Map<String, String> payload) implements IdentifiedDefinition {
    public TimeScheduledCommand {
        id = Validation.requireNonBlank(id, "Scheduled command id is required.");
        runAt = Validation.requireNonNull(runAt, "Scheduled command run time is required.");
        commandType = Validation.requireNonBlank(commandType, "Scheduled command type is required.");
        payload = ImmutableCollections.copyMap(payload);
    }
}
