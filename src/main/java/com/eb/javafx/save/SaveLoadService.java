package com.eb.javafx.save;

import com.eb.javafx.state.GameState;
import com.eb.javafx.util.InitializationGuard;
import com.eb.javafx.util.TimeFormatting;
import com.eb.javafx.util.Validation;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    private static final Pattern QUICK_FILE_PATTERN = Pattern.compile("quick-(\\d{3})\\.properties");
    private static final Pattern AUTO_FILE_PATTERN = Pattern.compile("auto-(\\d{3})\\.properties");

    /**
     * Save category — tags every slot with the source that wrote it so the Save screen can
     * present player-driven saves, F5 quicksaves, and scheduled autosaves on separate tabs
     * without their slot numbers colliding.
     *
     * <p>Each category gets its own filename prefix on disk
     * ({@code slot-NNN.properties} for NORMAL — legacy convention preserved for backward
     * compat — and {@code quick-NNN.properties} / {@code auto-NNN.properties} for the others),
     * so a slot 1 in the NORMAL tab and a slot 1 in the QUICK tab are independent files.</p>
     */
    public enum SaveSlotCategory {
        /** Player-initiated save (Save button, save screen click).  On-disk prefix: "slot-". */
        NORMAL("slot-"),
        /** F5-style quick save (rolling buffer the host fills automatically).  Prefix: "quick-". */
        QUICK("quick-"),
        /** Scheduled auto save (e.g. once per in-game day).  Prefix: "auto-". */
        AUTO("auto-");

        private final String filenamePrefix;

        SaveSlotCategory(String filenamePrefix) {
            this.filenamePrefix = filenamePrefix;
        }

        /** On-disk filename prefix used by {@link SaveLoadService#slotPath(SaveSlotCategory, int)}. */
        public String filenamePrefix() {
            return filenamePrefix;
        }
    }

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
        return writeSlotSummary(slot, gameState, description, savedAt, null);
    }

    /**
     * Variant of {@link #writeSlotSummary(int, GameState, String, Instant)} that also captures a
     * thumbnail PNG of the supplied {@link Scene} alongside the properties file.  The PNG is
     * written to {@link #slotThumbnailPath(int)} and recorded on the returned summary's
     * {@link SaveSlotSummary#thumbnail()} field so the Save / Load UI can render previews.
     *
     * <p>Pass {@code null} for {@code scene} to skip the screenshot — the summary will be
     * written without one and {@code SaveSlotSummary.thumbnail()} returns the on-disk path
     * regardless; callers should check {@link java.nio.file.Files#exists} before loading.</p>
     */
    public SaveSlotSummary writeSlotSummary(int slot, GameState gameState, String description,
                                              Instant savedAt, Scene scene) {
        return writeSlotSummary(SaveSlotCategory.NORMAL, slot, gameState, description, savedAt, scene);
    }

    /**
     * Category-aware variant of {@link #writeSlotSummary(int, GameState, String, Instant, Scene)}
     * that writes the slot's properties + thumbnail under the category's filename prefix.
     * Used by the Save screen's tabbed view so NORMAL, QUICK, and AUTO each have an
     * independent slot space.
     */
    public SaveSlotSummary writeSlotSummary(SaveSlotCategory category, int slot, GameState gameState,
                                              String description, Instant savedAt, Scene scene) {
        // Snapshot the Scene NOW so the thumbnail captured matches the moment of the save.
        // Snapshotting is JavaFX-thread-only and must run from the caller's context; if we
        // deferred to the WritableImage overload below without doing this, the JFX scene
        // graph could mutate before the rasterise call fires.
        WritableImage snapshot = scene == null ? null : scene.snapshot(null);
        return writeSlotSummary(category, slot, gameState, description, savedAt, snapshot);
    }

    /**
     * Variant that accepts a pre-captured {@link WritableImage} for the slot thumbnail —
     * useful when the calling screen captured its own snapshot BEFORE navigating to the
     * Save screen, so the saved thumbnail shows the gameplay scene the player came from
     * rather than the save screen itself.  Pass {@code null} to skip the thumbnail write.
     */
    public SaveSlotSummary writeSlotSummary(SaveSlotCategory category, int slot, GameState gameState,
                                              String description, Instant savedAt, WritableImage thumbnail) {
        return writeSlotSummary(category, slot, gameState, description, savedAt, thumbnail, null);
    }

    /**
     * Full-fat variant that lets the caller override the route the slot's
     * {@link SaveSlotSummary#startupRoute()} field records.  When {@code targetRoute} is
     * non-blank the summary persists that value (typical use case: the host captures
     * {@code routeContext.activeRouteId()} at save time so loading later restores the
     * player to the gameplay scene they came from, rather than the boot startup route).
     * When null/blank the summary falls back to {@code gameState.startupRoute()} — the
     * original behaviour.
     */
    public SaveSlotSummary writeSlotSummary(SaveSlotCategory category, int slot, GameState gameState,
                                              String description, Instant savedAt, WritableImage thumbnail,
                                              String targetRoute) {
        return writeSlotSummary(category, slot, gameState, description, savedAt, thumbnail,
                targetRoute, /*gameDate*/ "");
    }

    /**
     * Full-fat variant with a {@code gameDate} parameter — the in-game date / time label
     * (e.g. "Day 3 morning") the save UI shows in its Game-date column.  The engine treats
     * the value as an opaque host-supplied string; format is up to the calling game.  Pass
     * empty string when no game-date label is meaningful (e.g. for menu-only saves).
     */
    public SaveSlotSummary writeSlotSummary(SaveSlotCategory category, int slot, GameState gameState,
                                              String description, Instant savedAt, WritableImage thumbnail,
                                              String targetRoute, String gameDate) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(category, "Save category is required.");
        Validation.requireNonNull(gameState, "Game state is required for save summary writing.");
        Instant effectiveSavedAt = savedAt == null ? Instant.now() : savedAt;
        String effectiveRoute = (targetRoute == null || targetRoute.isBlank())
                ? gameState.startupRoute()
                : targetRoute;
        Path thumbnailPath = slotThumbnailPath(category, slot);
        if (thumbnail != null) {
            captureSlotThumbnail(thumbnail, category, slot);
        }
        SaveSlotSummary summary = new SaveSlotSummary(
                slot,
                schema.version(),
                effectiveRoute,
                description == null ? "" : description,
                effectiveSavedAt,
                Files.exists(thumbnailPath) ? thumbnailPath : null,
                category,
                gameDate == null ? "" : gameDate);
        Properties properties = new Properties();
        properties.setProperty("schemaVersion", Integer.toString(summary.schemaVersion()));
        properties.setProperty("startupRoute", summary.startupRoute());
        properties.setProperty("description", summary.description());
        properties.setProperty("savedAt", TimeFormatting.formatInstant(summary.savedAt()));
        properties.setProperty("gameDate", summary.gameDate());
        try (OutputStream outputStream = Files.newOutputStream(slotPath(category, slot))) {
            properties.store(outputStream, "eb JavaFX save summary");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write save slot: " + category + " " + slot, exception);
        }
        return summary;
    }

    /** Captures a PNG screenshot of {@code scene} and writes it to {@link #slotThumbnailPath}.
     *  Existing thumbnails for the slot are overwritten.  Must be called on the JavaFX
     *  application thread (Scene snapshots are JFX-thread-only). */
    public void captureSlotThumbnail(Scene scene, int slot) {
        captureSlotThumbnail(scene, SaveSlotCategory.NORMAL, slot);
    }

    /** Category-aware variant of {@link #captureSlotThumbnail(Scene, int)}. */
    public void captureSlotThumbnail(Scene scene, SaveSlotCategory category, int slot) {
        Validation.requireNonNull(scene, "Scene is required for thumbnail capture.");
        captureSlotThumbnail(scene.snapshot(null), category, slot);
    }

    /** Target width / height for the persisted thumbnail.  The save UI tiles render at
     *  288×162, so 480×270 gives ~1.7× the displayed size — enough quality buffer for
     *  Retina-style scaling while keeping file sizes around 30–50 KB rather than the
     *  3–5 MB a full-screen lossless PNG would consume.  Source aspect ratio is
     *  preserved (we scale to fit a 16:9 frame), so non-16:9 sources letterbox slightly
     *  rather than stretching. */
    private static final int THUMBNAIL_TARGET_WIDTH  = 480;
    private static final int THUMBNAIL_TARGET_HEIGHT = 270;
    /** JPEG quality — 0.85 is the standard "high quality without obvious artefacts"
     *  setting for photographic-style content (game screenshots, scene captures).  At
     *  this quality a 480×270 game screenshot lands at ~25–45 KB. */
    private static final float JPEG_QUALITY = 0.85f;

    /** Writes a pre-captured {@link WritableImage} as the slot's thumbnail JPG.  Used when
     *  the calling screen captured its own snapshot before navigating to the Save screen so
     *  the persisted preview reflects the gameplay scene, not the save UI.
     *
     *  <p>The snapshot is downscaled to a small thumbnail size and re-encoded as JPEG
     *  before writing — saving the full-resolution scene snapshot as PNG would produce
     *  multi-megabyte thumbnails for every slot.  The format change is observable on
     *  disk (the file is named {@code .jpg} not {@code .png}, per
     *  {@link #slotThumbnailPath(SaveSlotCategory, int)}); legacy {@code .png} thumbnails
     *  from older saves are still recognised by the read path's fallback lookup. */
    public void captureSlotThumbnail(WritableImage snapshot, SaveSlotCategory category, int slot) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(category, "Save category is required.");
        Validation.requireNonNull(snapshot, "Snapshot image is required for thumbnail capture.");
        BufferedImage fullImage = SwingFXUtils.fromFXImage(snapshot, null);
        if (fullImage == null) {
            throw new IllegalStateException("Unable to convert scene snapshot for slot: " + slot);
        }
        BufferedImage rgbThumbnail = downscaleToJpegThumbnail(fullImage);
        try {
            writeJpeg(rgbThumbnail, slotThumbnailPath(category, slot).toFile(), JPEG_QUALITY);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to write save-slot thumbnail: " + category + " " + slot, exception);
        }
    }

    /** Returns an RGB-backed BufferedImage scaled to fit inside the JPEG thumbnail target
     *  while preserving aspect ratio.  JPEG can't store an alpha channel, so the result
     *  is explicitly {@link BufferedImage#TYPE_INT_RGB} (any transparency in the source
     *  is composited against black, which is fine for opaque game screenshots).  The
     *  Graphics2D draw uses bilinear interpolation for a balance of speed and quality —
     *  thumbnails aren't on the hot path so quality matters more than throughput, but
     *  bicubic is overkill for this scale-down ratio. */
    private static BufferedImage downscaleToJpegThumbnail(BufferedImage source) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        double scale = Math.min(
                (double) THUMBNAIL_TARGET_WIDTH  / srcW,
                (double) THUMBNAIL_TARGET_HEIGHT / srcH);
        // Never upscale — if the source is already smaller than the target, write at its
        // native size to avoid blurring an already-tiny snapshot.
        if (scale > 1.0) {
            scale = 1.0;
        }
        int dstW = Math.max(1, (int) Math.round(srcW * scale));
        int dstH = Math.max(1, (int) Math.round(srcH * scale));
        BufferedImage rgb = new BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = rgb.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                    java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, dstW, dstH, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    /** Writes {@code image} as a JPEG to {@code target} at the supplied quality (0.0–1.0).
     *  Uses {@link javax.imageio.ImageWriter} directly so we can set the quality knob —
     *  {@code ImageIO.write(..., "jpg", ...)} hard-codes a default quality that produces
     *  larger files than necessary for save-screen thumbnails. */
    private static void writeJpeg(BufferedImage image, java.io.File target, float quality)
            throws IOException {
        java.util.Iterator<javax.imageio.ImageWriter> writers =
                ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available on this JVM.");
        }
        javax.imageio.ImageWriter writer = writers.next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (javax.imageio.stream.ImageOutputStream out =
                     ImageIO.createImageOutputStream(target)) {
            writer.setOutput(out);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    /** Deletes the slot's properties file and thumbnail PNG (if any).  No-op when the slot
     *  files don't exist.  Used by the Save screen's per-slot delete button — see the red
     *  minus on used grid tiles / list rows.
     *
     *  @return {@code true} when at least one file (properties or thumbnail) was removed;
     *          {@code false} when both were already absent.
     *  @throws IllegalStateException when the on-disk deletion failed (file locked, permissions). */
    public boolean deleteSlot(SaveSlotCategory category, int slot) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(category, "Save category is required.");
        boolean removed = false;
        try {
            removed |= Files.deleteIfExists(slotPath(category, slot));
            removed |= Files.deleteIfExists(slotThumbnailPath(category, slot));
            // Also clean up any legacy .png thumbnail from before the JPEG migration so
            // a slot deletion doesn't leave orphan files in the save directory.
            Path legacyPng = schema.saveDirectory().resolve(
                    String.format("%s%03d.png", category.filenamePrefix(), slot));
            removed |= Files.deleteIfExists(legacyPng);
            // Full gameplay snapshot file — written by captureSlotSnapshot, absent on
            // legacy / metadata-only saves.  deleteIfExists handles both cases.
            removed |= Files.deleteIfExists(slotSnapshotPath(category, slot));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to delete save slot: " + category + " " + slot, exception);
        }
        return removed;
    }

    /** Returns the filesystem path where the slot's thumbnail JPG is (or would be)
     *  stored.  Was {@code .png} historically — kept as {@code .jpg} now because the
     *  saved file is a downscaled JPEG (see {@link #captureSlotThumbnail}).  Legacy
     *  {@code .png} files from older saves still resolve via
     *  {@link #resolveExistingThumbnailPath}. */
    public Path slotThumbnailPath(int slot) {
        assertInitialized();
        validateSlot(slot);
        return schema.saveDirectory().resolve(String.format("slot-%03d.jpg", slot));
    }

    /**
     * Reads a versioned save-slot summary without hydrating full game state.
     *
     * @throws IllegalStateException when the slot is missing, unreadable, or schema-incompatible
     */
    public SaveSlotSummary readSlotSummary(int slot) {
        return readSlotSummary(SaveSlotCategory.NORMAL, slot);
    }

    /** Category-aware variant of {@link #readSlotSummary(int)}. */
    public SaveSlotSummary readSlotSummary(SaveSlotCategory category, int slot) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(category, "Save category is required.");
        Path path = slotPath(category, slot);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing save slot: " + category + " " + slot);
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read save slot: " + category + " " + slot, exception);
        }
        int schemaVersion = Integer.parseInt(properties.getProperty("schemaVersion"));
        if (!canLoadSchemaVersion(schemaVersion)) {
            throw new IllegalStateException("Unsupported save schema version: " + schemaVersion);
        }
        // resolveExistingThumbnailPath honours the new .jpg location AND falls back to
        // the legacy .png written by builds before the JPEG migration, so saves written
        // pre-migration still render their thumbnails after upgrade.
        Path thumbnailPath = resolveExistingThumbnailPath(category, slot);
        return new SaveSlotSummary(
                slot,
                schemaVersion,
                properties.getProperty("startupRoute", ""),
                properties.getProperty("description", ""),
                TimeFormatting.parseInstant(properties.getProperty("savedAt")),
                Files.exists(thumbnailPath) ? thumbnailPath : null,
                category,
                // gameDate defaults to empty string for legacy slot files written before
                // the field was introduced — the list view's Game-date column then just
                // shows blank for those rows, no schema version bump required.
                properties.getProperty("gameDate", ""));
    }

    /** Lists readable, schema-compatible save-slot summaries from the
     *  {@link SaveSlotCategory#NORMAL} category ordered by slot number. */
    public List<SaveSlotSummary> listSlotSummaries() {
        return listSlotSummaries(SaveSlotCategory.NORMAL);
    }

    /** Lists readable, schema-compatible save-slot summaries for the given category, ordered
     *  by slot number.  Filename patterns are picked from the category. */
    public List<SaveSlotSummary> listSlotSummaries(SaveSlotCategory category) {
        assertInitialized();
        Validation.requireNonNull(category, "Save category is required.");
        Pattern pattern = switch (category) {
            case NORMAL -> SLOT_FILE_PATTERN;
            case QUICK  -> QUICK_FILE_PATTERN;
            case AUTO   -> AUTO_FILE_PATTERN;
        };
        List<SaveSlotSummary> summaries = new ArrayList<>();
        try (var paths = Files.list(schema.saveDirectory())) {
            paths.sorted(Comparator.comparing(Path::getFileName))
                    .forEach(path -> {
                        Matcher matcher = pattern.matcher(path.getFileName().toString());
                        if (matcher.matches()) {
                            try {
                                summaries.add(readSlotSummary(category, Integer.parseInt(matcher.group(1))));
                            } catch (IllegalArgumentException | IllegalStateException exception) {
                                // Ignore unreadable or incompatible slot summaries so other saves remain listable.
                            }
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to list save slots for category: " + category, exception);
        }
        return List.copyOf(summaries);
    }

    /** Returns the validated properties-file path for a slot numbered 1 through 999.  Defaults
     *  to the {@link SaveSlotCategory#NORMAL} category — legacy callers keep writing to the
     *  {@code slot-NNN.properties} pattern they always did. */
    public Path slotPath(int slot) {
        return slotPath(SaveSlotCategory.NORMAL, slot);
    }

    /** Returns the validated properties-file path for a category + slot pair.  Files are
     *  named {@code <prefix>NNN.properties} where {@code prefix} is the category's
     *  {@link SaveSlotCategory#filenamePrefix}. */
    public Path slotPath(SaveSlotCategory category, int slot) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(category, "Save category is required.");
        return schema.saveDirectory().resolve(String.format("%s%03d.properties", category.filenamePrefix(), slot));
    }

    /** Same as {@link #slotThumbnailPath(int)} but category-scoped — returns
     *  {@code <prefix>NNN.png} alongside the properties file. */
    public Path slotThumbnailPath(SaveSlotCategory category, int slot) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(category, "Save category is required.");
        return schema.saveDirectory().resolve(String.format("%s%03d.jpg", category.filenamePrefix(), slot));
    }

    /** Returns the filesystem path where this slot's full-game-state snapshot JSON
     *  is (or would be) stored.  Companion to {@link #slotPath} (the metadata
     *  {@code .properties}) and {@link #slotThumbnailPath} (the JPEG thumbnail) —
     *  the three files together compose one save:
     *  <ul>
     *    <li>{@code <prefix>NNN.properties} — schema version, route, description, savedAt, gameDate.</li>
     *    <li>{@code <prefix>NNN.jpg} — downscaled thumbnail screenshot.</li>
     *    <li>{@code <prefix>NNN.snapshot.json} — full gameplay snapshot (this file).</li>
     *  </ul>
     *  Older saves predating the snapshot work don't have this file; the read path
     *  honours that via {@link #restoreSlotSnapshot} returning {@code false} in that case. */
    public Path slotSnapshotPath(SaveSlotCategory category, int slot) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(category, "Save category is required.");
        return schema.saveDirectory().resolve(
                String.format("%s%03d.snapshot.json", category.filenamePrefix(), slot));
    }

    /**
     * Captures {@code gameState}'s full snapshot at {@code gameDateTime} and writes it
     * as JSON to {@link #slotSnapshotPath}.  Pairs with {@link #writeSlotSummary} — the
     * SaveScreen calls writeSlotSummary first to persist the metadata + thumbnail, then
     * this method to persist the actual gameplay state.  Splitting them keeps the
     * metadata-only legacy callers (tests, ad-hoc summary writers) from being forced to
     * also capture a snapshot.
     *
     * <p>Encodes via {@link GameplayStateSnapshotJson}, which serialises every part of
     * {@link GameplayStateSnapshot} — progress, inventory, wardrobe, character states,
     * journal, location occupancy, AND every host-registered custom rollback section
     * (via {@code gameState.registerRollbackSnapshotSection}).  Host content modules
     * therefore round-trip automatically without further wiring at the SaveLoadService
     * level — register the section once at boot and it'll persist + restore here.</p>
     */
    public void captureSlotSnapshot(SaveSlotCategory category, int slot,
                                      GameState gameState,
                                      com.eb.javafx.gamesupport.GameDateTime gameDateTime) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(category, "Save category is required.");
        Validation.requireNonNull(gameState, "Game state is required for snapshot capture.");
        Validation.requireNonNull(gameDateTime, "Game date/time is required for snapshot capture.");
        GameplayStateSnapshot snapshot = gameState.snapshot(gameDateTime);
        String json = GameplayStateSnapshotJson.toJson(snapshot);
        try {
            java.nio.file.Files.writeString(slotSnapshotPath(category, slot), json);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "Unable to write save-slot snapshot: " + category + " " + slot, exception);
        }
    }

    /**
     * Restores the gameplay snapshot stored at {@link #slotSnapshotPath} into
     * {@code gameState}.  Returns {@code true} when a snapshot file was found and
     * applied; {@code false} when the file is absent (legacy save written before the
     * snapshot work, or admin / menu-only save where capture was skipped) — caller can
     * then decide whether to proceed without restoration or surface an error.
     *
     * <p>Decoding goes through {@link GameplayStateSnapshotJson#fromJson} which is the
     * inverse of the {@link #captureSlotSnapshot} encoder, then through
     * {@link GameState#restore(GameplayStateSnapshot)} which applies progress /
     * inventory / wardrobe / characters / journal / location occupancy AND walks the
     * custom-rollback-section list calling each host-registered consumer with its
     * payload.  Hosts therefore get their content modules (map layout, time of day,
     * moves, etc.) restored automatically.</p>
     */
    public boolean restoreSlotSnapshot(SaveSlotCategory category, int slot,
                                         GameState gameState) {
        assertInitialized();
        validateSlot(slot);
        Validation.requireNonNull(category, "Save category is required.");
        Validation.requireNonNull(gameState, "Game state is required for snapshot restore.");
        Path snapshotPath = slotSnapshotPath(category, slot);
        if (!java.nio.file.Files.exists(snapshotPath)) {
            return false;
        }
        String json;
        try {
            json = java.nio.file.Files.readString(snapshotPath);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "Unable to read save-slot snapshot: " + category + " " + slot, exception);
        }
        GameplayStateSnapshot snapshot = GameplayStateSnapshotJson.fromJson(
                json, "slot " + category + " " + slot);
        gameState.restore(snapshot);
        return true;
    }

    /** Returns the on-disk thumbnail path that ACTUALLY exists for this slot — checks the
     *  current {@code .jpg} layout first, then falls back to the legacy {@code .png}
     *  written by builds before the JPEG migration.  Used by the read path so existing
     *  saves keep showing their thumbnails after the format change.  Returns the
     *  {@code .jpg} path when neither file exists so callers see the canonical "where it
     *  would live" location for empty slots. */
    public Path resolveExistingThumbnailPath(SaveSlotCategory category, int slot) {
        Path jpg = slotThumbnailPath(category, slot);
        if (Files.exists(jpg)) {
            return jpg;
        }
        Path legacyPng = schema.saveDirectory().resolve(
                String.format("%s%03d.png", category.filenamePrefix(), slot));
        if (Files.exists(legacyPng)) {
            return legacyPng;
        }
        return jpg;
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
        private final Path thumbnail;
        private final SaveSlotCategory category;
        /** Host-supplied in-game date/time label captured at save time.  Format is opaque
         *  to the engine — typically something like "Day 3 morning" — and rendered as-is
         *  in the save list's Game-date column.  Empty string for legacy saves written
         *  before the field existed. */
        private final String gameDate;

        private SaveSlotSummary(int slot, int schemaVersion, String startupRoute, String description,
                                 Instant savedAt, Path thumbnail, SaveSlotCategory category,
                                 String gameDate) {
            this.slot = slot;
            this.schemaVersion = schemaVersion;
            this.startupRoute = startupRoute;
            this.description = description;
            this.savedAt = savedAt;
            this.thumbnail = thumbnail;
            this.category = category;
            this.gameDate = gameDate == null ? "" : gameDate;
        }

        /** Returns the category this summary was loaded under. */
        public SaveSlotCategory category() {
            return category;
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

        /** Returns the host-supplied in-game date / time label captured at save time —
         *  format is opaque to the engine (typically "Day 3 morning" or similar).
         *  Empty string for legacy saves written before the field existed. */
        public String gameDate() {
            return gameDate;
        }

        /** Returns the absolute path to this slot's screenshot PNG, or {@code null} when no
         *  screenshot was captured (older saves, or save callers that didn't supply a Scene). */
        public Path thumbnail() {
            return thumbnail;
        }
    }
}
