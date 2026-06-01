package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.save.SaveLoadService.SaveSlotCategory;
import com.eb.javafx.save.SaveLoadService.SaveSlotSummary;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Generic, host-agnostic quick-save / quick-load / daily auto-save actions.
 *
 * <p>All three reuse the engine's full save pipeline — {@link SaveLoadService#writeSlotSummary}
 * for metadata + thumbnail and {@link SaveLoadService#captureSlotSnapshot} for the full
 * gameplay snapshot — so a quick/auto save restores exactly like a normal numbered save.  The
 * in-game date label + snapshot timestamp come from the same suppliers a host registers with
 * {@link SaveScreen#setGameDateSupplier} / {@link SaveScreen#setGameDateTimeSupplier}, so no
 * extra host wiring is needed beyond what normal saves already require.</p>
 *
 * <p>Quick and auto saves each live in their own rotating slot buffer
 * ({@link SaveSlotCategory#QUICK} / {@link SaveSlotCategory#AUTO}): each new save lands on the
 * slot one past the most-recently-written one, wrapping {@code N → 1} so the buffer keeps the
 * most recent {@code slotCount} entries and overwrites its oldest position once full.</p>
 *
 * <ul>
 *   <li>{@link #quickSave} — writes silently then reports the slot via an info dialog
 *       ("Quick save successful to slot N").</li>
 *   <li>{@link #quickLoad} — loads the most-recent quick save, behind an "are you sure"
 *       confirmation so a stray key press can't wipe live progress.</li>
 *   <li>{@link #autoSave} — writes silently (no dialog); intended to be triggered by the host
 *       at a game-defined moment (e.g. end of day).  {@link #autoSaveIfEnabled} additionally
 *       gates on the {@code save.autoSaveDaily} preference.</li>
 * </ul>
 *
 * <p>The host decides <em>when</em> to call these (footer button, F5/F9, day rollover, …); this
 * class owns the <em>how</em>.</p>
 */
public final class QuickSaveActions {

    /** Default number of rotating slots for quick / auto saves before the buffer wraps. */
    public static final int DEFAULT_SLOT_COUNT = 15;

    private QuickSaveActions() {
    }

    // ----- Quick save ---------------------------------------------------------------------

    /** Quick save into the rotating {@link SaveSlotCategory#QUICK} buffer (default slot count). */
    public static void quickSave(RouteContext context) {
        quickSave(context, DEFAULT_SLOT_COUNT);
    }

    /** Quick save into the rotating {@link SaveSlotCategory#QUICK} buffer, reporting the written
     *  slot via an info dialog. */
    public static void quickSave(RouteContext context, int slotCount) {
        if (!canSave(context)) {
            System.err.println("[QuickSaveActions] quickSave skipped — no save service / game state.");
            return;
        }
        int slots = normaliseSlotCount(slotCount);
        Scene scene = activeScene(context);
        int slot = writeRotating(context, SaveSlotCategory.QUICK, slots, "Quick save ", scene);
        if (slot <= 0) {
            DialogMessages.error(scene, context.uiTheme(), "Quick Save",
                    "Quick save failed.", "", null);
            return;
        }
        DialogMessages.info(scene, context.uiTheme(), "Quick Save",
                "Quick save successful to slot " + slot, "", null);
    }

    // ----- Quick load ---------------------------------------------------------------------

    /** Quick load the most-recent quick save, behind a confirmation (default slot count). */
    public static void quickLoad(RouteContext context) {
        quickLoad(context, DEFAULT_SLOT_COUNT);
    }

    /** Quick load the most-recent quick save (by timestamp), behind an "are you sure" confirm. */
    public static void quickLoad(RouteContext context, int slotCount) {
        if (!canSave(context)) {
            System.err.println("[QuickSaveActions] quickLoad skipped — no save service / game state.");
            return;
        }
        int slots = normaliseSlotCount(slotCount);
        Scene scene = activeScene(context);
        SaveSlotSummary latest = latest(context.saveLoadService(), SaveSlotCategory.QUICK, slots);
        if (latest == null) {
            DialogMessages.info(scene, context.uiTheme(), "Quick Load",
                    "No quick saves found.", "", null);
            return;
        }
        DialogMessages.confirm(scene, context.uiTheme(), "Quick Load",
                "Load quick save slot " + latest.slot() + "?",
                "Any unsaved progress will be lost.",
                result -> {
                    if (result == DialogMessages.Result.OK) {
                        // Reuse the Save screen's exact restore-then-navigate path so quick load
                        // behaves identically to a normal load.
                        SaveScreen.triggerLoad(context, latest);
                    }
                });
    }

    // ----- Auto save ----------------------------------------------------------------------

    /** Silent auto save into the rotating {@link SaveSlotCategory#AUTO} buffer (default slots),
     *  gated on the host's {@code save.autoSaveDaily} preference.  Returns true when a save was
     *  actually written.  Intended for a host-defined trigger such as end-of-day. */
    public static boolean autoSaveIfEnabled(RouteContext context) {
        return autoSaveIfEnabled(context, DEFAULT_SLOT_COUNT);
    }

    /** Preference-gated variant of {@link #autoSave} — no-op (returns false) when the
     *  {@code save.autoSaveDaily} preference is off. */
    public static boolean autoSaveIfEnabled(RouteContext context, int slotCount) {
        if (context == null || context.preferencesService() == null
                || !context.preferencesService().autoSaveDaily()) {
            return false;
        }
        return autoSave(context, slotCount);
    }

    /** Silent auto save into the rotating {@link SaveSlotCategory#AUTO} buffer (default slots),
     *  regardless of preference.  Returns true when a save was written. */
    public static boolean autoSave(RouteContext context) {
        return autoSave(context, DEFAULT_SLOT_COUNT);
    }

    /** Silent auto save into the rotating {@link SaveSlotCategory#AUTO} buffer.  No dialog — it
     *  must never interrupt the moment that triggered it. */
    public static boolean autoSave(RouteContext context, int slotCount) {
        if (!canSave(context)) {
            return false;
        }
        int slots = normaliseSlotCount(slotCount);
        Scene scene = activeScene(context);
        int slot = writeRotating(context, SaveSlotCategory.AUTO, slots, "Auto save — ", scene);
        return slot > 0;
    }

    // ----- Shared machinery ---------------------------------------------------------------

    /** Writes the current game state into the next rotating slot of {@code category}.  Returns
     *  the written slot number, or {@code -1} on failure (logged, never thrown). */
    private static int writeRotating(RouteContext context, SaveSlotCategory category,
                                     int slotCount, String descriptionPrefix, Scene scene) {
        SaveLoadService service = context.saveLoadService();
        int slot = nextSlot(service, category, slotCount);
        String callerRoute = context.activeRouteId();
        String gameDate = SaveScreen.currentGameDateString();
        com.eb.javafx.gamesupport.GameDateTime gameDateTime = SaveScreen.currentGameDateTime();
        WritableImage thumbnail = scene == null ? null : scene.snapshot(null);
        try {
            service.writeSlotSummary(category, slot, context.gameState(),
                    descriptionPrefix + (gameDate == null || gameDate.isBlank()
                            ? Integer.toString(slot) : gameDate),
                    Instant.now(), thumbnail, callerRoute, gameDate);
            // Full gameplay-state snapshot — required so loading restores actual state, not just
            // navigation metadata.  Skipped only when the game clock isn't initialised yet.
            if (gameDateTime != null) {
                service.captureSlotSnapshot(category, slot, context.gameState(), gameDateTime);
            }
            return slot;
        } catch (RuntimeException ex) {
            System.err.println("[QuickSaveActions] " + category + " save to slot " + slot
                    + " failed: " + ex);
            return -1;
        }
    }

    /** Next rotating slot: one past the most-recently-written slot in {@code category},
     *  wrapping {@code slotCount → 1}.  Starts at slot 1 when the buffer is empty. */
    public static int nextSlot(SaveLoadService service, SaveSlotCategory category, int slotCount) {
        int slots = normaliseSlotCount(slotCount);
        SaveSlotSummary latest = latest(service, category, slots);
        if (latest == null) {
            return 1;
        }
        return (latest.slot() % slots) + 1;
    }

    /** The most-recently-written save (by timestamp) in {@code category} within the rotating
     *  range {@code [1, slotCount]}, or null when none exist. */
    public static SaveSlotSummary latest(SaveLoadService service, SaveSlotCategory category, int slotCount) {
        int slots = normaliseSlotCount(slotCount);
        List<SaveSlotSummary> summaries = service.listSlotSummaries(category);
        return summaries.stream()
                .filter(summary -> summary.slot() >= 1 && summary.slot() <= slots)
                .max(Comparator.comparing(SaveSlotSummary::savedAt))
                .orElse(null);
    }

    private static boolean canSave(RouteContext context) {
        return context != null
                && context.saveLoadService() != null
                && context.gameState() != null;
    }

    private static Scene activeScene(RouteContext context) {
        return context.primaryStage() == null ? null : context.primaryStage().getScene();
    }

    private static int normaliseSlotCount(int slotCount) {
        return Math.max(1, slotCount);
    }
}
