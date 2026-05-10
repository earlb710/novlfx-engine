package com.eb.javafx.gamesupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MapAndLocationTextDefinitionTest {
    @TempDir
    Path tempDir;

    @Test
    void mapTextLoadsDescriptionsAndDefaultDescription() throws Exception {
        MapTextDefinition definition = MapTextDefinition.load(testResource("map-text.en.json"));

        assertEquals("en", definition.language());
        assertEquals(List.of("town", "main"), definition.maps().stream().map(MapTextEntry::mapId).toList());
        assertEquals("Town Map", definition.map("town").orElseThrow().description());
        assertEquals(MapTextEntry.DEFAULT_DESCRIPTION, definition.map("main").orElseThrow().description());
        assertTrue(definition.toJson().contains("\"description\": \"Main Map\""));
    }

    @Test
    void mapTextRoundTripsThroughJsonAndFileSave() throws Exception {
        MapTextDefinition original = MapTextDefinition.load(testResource("map-text.en.json"));
        Path output = tempDir.resolve("map-text.en.json");

        original.save(output);
        MapTextDefinition reloaded = MapTextDefinition.load(output);

        assertEquals(original.language(), reloaded.language());
        assertEquals(original.maps(), reloaded.maps());
        assertTrue(Files.readString(output).contains("\"mapId\": \"main\""));
        assertTrue(Files.readString(output).contains("\"description\": \"Main Map\""));
    }

    @Test
    void mapTextRejectsDuplicateMapIdsAndEmptyFiles() {
        assertThrows(IllegalArgumentException.class, () -> MapTextDefinition.of("en", List.of()));

        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class, () ->
                MapTextDefinition.of("en", List.of(
                        new MapTextEntry("town", "Town"),
                        new MapTextEntry("town", "Other"))));

        assertEquals("Map text already registered: town", duplicate.getMessage());
    }

    @Test
    void locationTextLoadsDescriptionsConditionsAndReferences() throws Exception {
        LocationTextDefinition definition = LocationTextDefinition.load(testResource("location-text-town.en.json"));

        assertEquals("en", definition.language());
        assertEquals("town", definition.mapId());
        assertEquals("town.square", definition.reference("square"));
        assertEquals(Optional.of(definition.location("square").orElseThrow()), definition.locationByReference("town.square"));
        assertFalse(definition.locationByReference("other.square").isPresent());

        LocationTextEntry square = definition.location("square").orElseThrow();
        assertEquals("town.square", square.reference(definition.mapId()));
        assertEquals(
                "The market square is quiet after dark.",
                square.descriptionForConditions(List.of("time of day=night")).text());
        assertEquals(
                "The market square is open.",
                square.descriptionForConditions(List.of("weather=rain")).text());
    }

    @Test
    void locationTextRoundTripsThroughJsonAndFileSave() throws Exception {
        LocationTextDefinition original = LocationTextDefinition.load(testResource("location-text-town.en.json"));
        Path output = tempDir.resolve("location-text-town.en.json");

        original.save(output);
        LocationTextDefinition reloaded = LocationTextDefinition.load(output);

        assertEquals(original.language(), reloaded.language());
        assertEquals(original.mapId(), reloaded.mapId());
        assertEquals(original.locations(), reloaded.locations());
        assertTrue(Files.readString(output).contains("\"conditions\": [\"time of day=day\"]"));
    }

    @Test
    void locationTextRejectsDuplicateLocationIdsAndEmptyDescriptions() {
        assertThrows(IllegalArgumentException.class, () -> LocationTextDefinition.of("en", "town", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new LocationTextEntry("square", List.of()));

        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class, () ->
                LocationTextDefinition.of("en", "town", List.of(
                        new LocationTextEntry("square", List.of(new LocationDescriptionVariant("A", List.of()))),
                        new LocationTextEntry("square", List.of(new LocationDescriptionVariant("B", List.of()))))));

        assertEquals("Location text already registered: square", duplicate.getMessage());
    }

    private static Path testResource(String name) throws URISyntaxException {
        URL resource = MapAndLocationTextDefinitionTest.class.getResource("/com/eb/javafx/gamesupport/" + name);
        if (resource == null) {
            throw new IllegalArgumentException("Missing test resource: " + name);
        }
        return Path.of(resource.toURI());
    }
}
