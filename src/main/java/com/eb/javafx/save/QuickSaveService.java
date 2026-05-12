package com.eb.javafx.save;

import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.state.GameState;
import com.eb.javafx.util.SimpleJson;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public final class QuickSaveService {
    private static final String DEFAULT_SLOT_ID = "quicksave";

    private final SaveLoadService saveLoadService;
    private final GameEventBus eventBus;
    private final String slotId;

    public QuickSaveService(SaveLoadService saveLoadService, GameEventBus eventBus) {
        this(saveLoadService, eventBus, DEFAULT_SLOT_ID);
    }

    public QuickSaveService(SaveLoadService saveLoadService, GameEventBus eventBus, String slotId) {
        this.saveLoadService = Validation.requireNonNull(saveLoadService, "saveLoadService");
        this.eventBus = Validation.requireNonNull(eventBus, "eventBus");
        this.slotId = Validation.requireNonBlank(slotId, "slotId");
    }

    public void quickSave(GameState state) {
        Validation.requireNonNull(state, "state");
        String json = "{\"startupRoute\":\"" + escapeJson(state.startupRoute()) + "\"}";
        try {
            Files.writeString(slotPath(), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write quick save: " + slotPath(), e);
        }
        eventBus.publish(QuickSaveEvent.create(slotId));
    }

    public Optional<GameState> quickLoad() {
        Path path = slotPath();
        if (!Files.exists(path)) return Optional.empty();
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Object parsed = SimpleJson.parse(json, path.toString());
            if (parsed instanceof Map<?, ?> root && root.get("startupRoute") instanceof String route) {
                return Optional.of(new GameState(route));
            }
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }
        return sb.toString();
    }

    private Path slotPath() {
        return saveLoadService.schema().saveDirectory().resolve(slotId + ".json");
    }
}
