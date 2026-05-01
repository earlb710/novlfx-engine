package com.eb.javafx.gamesupport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocationSupportTest {

    @Test
    void locationRegistryStoresGenericDescriptorsInRegistrationOrder() {
        LocationRegistry registry = new LocationRegistry();
        LocationDescriptor hub = new LocationDescriptor(
                "hub",
                "Hub",
                "location",
                null,
                List.of("root"),
                List.of());
        LocationDescriptor room = new LocationDescriptor(
                "room",
                "Room",
                "location",
                "hub",
                List.of("interior"),
                List.of());

        registry.register(hub);
        registry.register(room);

        assertEquals(List.of(hub, room), registry.locations());
        assertTrue(registry.location("room").isPresent());
        assertEquals("hub", registry.location("room").orElseThrow().parentLocationId());
        assertThrows(UnsupportedOperationException.class, () -> registry.locations().add(hub));
    }

    @Test
    void locationRegistryRejectsDuplicateIdsAndMissingReferences() {
        LocationRegistry registry = new LocationRegistry();
        LocationDescriptor location = new LocationDescriptor(
                "room",
                "Room",
                "location",
                "missing-hub",
                List.of(),
                List.of("missing-action"));

        registry.register(location);

        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class, () ->
                registry.register(new LocationDescriptor("room", "Other", "location", null, List.of(), List.of())));
        assertEquals("Location already registered: room", duplicate.getMessage());

        IllegalStateException missingParent = assertThrows(IllegalStateException.class, () ->
                registry.validateReferences(new ActionRegistry()));
        assertEquals("Location room references missing parent location: missing-hub", missingParent.getMessage());
    }

    @Test
    void locationRegistryValidatesActionReferencesAfterParentReferences() {
        LocationRegistry registry = new LocationRegistry();
        registry.register(new LocationDescriptor("hub", "Hub", "location", null, List.of(), List.of()));
        registry.register(new LocationDescriptor("room", "Room", "location", "hub", List.of(), List.of("rest")));

        IllegalStateException missingAction = assertThrows(IllegalStateException.class, () ->
                registry.validateReferences(new ActionRegistry()));
        assertEquals("Location room references missing action: rest", missingAction.getMessage());

        ActionRegistry actions = new ActionRegistry();
        actions.register(new GameAction("rest", "Rest", "location", List.of(), List.of()));
        registry.validateReferences(actions);
    }

    @Test
    void occupancyMovesCharactersBetweenKnownLocations() {
        LocationRegistry registry = new LocationRegistry();
        registry.register(new LocationDescriptor("hub", "Hub", "location", null, List.of(), List.of()));
        registry.register(new LocationDescriptor("room", "Room", "location", "hub", List.of(), List.of()));
        LocationOccupancy occupancy = new LocationOccupancy();

        occupancy.place("character", "hub", registry);
        occupancy.place("character", "room", registry);

        assertEquals("room", occupancy.locationOf("character").orElseThrow());
        assertFalse(occupancy.charactersAt("hub").contains("character"));
        assertTrue(occupancy.charactersAt("room").contains("character"));
        assertThrows(UnsupportedOperationException.class, () -> occupancy.charactersAt("room").add("other"));
    }

    @Test
    void occupancyRejectsUnknownLocations() {
        LocationOccupancy occupancy = new LocationOccupancy();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                occupancy.place("character", "missing", new LocationRegistry()));

        assertEquals("Unknown location: missing", exception.getMessage());
    }
}
