package com.eb.javafx.scene;

import com.eb.javafx.save.SaveSnapshotCodec;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.SimpleJson;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RollbackSnapshotCodec implements SaveSnapshotCodec<RollbackBuffer> {
    public static final String SECTION_ID = "rollback";
    public static final int SCHEMA_VERSION = 1;

    private final int capacity;

    public RollbackSnapshotCodec() {
        this(100);
    }

    public RollbackSnapshotCodec(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public String sectionId() {
        return SECTION_ID;
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public String toJson(RollbackBuffer buffer) {
        Validation.requireNonNull(buffer, "buffer");
        List<RollbackEntry> ordered = new ArrayList<>();
        while (buffer.size() > 0) {
            buffer.pop().ifPresent(e -> ordered.add(0, e));
        }
        // re-fill buffer so it is unchanged after toJson
        for (RollbackEntry entry : ordered) {
            buffer.snapshot(entry.flowState());
        }
        StringBuilder entries = new StringBuilder("[");
        boolean first = true;
        for (RollbackEntry entry : ordered) {
            if (!first) entries.append(",");
            entries.append(entryToJson(entry));
            first = false;
        }
        entries.append("]");
        return "{\n  \"entries\": " + entries + "\n}";
    }

    @Override
    public RollbackBuffer fromJson(String json, String sourceName) {
        Object parsed = SimpleJson.parse(json, sourceName);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("Rollback snapshot JSON root must be an object in: " + sourceName);
        }
        Object rawEntries = root.get("entries");
        RollbackBuffer buffer = new RollbackBuffer(capacity);
        if (rawEntries instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> entryMap)) {
                    throw new IllegalArgumentException("Rollback entry must be an object in: " + sourceName);
                }
                buffer.snapshot(flowStateFromMap(entryMap, sourceName));
            }
        }
        return buffer;
    }

    private String entryToJson(RollbackEntry entry) {
        SceneFlowState s = entry.flowState();
        return "{\"sceneId\": " + JsonStrings.quote(s.activeSceneId())
                + ", \"stepIndex\": " + s.stepIndex() + "}";
    }

    private SceneFlowState flowStateFromMap(Map<?, ?> map, String sourceName) {
        Object sceneId = map.get("sceneId");
        Object stepIndex = map.get("stepIndex");
        if (!(sceneId instanceof String sid)) {
            throw new IllegalArgumentException("Rollback entry sceneId must be a string in: " + sourceName);
        }
        if (!(stepIndex instanceof Number idx)) {
            throw new IllegalArgumentException("Rollback entry stepIndex must be a number in: " + sourceName);
        }
        return new SceneFlowState(sid, idx.intValue(), List.of(), List.of(), null);
    }
}
