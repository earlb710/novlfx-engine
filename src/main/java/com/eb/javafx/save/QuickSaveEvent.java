package com.eb.javafx.save;

import com.eb.javafx.events.GameEvent;
import java.util.Map;

public final class QuickSaveEvent {
    public static final String EVENT_TYPE = "QUICK_SAVE_COMPLETED";

    public static GameEvent create(String slotId) {
        return GameEvent.now(EVENT_TYPE, slotId, Map.of("slotId", slotId));
    }

    private QuickSaveEvent() {}
}
