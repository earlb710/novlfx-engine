package com.eb.javafx.scene;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Registry for named hotspot map definitions. */
public final class HotspotMapRegistry {
    private final Map<String, HotspotMapDefinition> maps = new LinkedHashMap<>();

    public void register(HotspotMapDefinition definition) {
        maps.put(definition.id(), definition);
    }

    public Optional<HotspotMapDefinition> find(String id) {
        return Optional.ofNullable(maps.get(id));
    }

    public HotspotMapDefinition require(String id) {
        return find(id).orElseThrow(() ->
            new IllegalArgumentException("No hotspot map registered with id: " + id));
    }
}
