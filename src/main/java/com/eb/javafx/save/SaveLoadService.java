package com.eb.javafx.save;

import com.eb.javafx.state.GameState;
import com.eb.javafx.util.InitializationGuard;
import com.eb.javafx.util.TimeFormatting;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Section 1.8 boundary for the explicit JavaFX save/load lifecycle.
 *
 * <p>Some source engines serialize live runtime objects. The JavaFX port should instead load
 * registries and services first, then hydrate mutable game state from a versioned
 * schema. This service now writes versioned slot summaries as properties files so
 * save/load screens can validate schema metadata before full game-state
 * serialization is migrated.</p>
 */
public final class SaveLoadService {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Pattern SLOT_FILE_PATTERN = Pattern.compile("slot-(\\d{3})\\.properties");

    private final Path configuredSaveDirectory;
    private final InitializationGuard initializationGuard = new InitializationGuard("Save/load service used before initialization.");
    private SaveSchema schema;

    public SaveLoadService() {
        this(Path.of(System.getProperty("user.home"), ".eb", "saves"));
    }

    public SaveLoadService(Path saveDirectory) {
        this.configuredSaveDirectory = saveDirectory;
    }

    /**
     * Initializes schema metadata and creates the configured save directory.
     *
     * @throws IllegalStateException when the save directory cannot be created
     */
    public void initialize() {
        schema = new SaveSchema(
                CURRENT_SCHEMA_VERSION,
                configuredSaveDirectory);
        try {
            Files.createDirectories(schema.saveDirectory());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create save directory: " + schema.saveDirectory(), exception);
        }
        initializationGuard.markInitialized();
    }

    /** Returns whether startup has prepared the save/load boundary. */
    public boolean isInitialized() {
        return initializationGuard.isInitialized();
    }

    /** Returns explicit save schema metadata created during startup. */
    public SaveSchema schema() {
        assertInitialized();
        return schema;
    }

    /** Returns whether a saved version can be loaded without migration. */
    public boolean canLoadSchemaVersion(int version) {
        assertInitialized();
        return version == schema.version();
    }

    /**
     * Writes a lightweight versioned save-slot summary for migrated save/load UI.
     *
     * <p>Slots are numbered 1 through 999 and stored as {@code slot-NNN.properties}
     * files. This summary records metadata only; full game-state serialization is
     * intentionally deferred.</p>
     */
    public SaveSlotSummary writeSlotSummary(int slot, GameState gameState, String description, Instant savedAt) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(gameState, "Game state is required for save summary writing.");
        Instant effectiveSavedAt = savedAt == null ? Instant.now() : savedAt;
        SaveSlotSummary summary = new SaveSlotSummary(
                slot,
                schema.version(),
                gameState.startupRoute(),
                description == null ? "" : description,
                effectiveSavedAt);
        Properties properties = new Properties();
        properties.setProperty("schemaVersion", Integer.toString(summary.schemaVersion()));
        properties.setProperty("startupRoute", summary.startupRoute());
        properties.setProperty("description", summary.description());
        properties.setProperty("savedAt", TimeFormatting.formatInstant(summary.savedAt()));
        try (OutputStream outputStream = Files.newOutputStream(slotPath(slot))) {
            properties.store(outputStream, "eb JavaFX save summary");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write save slot: " + slot, exception);
        }
        return summary;
    }

    /**
     * Reads a versioned save-slot summary without hydrating full game state.
     *
     * @throws IllegalStateException when the slot is missing, unreadable, or schema-incompatible
     */
    public SaveSlotSummary readSlotSummary(int slot) {
        assertInitialized();
        validateSlot(slot);
        Path path = slotPath(slot);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing save slot: " + slot);
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read save slot: " + slot, exception);
        }
        int schemaVersion = Integer.parseInt(properties.getProperty("schemaVersion"));
        if (!canLoadSchemaVersion(schemaVersion)) {
            throw new IllegalStateException("Unsupported save schema version: " + schemaVersion);
        }
        return new SaveSlotSummary(
                slot,
                schemaVersion,
                properties.getProperty("startupRoute", ""),
                properties.getProperty("description", ""),
                TimeFormatting.parseInstant(properties.getProperty("savedAt")));
    }

    /** Lists readable, schema-compatible save-slot summaries ordered by slot number. */
    public List<SaveSlotSummary> listSlotSummaries() {
        assertInitialized();
        List<SaveSlotSummary> summaries = new ArrayList<>();
        try (var paths = Files.list(schema.saveDirectory())) {
            paths.sorted(Comparator.comparing(Path::getFileName))
                    .forEach(path -> {
                        Matcher matcher = SLOT_FILE_PATTERN.matcher(path.getFileName().toString());
                        if (matcher.matches()) {
                            summaries.add(readSlotSummary(Integer.parseInt(matcher.group(1))));
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to list save slots.", exception);
        }
        return List.copyOf(summaries);
    }

    /** Returns the validated properties-file path for a slot numbered 1 through 999. */
    public Path slotPath(int slot) {
        assertInitialized();
        validateSlot(slot);
        return schema.saveDirectory().resolve(String.format("slot-%03d.properties", slot));
    }

    private void assertInitialized() {
        initializationGuard.requireInitialized();
    }

    private void validateSlot(int slot) {
        Validation.requireSlot(slot);
    }

    /**
     * Minimal versioned save schema descriptor for migrated save/load screens.
     *
     * <p>The version controls compatibility checks and the save directory is owned
     * by {@link SaveLoadService}, which creates it during initialization.</p>
     */
    public static final class SaveSchema {
        private final int version;
        private final Path saveDirectory;

        private SaveSchema(int version, Path saveDirectory) {
            this.version = version;
            this.saveDirectory = saveDirectory;
        }

        /** Returns the schema version that summaries must match to load without migration. */
        public int version() {
            return version;
        }

        /** Returns the directory containing versioned save-slot property files. */
        public Path saveDirectory() {
            return saveDirectory;
        }
    }

    /**
     * Lightweight save-slot metadata used before full state hydration exists.
     *
     * <p>The summary captures slot number, schema version, startup route, optional
     * description, and save timestamp so UI can list saves without deserializing
     * future mutable gameplay state.</p>
     */
    public static final class SaveSlotSummary {
        private final int slot;
        private final int schemaVersion;
        private final String startupRoute;
        private final String description;
        private final Instant savedAt;

        private SaveSlotSummary(int slot, int schemaVersion, String startupRoute, String description, Instant savedAt) {
            this.slot = slot;
            this.schemaVersion = schemaVersion;
            this.startupRoute = startupRoute;
            this.description = description;
            this.savedAt = savedAt;
        }

        /** Returns the one-based save slot number. */
        public int slot() {
            return slot;
        }

        /** Returns the schema version stored with this summary. */
        public int schemaVersion() {
            return schemaVersion;
        }

        /** Returns the route that should be entered after loading this save. */
        public String startupRoute() {
            return startupRoute;
        }

        /** Returns the optional player/debug-facing save description. */
        public String description() {
            return description;
        }

        /** Returns the timestamp recorded when the summary was written. */
        public Instant savedAt() {
            return savedAt;
        }
    }
}
