package com.eb.javafx.ui;

import com.eb.javafx.debug.DebugScreenInspector;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.SaveScreenViewMode;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.save.SaveLoadService.SaveSlotCategory;
import com.eb.javafx.save.SaveLoadService.SaveSlotSummary;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Save screen modeled on {@link PreferencesSummaryScreen} — same shell, same panel chrome,
 * same close-action behaviour.  Replaces the placeholder {@link SaveLoadSummaryScreen} as the
 * route surface for {@link SceneRouter#SAVE_LOAD_ROUTE}.
 *
 * <h2>Layout</h2>
 * <ul>
 *   <li><b>Top</b> — "Show as list" checkbox that toggles between grid and list presentation.
 *       Default (unchecked) is grid view.  Persisted via
 *       {@link PreferencesService#saveSaveScreenViewMode}.</li>
 *   <li><b>Centre</b> — either:
 *     <ul>
 *       <li><b>Grid</b> — 5 columns × 4 rows = 20 tiled slot cards.  Each card shows the slot
 *           number, the thumbnail captured when the save was written, the description, and the
 *           real wall-clock date / time.</li>
 *       <li><b>List</b> — vertical rows, one per slot, with columns for slot number /
 *           description / saved-at / schema version and the thumbnail pinned on the right.</li>
 *     </ul>
 *   </li>
 *   <li><b>Bottom</b> — "Auto save daily" checkbox + Close button.  Auto-save preference
 *       persists via {@link PreferencesService#saveAutoSaveDaily}.</li>
 * </ul>
 *
 * <h2>Save action</h2>
 *
 * <p>Clicking a slot card or list row writes the current game state to that slot.  Filled slots
 * prompt for overwrite confirmation; empty slots save immediately.  Both branches capture a
 * screenshot of the most recent gameplay scene via {@link SaveLoadService#captureSlotThumbnail}
 * so the tile rendering after save shows the live preview.</p>
 */
public final class SaveScreen {

    /** Number of slot tiles per row in the grid view. */
    public static final int GRID_COLUMNS = 5;

    /** Number of slot tiles per column in the grid view (so the screen holds 5×3 = 15 saves). */
    public static final int GRID_ROWS = 3;

    /** Total tiles shown by the grid view (and therefore the number of save slots the screen
     *  exposes through its tile UI — additional slots written via {@link SaveLoadService} still
     *  exist but won't appear on the default 5×4 board). */
    public static final int SLOT_COUNT = GRID_COLUMNS * GRID_ROWS;

    /** Outer tile size for grid view (close to 16:9 — 350/197 ≈ 1.777, vs the exact
     *  16:9 ratio of 1.778, a difference well below 1 px at this scale).  Width is
     *  fixed at 350 px per the design ask; the height of 197 keeps the picture in
     *  near-perfect 16:9 so the underlying screenshots cover the tile face without
     *  visible letterboxing.  Band width is 5 × 350 + 4 × 6 (hgap) = 1774 px, so the
     *  grid fits in any window ≥ ~1850 px wide (the load-from-admin window has been
     *  bumped to 1920 to match). */
    private static final int DEFAULT_GRID_THUMB_WIDTH = 350;
    private static final int DEFAULT_GRID_THUMB_HEIGHT = 197;
    // Config-overridable (save.gridThumbnailWidth/Height, applied at boot).  Non-final so a mod
    // can resize the grid tiles; defaults preserve the 350×197 (≈16:9) layout above.
    private static int GRID_THUMB_WIDTH = DEFAULT_GRID_THUMB_WIDTH;
    private static int GRID_THUMB_HEIGHT = DEFAULT_GRID_THUMB_HEIGHT;

    /** Inset between the tile border and the thumbnail image — the picture itself
     *  shouldn't touch the tile border (delete pip + caption strip still do).  The
     *  image rectangle becomes (GRID_THUMB_WIDTH − 2×IMAGE_INSET) by
     *  (GRID_THUMB_HEIGHT − 2×IMAGE_INSET); with preserveRatio on, a 16:9 source
     *  letterboxes a couple of pixels left/right inside that frame, which is fine. */
    private static final int IMAGE_INSET = 4;

    /** Maximum number of pages a player can spawn via the "+" chip.  Each page exposes
     *  {@link #SLOT_COUNT} (15) tiles, so 10 pages = 150 slots per category — well
     *  beyond what any actual playthrough needs but a finite cap keeps the page strip
     *  from growing without bound and the on-disk save directory predictable. */
    private static final int MAX_PAGES = 10;

    /** Thumbnail size for list rows (smaller — sits to the right of the text columns). */
    private static final int DEFAULT_LIST_THUMB_WIDTH = 96;
    private static final int DEFAULT_LIST_THUMB_HEIGHT = 54;
    // Config-overridable (save.listThumbnailWidth/Height, applied at boot).
    private static int LIST_THUMB_WIDTH = DEFAULT_LIST_THUMB_WIDTH;
    private static int LIST_THUMB_HEIGHT = DEFAULT_LIST_THUMB_HEIGHT;

    /** Overrides the save-tile thumbnail dimensions (the {@code save.gridThumbnail*} /
     *  {@code save.listThumbnail*} config). Null / non-positive args keep the current value.
     *  Called once at boot. */
    public static void setThumbnailSizes(Integer gridWidth, Integer gridHeight,
                                         Integer listWidth, Integer listHeight) {
        if (gridWidth != null && gridWidth > 0)   { GRID_THUMB_WIDTH = gridWidth; }
        if (gridHeight != null && gridHeight > 0) { GRID_THUMB_HEIGHT = gridHeight; }
        if (listWidth != null && listWidth > 0)   { LIST_THUMB_WIDTH = listWidth; }
        if (listHeight != null && listHeight > 0) { LIST_THUMB_HEIGHT = listHeight; }
    }

    /** The current grid-tile thumbnail width (px) — drives the persisted-thumbnail resolution so
     *  the saved preview matches whatever tile size is configured. */
    public static int gridThumbnailWidth() {
        return GRID_THUMB_WIDTH;
    }

    /** The current grid-tile thumbnail height (px). */
    public static int gridThumbnailHeight() {
        return GRID_THUMB_HEIGHT;
    }

    private static final String PREFERENCES_ID = "preferences";
    private static final Set<String> ENABLED_FOOTER_IDS = Set.of(PREFERENCES_ID);

    /** Date / time format used in both views.  Local zone, locale-default formatting. */
    private static final DateTimeFormatter SAVED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Whether the screen is in Save mode (clicking a slot writes the current game state
     *  to it) or Load mode (clicking a filled slot loads it).  Toggled by the Save / Load
     *  header at the top of the screen — see the mode-label pair built in
     *  {@link #createScene}. */
    public enum SaveLoadMode { SAVE, LOAD }

    /** One-shot baton holding the caller's pre-captured scene snapshot for the next
     *  {@link #createScene} invocation.  Hosts that navigate to the save screen should call
     *  {@link #prepareCallerSnapshot} on their own scene right before the navigation so the
     *  save screen can persist a thumbnail of the gameplay scene the player came from
     *  rather than a thumbnail of the save screen itself.  {@code createScene} reads + clears
     *  this field once, so unconsumed snapshots never linger across screens. */
    private static WritableImage pendingCallerSnapshot;

    /** One-shot baton holding the {@link SaveLoadMode} the next {@link #createScene} call
     *  should open in.  Set by {@link #prepareInitialMode} from the caller — typically a
     *  footer-button click handler that wants the screen to open ready to save (from a Save
     *  button) or ready to load (from a Load button).  Defaults to {@link SaveLoadMode#SAVE}
     *  when the caller didn't call prepare.  Cleared on read by {@code createScene}. */
    private static SaveLoadMode pendingInitialMode;

    /** One-shot baton holding the route id the caller wants saves made from this screen
     *  session to record as their startup route.  Set by {@link #prepareCallerRoute} from
     *  the caller (typically {@code routeContext.activeRouteId()} captured before
     *  navigating to the save screen).  Used so loading a save returns the player to the
     *  gameplay route they were on when they saved, not the boot startup route.  Null
     *  falls through to the existing {@code gameState.startupRoute()} default. */
    private static String pendingCallerRoute;

    /** Host-supplied default save-name source.  Consulted by {@link #defaultSaveName} when
     *  an empty slot is about to be written — gives the host a chance to substitute a
     *  game-context-aware default (current map location, current scene, current chapter,
     *  …) for the engine's generic "{Category} {Slot} — {timestamp}" fallback.  Registered
     *  once at boot via {@link #setDefaultSaveNameSupplier}; null leaves the fallback in
     *  place (engines that don't care about the default just don't register one). */
    private static java.util.function.Supplier<String> defaultSaveNameSupplier;

    /**
     * Registers a host-supplied source of default save names.  The supplier is invoked
     * every time the save-name prompt opens for an empty slot (overwriting an existing
     * slot still defaults to that slot's recorded name).  Returning {@code null} or a
     * blank string from the supplier falls back to the engine's generic default.
     *
     * <p>Pass {@code null} to unregister.  Thread-safety: caller is responsible for
     * setting this on the JavaFX application thread, same constraint as every other
     * save-screen static.</p>
     */
    public static void setDefaultSaveNameSupplier(java.util.function.Supplier<String> supplier) {
        defaultSaveNameSupplier = supplier;
    }

    /** Host-supplied source of the in-game date / time label that gets persisted with
     *  every save (and surfaced in the list view's Game-date column).  Returning null /
     *  blank persists an empty value, which the list view then renders as blank — no
     *  schema bump required for hosts that haven't wired the supplier yet. */
    private static java.util.function.Supplier<String> gameDateSupplier;

    /** Registers the in-game date supplier — see {@link #gameDateSupplier}.  Pass null
     *  to unregister.  Called on the JFX thread, same constraint as the other batons. */
    public static void setGameDateSupplier(java.util.function.Supplier<String> supplier) {
        gameDateSupplier = supplier;
    }

    /** Host-supplied source of the live {@link com.eb.javafx.gamesupport.GameDateTime}
     *  used as the snapshot timestamp when capturing a full save.  Distinct from
     *  {@link #gameDateSupplier} (which returns a display string for the list column)
     *  because the snapshot machinery needs the structured {@code day + slotId} pair to
     *  feed {@link com.eb.javafx.state.GameState#snapshot}.  When null or returning
     *  null, the snapshot capture step is skipped and the slot writes metadata only —
     *  loading such a slot won't restore gameplay state (legacy / menu-only saves). */
    private static java.util.function.Supplier<com.eb.javafx.gamesupport.GameDateTime>
            gameDateTimeSupplier;

    /** Registers the snapshot-time supplier — see {@link #gameDateTimeSupplier}.  Pass
     *  null to unregister.  Called on the JFX thread. */
    public static void setGameDateTimeSupplier(
            java.util.function.Supplier<com.eb.javafx.gamesupport.GameDateTime> supplier) {
        gameDateTimeSupplier = supplier;
    }

    /** Reads the GameDateTime supplier with try/catch isolation — returns null when no
     *  supplier is registered, it returned null, or it threw.  Callers should skip the
     *  snapshot-capture step on null without aborting the save (metadata still writes). */
    static com.eb.javafx.gamesupport.GameDateTime currentGameDateTime() {
        java.util.function.Supplier<com.eb.javafx.gamesupport.GameDateTime> supplier = gameDateTimeSupplier;
        if (supplier == null) {
            return null;
        }
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            System.err.println("[SaveScreen] GameDateTime supplier threw: " + ex);
            return null;
        }
    }

    /** Reads the supplier with try/catch isolation so a host bug can't break the save
     *  write path.  Returns empty string when no supplier is registered, the supplier
     *  returned null/blank, or the supplier threw. */
    static String currentGameDateString() {
        java.util.function.Supplier<String> supplier = gameDateSupplier;
        if (supplier == null) {
            return "";
        }
        try {
            String value = supplier.get();
            return value == null ? "" : value;
        } catch (RuntimeException ex) {
            System.err.println("[SaveScreen] Game-date supplier threw: " + ex);
            return "";
        }
    }

    /**
     * Captures a snapshot of {@code callerScene} and stashes it for the next
     * {@link #createScene} call to use as the save-thumbnail source.  Call this from the
     * host's save / load footer button handler immediately before
     * {@code context.pushAndNavigateTo(SAVE_LOAD_ROUTE)} so the resulting save records the
     * scene the player was looking at when they clicked the button.
     *
     * <p>Snapshot timing matters: the call must be on the JavaFX application thread and
     * before the scene root is reparented onto the save screen (which is what {@code
     * pushAndNavigateTo} does internally).  Passing {@code null} clears any pending
     * snapshot — useful when a previous prepare call left a stale baton behind.</p>
     */
    public static void prepareCallerSnapshot(javafx.scene.Scene callerScene) {
        pendingCallerSnapshot = callerScene == null ? null : callerScene.snapshot(null);
    }

    /**
     * Sets the initial mode the next {@link #createScene} call should open in.  Passing
     * {@link SaveLoadMode#LOAD} makes the screen open with the Load word highlighted and
     * tile clicks routed to load-by-route; passing {@link SaveLoadMode#SAVE} (or never
     * calling this) keeps the default save-on-click behaviour.
     *
     * <p>Pairs with {@link #prepareCallerSnapshot}: a host that wires both a Load button
     * and a Save button typically calls prepare-snapshot in both branches and prepare-mode
     * with the matching mode in each branch, so the screen always opens with both the
     * right thumbnail source and the right action target.</p>
     */
    public static void prepareInitialMode(SaveLoadMode initialMode) {
        pendingInitialMode = initialMode;
    }

    /**
     * Stashes the route id that the next {@link #createScene} call should record as the
     * target route for saves made during its session.  Hosts should pass
     * {@code routeContext.activeRouteId()} (the gameplay route the player is currently on)
     * immediately before navigating to the save screen so that {@link SaveSlotSummary
     * #startupRoute()} on saves later resolves to gameplay rather than the immutable boot
     * startup route on {@code GameState}.  Pass {@code null} to clear.
     */
    public static void prepareCallerRoute(String callerRoute) {
        pendingCallerRoute = callerRoute;
    }

    private SaveScreen() {
    }

    public static Scene createScene(RouteContext context) {
        return createScene(context, context.preferencesService().windowWidth(),
                context.preferencesService().windowHeight());
    }

    static Scene createScene(RouteContext context, double width, double height) {
        Runnable closeAction = () -> {
            if (!context.navigateBack()) {
                context.navigateTo(SceneRouter.MAIN_MENU_ROUTE);
            }
        };

        // Consume the one-shot caller snapshot (see prepareCallerSnapshot) so every save
        // triggered from this screen instance uses the same gameplay-screen thumbnail.
        // Clearing the static after read prevents a stale snapshot from leaking into a
        // future save-screen open if no caller calls prepare again.
        final WritableImage callerSnapshot = pendingCallerSnapshot;
        pendingCallerSnapshot = null;

        // Consume the one-shot initial-mode baton — same lifecycle as the snapshot baton.
        // Defaults to SAVE when no caller called prepareInitialMode (back-compat for hosts
        // that haven't adopted the prepare pattern yet).
        final SaveLoadMode initialScreenMode = pendingInitialMode == null
                ? SaveLoadMode.SAVE
                : pendingInitialMode;
        pendingInitialMode = null;

        // Consume the one-shot caller-route baton — used as the target route persisted on
        // saves written from this session (so loading later navigates back to gameplay).
        // Null falls through to gameState.startupRoute() inside SaveLoadService.
        final String callerRoute = pendingCallerRoute;
        pendingCallerRoute = null;

        // ---- Top: "Save / Load" mode header ----------------------------------------
        // Two clickable Labels separated by a slash.  The active mode renders in white with
        // a soft drop shadow; the inactive mode renders in muted grey with no effect.  Click
        // either to switch modes — SAVE writes to a slot, LOAD navigates to the slot's
        // recorded startupRoute.  Font family is "Nasalization Rg" (AltLife pre-loads this
        // via FontResources.load("Nasalization Rg.otf")); other hosts that haven't loaded
        // it fall through to sans-serif.  This header REPLACES the ScreenShell title bar
        // below (root.setTop(modeHeader)) so the screen has a single heading instead of
        // both a small "Save" title and the big mode toggle.
        SimpleObjectProperty<SaveLoadMode> mode = new SimpleObjectProperty<>(initialScreenMode);
        Label saveModeLabel = new Label(screenText("mode.save"));
        Label loadModeLabel = new Label(screenText("mode.load"));
        Label modeSeparator = new Label(" / ");
        for (Label lbl : new Label[]{saveModeLabel, loadModeLabel, modeSeparator}) {
            // Font size lives in CSS (.save-screen-mode-title) so it scales with the global
            // Text-size accessibility setting and is mod-overridable; family/weight stay inline.
            lbl.getStyleClass().add("save-screen-mode-title");
            lbl.setStyle("-fx-font-family: 'Nasalization Rg', sans-serif; -fx-font-weight: bold;");
        }
        modeSeparator.setStyle(modeSeparator.getStyle() + " -fx-text-fill: #777;");
        saveModeLabel.setOnMouseClicked(event -> mode.set(SaveLoadMode.SAVE));
        loadModeLabel.setOnMouseClicked(event -> mode.set(SaveLoadMode.LOAD));
        // Click-colour feedback: while a Save / Load word is pressed it flashes the theme accent
        // colour, so the click registers visually even when it doesn't change the mode (re-clicking
        // the already-active word).  On release the proper active / inactive style is restored.
        String modeClickColor = context.uiTheme() == null || context.uiTheme().accentColor() == null
                || context.uiTheme().accentColor().isBlank()
                        ? "#ffd54a" : context.uiTheme().accentColor();
        installModeClickColor(saveModeLabel, mode, SaveLoadMode.SAVE, modeClickColor);
        installModeClickColor(loadModeLabel, mode, SaveLoadMode.LOAD, modeClickColor);
        // Initial highlight reflects the prepared mode — SAVE open → Save word lit,
        // LOAD open → Load word lit.  The mode-property change listener installed below
        // keeps the two labels in sync as the player switches modes mid-session.
        applyModeStyle(saveModeLabel, initialScreenMode == SaveLoadMode.SAVE);
        applyModeStyle(loadModeLabel, initialScreenMode == SaveLoadMode.LOAD);

        HBox modeHeader = new HBox(0, saveModeLabel, modeSeparator, loadModeLabel);
        modeHeader.setAlignment(Pos.CENTER);
        modeHeader.setPadding(new Insets(4, 0, 8, 0));

        // ---- "Show as list" view-mode checkbox -------------------------------------
        SaveScreenViewMode initialViewMode = context.preferencesService().saveScreenViewMode();
        CheckBox listToggle = new CheckBox(screenText("item.show-as-list.label"));
        listToggle.setSelected(initialViewMode == SaveScreenViewMode.LIST);

        // ---- Centre slot area: one tab per save category -------------------------------
        // Each tab carries its own block-wrapped VBox container.  Tabs are AltLife-style
        // (see save-screen-tabs CSS class in default.css / UiTheme generated stylesheet)
        // and the inner block draws a tinted rounded panel around the slot grid so the
        // tile cluster reads as a unit instead of floating against the scene background.
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("save-screen-tabs");
        String accentHex = context.uiTheme() == null ? "#888888" : context.uiTheme().accentColor();
        tabPane.setStyle(
                "-fx-base: " + accentHex + ";"
                + " -fx-accent: " + accentHex + ";"
                + " -fx-focus-color: " + accentHex + ";"
                + " -fx-faint-focus-color: transparent;");

        // Captured during tab construction so the auto-save-daily checkbox can bind its
        // visibility to "is the Auto tab selected" (the checkbox only makes sense there).
        final Tab[] autoTab = new Tab[1];
        Map<SaveSlotCategory, VBox> categoryContainers = new java.util.EnumMap<>(SaveSlotCategory.class);
        Map<SaveSlotCategory, String> categoryLabels = Map.of(
                SaveSlotCategory.NORMAL, screenText("tab.normal"),
                SaveSlotCategory.QUICK,  screenText("tab.quick"),
                SaveSlotCategory.AUTO,   screenText("tab.auto"));
        for (SaveSlotCategory cat : SaveSlotCategory.values()) {
            VBox container = new VBox(8);
            container.setPadding(new Insets(8));
            categoryContainers.put(cat, container);
            // The "block" — tinted background + rounded corners + faint border, like the
            // AltLife startup-options style.  Mirrors the layout-card look from
            // ScreenShell.LAYOUT_CARD_STYLE_CLASS without coupling to that class so the
            // styling tweak (e.g. wider border, different tint) stays scoped here.
            // Background + border pulled from the active UiTheme so the panel chrome
            // tracks theme switches alongside the tiles inside it.
            UiTheme blockTheme = context.uiTheme();
            String blockPanel  = blockTheme == null || blockTheme.panelBackground() == null
                    || blockTheme.panelBackground().isBlank()
                            ? "rgba(20, 30, 24, 0.45)" : blockTheme.panelBackground();
            String blockBorder = blockTheme == null || blockTheme.accentColor() == null
                    || blockTheme.accentColor().isBlank()
                            ? "rgba(255, 255, 255, 0.15)" : blockTheme.accentColor();
            VBox block = new VBox(container);
            block.getStyleClass().add("save-grid-block");
            // Corner radius lives in CSS (.save-grid-block); colours/padding stay inline.
            block.setStyle(
                    "-fx-background-color: " + blockPanel + ";"
                    + " -fx-border-color: " + blockBorder + ";"
                    + " -fx-border-width: 1;"
                    + " -fx-padding: 8;");
            // ScrollPane wraps the block so the list view (when active) remains scrollable
            // on small windows — the grid view is sized to fit within 1080 without
            // scrolling, but the list view always benefits from a fallback.
            ScrollPane tabScroll = new ScrollPane(block);
            tabScroll.setFitToWidth(true);
            tabScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            tabScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            tabScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            tabScroll.getStyleClass().add("engine-slim-scrollbar");
            Tab tab = new Tab(categoryLabels.get(cat), tabScroll);
            tabPane.getTabs().add(tab);
            if (cat == SaveSlotCategory.AUTO) {
                autoTab[0] = tab;
            }
        }

        // ---- Pagination state ------------------------------------------------------
        // Each page exposes the same SLOT_COUNT (5×3 = 15) tiles, but the underlying
        // slot numbers shift by SLOT_COUNT per page — page 2 maps to slots 16..30,
        // page 3 to 31..45, etc.  pageCount caps at MAX_PAGES (10) so a player can
        // accumulate up to 150 slots per category before the "+" button hides.
        // selectedPage drives which slot range is currently displayed.  Both are persisted to
        // global preferences (save.pageCount / save.selectedPage) so pages the player spawns
        // via the "+" chip — and the page they were last looking at — survive a restart.
        int storedPageCount = Math.min(MAX_PAGES,
                Math.max(1, context.preferencesService().saveScreenPageCount()));
        int storedSelectedPage = Math.min(storedPageCount,
                Math.max(1, context.preferencesService().saveScreenSelectedPage()));
        javafx.beans.property.IntegerProperty pageCount    = new javafx.beans.property.SimpleIntegerProperty(storedPageCount);
        javafx.beans.property.IntegerProperty selectedPage = new javafx.beans.property.SimpleIntegerProperty(storedSelectedPage);
        // Persist every page-count change (the "+" chip increments it) to global setup.
        pageCount.addListener((obs, was, is) ->
                context.preferencesService().saveSaveScreenPageCount(is.intValue()));
        // Persist the active page so the screen re-opens on it next session.
        selectedPage.addListener((obs, was, is) ->
                context.preferencesService().saveSaveScreenSelectedPage(is.intValue()));

        Runnable rebuildAllTabs = () -> {
            int page = Math.max(1, selectedPage.get());
            for (SaveSlotCategory cat : SaveSlotCategory.values()) {
                rebuildCategory(cat, categoryContainers.get(cat), listToggle, context, mode.get(),
                        callerSnapshot, callerRoute, page);
            }
        };
        listToggle.selectedProperty().addListener((obs, was, is) -> {
            context.preferencesService().saveSaveScreenViewMode(
                    is ? SaveScreenViewMode.LIST : SaveScreenViewMode.GRID);
            rebuildAllTabs.run();
        });
        mode.addListener((obs, was, is) -> {
            applyModeStyle(saveModeLabel, is == SaveLoadMode.SAVE);
            applyModeStyle(loadModeLabel, is == SaveLoadMode.LOAD);
            rebuildAllTabs.run();
        });
        selectedPage.addListener((obs, was, is) -> rebuildAllTabs.run());
        rebuildAllTabs.run();

        // ---- Page strip ------------------------------------------------------------
        // "Page : [1] [2] ... [+]" pinned to the right of a row below the tab block.
        // Selected page draws a rounded accent border + filled accent background;
        // hover lights the background on un-selected pages.  Rebuilt whenever
        // pageCount changes so the "+" button moves as new pages are added.
        UiTheme stripTheme = context.uiTheme();
        String stripAccent = stripTheme == null || stripTheme.accentColor() == null
                || stripTheme.accentColor().isBlank()
                        ? "#143869" : stripTheme.accentColor();
        String stripText = stripTheme == null || stripTheme.textColor() == null
                || stripTheme.textColor().isBlank()
                        ? "#e6e6e6" : stripTheme.textColor();
        HBox pageStrip = new HBox(6);
        pageStrip.setAlignment(Pos.CENTER_RIGHT);
        pageStrip.setPadding(new Insets(6, 12, 6, 12));
        Label pageLabel = new Label("Page :");
        // Font size lives in CSS (.save-screen-page-label); colour stays inline (strip-themed).
        pageLabel.getStyleClass().add("save-screen-page-label");
        pageLabel.setStyle("-fx-text-fill: " + stripText + "; -fx-font-weight: bold;");
        Runnable rebuildPageStrip = () -> {
            pageStrip.getChildren().clear();
            pageStrip.getChildren().add(pageLabel);
            int count = pageCount.get();
            for (int i = 1; i <= count; i++) {
                int pageNum = i;
                pageStrip.getChildren().add(buildPageChip(pageNum, selectedPage, stripAccent, stripText));
            }
            if (count < MAX_PAGES) {
                pageStrip.getChildren().add(buildAddPageChip(pageCount, selectedPage, stripAccent, stripText));
            }
        };
        pageCount.addListener((obs, was, is) -> rebuildPageStrip.run());
        // selectedPage listener also re-renders the strip so the rounded border
        // moves to the newly active chip — without this the previously-selected
        // chip keeps its border because the chips don't share state.
        selectedPage.addListener((obs, was, is) -> rebuildPageStrip.run());
        rebuildPageStrip.run();

        // The "Show as list" checkbox sits above the tab strip — the big Save/Load mode
        // header has been promoted out of the content area into the BorderPane shell's
        // top slot (see root.setTop below), so this top block only carries the secondary
        // view-mode toggle now.
        VBox topBlock = new VBox(4, listToggle);
        topBlock.setAlignment(Pos.CENTER);
        topBlock.setPadding(new Insets(4, 8, 4, 8));

        BorderPane centre = new BorderPane();
        centre.setTop(topBlock);
        centre.setCenter(tabPane);
        // Page strip sits directly under the tab block, right-aligned so the active
        // page indicator and the "+" button land next to the screen edge.
        centre.setBottom(pageStrip);

        // ---- Bottom auto-save (left) + back button (centre) ---------------------------
        CheckBox autoSaveDaily = new CheckBox(screenText("item.auto-save-daily.label"));
        autoSaveDaily.setSelected(context.preferencesService().autoSaveDaily());
        autoSaveDaily.selectedProperty().addListener((obs, was, is) ->
                context.preferencesService().saveAutoSaveDaily(is));
        // The checkbox only applies to auto-saves, so it's shown only while the Auto tab is
        // the active tab.  managed follows visible so the bottom strip reclaims the space
        // (keeping the Back button centred) when it's hidden.
        Runnable refreshAutoSaveVisibility = () -> {
            boolean onAutoTab = autoTab[0] != null
                    && tabPane.getSelectionModel().getSelectedItem() == autoTab[0];
            autoSaveDaily.setVisible(onAutoTab);
            autoSaveDaily.setManaged(onAutoTab);
        };
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, was, is) ->
                refreshAutoSaveVisibility.run());
        refreshAutoSaveVisibility.run();

        // Bigger back button than the other nav chips — set the pref size BEFORE applying the
        // artwork so the SVG button art rasterises to these dimensions (applyArtwork reads the
        // button's pref size as its fixed size).
        Button backButton = new Button(screenText("item.back.label"));
        backButton.setPrefSize(240, 64);
        ButtonStyling.applyDefaultShapeArtwork(backButton);
        backButton.setOnAction(event -> closeAction.run());

        // BorderPane gives us the centred Back button while keeping the auto-save checkbox
        // anchored on the left.  setCenter aligns the button on the X-midline of the bottom
        // strip regardless of the left-side checkbox's width — same shape the Preferences
        // screen uses for its central button row, just adapted for one button instead of two.
        BorderPane bottomBox = new BorderPane();
        bottomBox.setLeft(autoSaveDaily);
        bottomBox.setCenter(backButton);
        BorderPane.setAlignment(autoSaveDaily, Pos.CENTER_LEFT);
        BorderPane.setAlignment(backButton, Pos.CENTER);
        bottomBox.setPadding(new Insets(12, 12, 12, 12));

        BorderPane contentArea = new BorderPane();
        contentArea.setCenter(centre);
        contentArea.setBottom(bottomBox);

        BorderPane root = ScreenShell.titled(screenText("screen.title"), contentArea, footerOptions());
        // Replace the standard ScreenShell title bar (a small left-aligned "Save" Label) with
        // the big centered Save / Load mode header.  The mode header is the screen's actual
        // heading — the player switches mode by clicking either word — so having both would
        // duplicate the title and waste vertical space.  setTop swaps in our HBox while
        // preserving the body + footer + screen-root margins that .titled set up.
        root.setTop(modeHeader);
        BorderPane.setMargin(modeHeader, new Insets(8, 12, 4, 12));
        HBox footer = (HBox) root.getBottom();
        ScreenShell.applyFooterPreferences(footer, context.preferencesService(), context.uiTheme());
        wireFooter(footer, closeAction);

        Scene scene = themedSaveScene(context, root, width, height);
        scene.getStylesheets().add(context.uiTheme().stylesheet());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.P) {
                closeAction.run();
                event.consume();
            }
        });
        DebugScreenInspector.setScreenClass(scene, SaveScreen.class);
        return scene;
    }

    /** Rebuilds the slot view for a single category tab using the current toggle state +
     *  save/load mode + page.  The page flows down so a slot at index {@code i} in the
     *  rendered grid corresponds to actual slot number {@code (page-1)*SLOT_COUNT + i + 1}
     *  on disk — page 2 thus addresses slots 16..30, page 3 31..45, etc. */
    private static void rebuildCategory(SaveSlotCategory category, VBox slotContainer,
                                         CheckBox listToggle, RouteContext context,
                                         SaveLoadMode screenMode, WritableImage callerSnapshot,
                                         String callerRoute, int page) {
        SaveScreenViewMode viewMode = listToggle.isSelected()
                ? SaveScreenViewMode.LIST
                : SaveScreenViewMode.GRID;
        Map<Integer, SaveSlotSummary> summaries = indexSummariesBySlot(context.saveLoadService(), category);
        slotContainer.getChildren().clear();
        Runnable refresh = () -> rebuildCategory(category, slotContainer, listToggle, context,
                screenMode, callerSnapshot, callerRoute, page);
        if (viewMode == SaveScreenViewMode.GRID) {
            slotContainer.getChildren().add(buildGrid(
                    context, category, summaries, refresh, screenMode, callerSnapshot, callerRoute, page));
        } else {
            slotContainer.getChildren().add(buildList(
                    context, category, summaries, refresh, screenMode, callerSnapshot, callerRoute, page));
        }
    }

    /** Paints the active mode label white with a soft drop shadow + scale-up, paints the
     *  inactive mode label in muted grey with no effect.  Cursor is hand on both so the
     *  player can switch by clicking either. */
    /** Adds pressed-state colour feedback to a Save / Load mode word: while the mouse is held the
     *  glyph fills with {@code clickColor}; on release the proper active / inactive style is
     *  restored (from the live {@code mode}).  Gives the words a visible "click colour" even when
     *  the click doesn't change the mode. */
    private static void installModeClickColor(Label label,
                                              SimpleObjectProperty<SaveLoadMode> mode,
                                              SaveLoadMode thisMode, String clickColor) {
        label.setOnMousePressed(event -> label.setStyle(
                "-fx-font-family: 'Nasalization Rg', sans-serif;"
                + " -fx-font-size: 36px;"
                + " -fx-font-weight: bold;"
                + " -fx-text-fill: " + clickColor + ";"
                + " -fx-cursor: hand;"));
        label.setOnMouseReleased(event -> applyModeStyle(label, mode.get() == thisMode));
    }

    private static void applyModeStyle(Label label, boolean active) {
        if (active) {
            label.setStyle(
                    "-fx-font-family: 'Nasalization Rg', sans-serif;"
                    + " -fx-font-size: 36px;"
                    + " -fx-font-weight: bold;"
                    + " -fx-text-fill: white;"
                    + " -fx-cursor: hand;");
            // Soft white drop shadow — sits behind the glyph rather than the side, gives
            // the "highlighted" reading without making the text look bevelled.
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.web("#ffffff", 0.65));
            shadow.setRadius(12.0);
            shadow.setSpread(0.25);
            shadow.setOffsetX(0);
            shadow.setOffsetY(0);
            label.setEffect(shadow);
        } else {
            label.setStyle(
                    "-fx-font-family: 'Nasalization Rg', sans-serif;"
                    + " -fx-font-size: 36px;"
                    + " -fx-font-weight: bold;"
                    + " -fx-text-fill: #777777;"
                    + " -fx-cursor: hand;");
            label.setEffect(null);
        }
    }

    // ---- Grid view ---------------------------------------------------------------------

    private static GridPane buildGrid(RouteContext context,
                                       SaveSlotCategory category,
                                       Map<Integer, SaveSlotSummary> summaries,
                                       Runnable afterSave,
                                       SaveLoadMode screenMode,
                                       WritableImage callerSnapshot,
                                       String callerRoute,
                                       int page) {
        GridPane grid = new GridPane();
        // 6 px gaps + 4 px padding fit the 288 px tiles comfortably: 5 × 288 + 4 × 6 +
        // 2 × 4 (grid padding) = 1472 px content band, leaving the rest for block /
        // container chrome + window side margin.
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPadding(new Insets(4));
        for (int col = 0; col < GRID_COLUMNS; col++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHalignment(HPos.CENTER);
            cc.setPercentWidth(100.0 / GRID_COLUMNS);
            grid.getColumnConstraints().add(cc);
        }
        int pageOffset = Math.max(0, page - 1) * SLOT_COUNT;
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slot = pageOffset + i + 1;
            int col = i % GRID_COLUMNS;
            int row = i / GRID_COLUMNS;
            grid.add(buildGridTile(context, category, slot, summaries.get(slot), afterSave,
                    screenMode, callerSnapshot, callerRoute), col, row);
        }
        return grid;
    }

    private static Node buildGridTile(RouteContext context, SaveSlotCategory category,
                                       int slot, SaveSlotSummary summary, Runnable afterSave,
                                       SaveLoadMode screenMode, WritableImage callerSnapshot,
                                       String callerRoute) {
        // Thumbnail fit-size is the tile size MINUS the IMAGE_INSET on each side, so
        // the picture has a small visible gap to the tile border instead of touching it.
        // The delete pip and caption strip remain flush to their respective edges of the
        // tile (they sit at higher Z in the StackPane and have no margin), so only the
        // image is inset.  StackPane's default child alignment is CENTER, which centers
        // the smaller ImageView inside the larger tile rect — yielding the inset on all
        // four sides without an explicit setMargin call.
        ImageView thumb = thumbnailViewFor(summary,
                GRID_THUMB_WIDTH - 2 * IMAGE_INSET,
                GRID_THUMB_HEIGHT - 2 * IMAGE_INSET);

        // Caption text — same content rules as before (empty slot = "Slot N", filled
        // slot = "Description · saved-at").  Rendered on a translucent dark strip pinned
        // to the bottom of the tile so it reads against any thumbnail without obscuring
        // most of the picture.
        // Pull theme colours up front — used for caption text fill, tile border, and
        // tile background.  Fallbacks match the previous hardcoded values so a host
        // without a configured UiTheme renders the same chrome it did before.
        UiTheme theme = context.uiTheme();
        final String themeText   = (theme != null && theme.textColor()       != null && !theme.textColor().isBlank())
                ? theme.textColor()       : "#e6e6e6";
        final String themeAccent = (theme != null && theme.accentColor()     != null && !theme.accentColor().isBlank())
                ? theme.accentColor()     : "#143869";
        final String themePanel  = (theme != null && theme.panelBackground() != null && !theme.panelBackground().isBlank())
                ? theme.panelBackground() : "rgba(10, 20, 38, 0.65)";

        Label caption;
        final String captionFontSize;
        if (summary == null) {
            caption = new Label(emptyTileLabel(category, slot));
            // Bumped 11 → 13 — the empty-slot label is the only text on the tile so it
            // benefits from a slightly larger face for legibility at small tile sizes.
            captionFontSize = "13";
        } else {
            String description = summary.description().isBlank()
                    ? screenText("label.unnamed-save")
                    : summary.description();
            String savedAt = formatSavedAt(summary.savedAt());
            String combined = savedAt.isEmpty() ? description : description + " · " + savedAt;
            caption = new Label(combined);
            // Bumped 10 → 12 — the filled-slot caption packs description + timestamp on
            // a single ellipsised line, so it stays one step smaller than the empty
            // label but still reads clearly.  Both sizes increase by 2 px from the
            // previous values per the design ask.
            captionFontSize = "12";
        }
        // Inline-style template — fully opaque black strip pinned to the very bottom of
        // the tile, holding the save name + timestamp.  "0% transparency" per the design
        // ask = alpha 1.0, so the strip reads as a solid black band beneath the image
        // rather than a translucent overlay.  Padded 2 px top/bottom, 6 px left/right
        // so the strip stays a single tight line.
        final String captionStripBase =
                "-fx-background-color: black;"
              + " -fx-padding: 2 6 2 6;"
              + " -fx-font-size: " + captionFontSize + "px;";
        // Resting text uses the theme's text colour (matches every other label in the
        // app); hover bumps to plain white so the active tile reads as the current
        // target regardless of which theme variant is active.
        final String captionRestStyle  = captionStripBase + " -fx-text-fill: " + themeText + ";";
        final String captionHoverStyle = captionStripBase + " -fx-text-fill: white;";
        caption.setStyle(captionRestStyle);
        caption.setWrapText(false);
        caption.setMaxWidth(Double.MAX_VALUE);
        caption.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        StackPane tile = new StackPane();
        tile.getStyleClass().add("save-tile");
        tile.setPrefSize(GRID_THUMB_WIDTH, GRID_THUMB_HEIGHT);
        tile.setMinSize(GRID_THUMB_WIDTH, GRID_THUMB_HEIGHT);
        tile.setMaxSize(GRID_THUMB_WIDTH, GRID_THUMB_HEIGHT);
        // Border + background pulled from the active UiTheme so the tile chrome tracks
        // theme switches (forest / dark / etc.) instead of staying locked to the old
        // hardcoded blue palette.  Inline rather than via LAYOUT_CARD_STYLE_CLASS — the
        // shared .layout-card CSS rule sets -fx-padding: 12px which would push the
        // thumbnail / delete pip / caption strip 12 px inward from every edge, and we
        // want them flush to the actual tile rect.  The resting style uses a 1 px border
        // at theme accent; the hover style swaps to a 2 px white border so the active
        // tile reads as the click target.
        // Corner radius lives in CSS (.save-tile); the rest/hover inline styles carry only the
        // state-dependent colours / border width.
        final String tileRestStyle =
                "-fx-background-color: " + themePanel + ";"
              + " -fx-border-color: " + themeAccent + ";"
              + " -fx-border-width: 1;"
              + " -fx-padding: 0;"
              + " -fx-cursor: hand;";
        final String tileHoverStyle =
                "-fx-background-color: " + themePanel + ";"
              + " -fx-border-color: white;"
              + " -fx-border-width: 2;"
              + " -fx-padding: 0;"
              + " -fx-cursor: hand;";
        tile.setStyle(tileRestStyle);
        // Soft white outer glow on hover via DropShadow effect — pairs with the brighter
        // border so the tile reads as elevated above its neighbours rather than just
        // outlined.  The effect is only set/cleared on hover so non-hovered tiles stay
        // flat and the GPU only paints the shadow for the one tile under the cursor.
        javafx.scene.effect.DropShadow hoverGlow = new javafx.scene.effect.DropShadow();
        hoverGlow.setColor(javafx.scene.paint.Color.web("#ffffff", 0.55));
        hoverGlow.setRadius(14.0);
        hoverGlow.setSpread(0.15);
        tile.getChildren().add(thumb);
        StackPane.setAlignment(thumb, Pos.CENTER);

        // Caption strip pinned to BOTTOM_CENTER.  setMaxHeight(USE_PREF_SIZE) is critical:
        // HBox's default maxHeight is unbounded (Double.MAX_VALUE), and StackPane will
        // resize a resizable child up to its max within the area — so without this the
        // HBox would stretch to fill the tile vertically and the caption text would
        // appear vertically centred (with its internal CENTER_LEFT alignment) rather
        // than sitting at the bottom edge.  Pinning maxHeight to USE_PREF_SIZE makes
        // the strip lay out at its content's preferred height (one text line + padding),
        // which lets the BOTTOM_CENTER alignment land the strip flush at the tile bottom.
        HBox captionStrip = new HBox(caption);
        captionStrip.setMaxWidth(Double.MAX_VALUE);
        captionStrip.setMaxHeight(Region.USE_PREF_SIZE);
        captionStrip.setAlignment(Pos.CENTER_LEFT);
        caption.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(caption, Priority.ALWAYS);
        StackPane.setAlignment(captionStrip, Pos.BOTTOM_CENTER);
        // 2 px left/right inset so the black strip doesn't quite reach the tile's left
        // and right borders — exposes a thin sliver of the tile background colour at
        // each end of the strip, framing the caption against the card chrome.  Bottom
        // stays 0 so the strip remains flush with the tile's bottom border.
        StackPane.setMargin(captionStrip, new Insets(0, 2, 0, 2));
        tile.getChildren().add(captionStrip);

        if (summary != null) {
            Button deleteBtn = buildDeleteButton(context, category, slot, afterSave);
            // TOP_RIGHT, zero margin — pip's top-right corner sits AT the tile's
            // top-right corner (i.e. it touches both the top and right borders).
            // With the tile's -fx-padding overridden to 0 (above), the StackPane lays
            // children out against the actual tile rect, not an inset content rect.
            StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(deleteBtn, new Insets(0, 0, 0, 0));
            tile.getChildren().add(deleteBtn);
        }

        // Single-click reveals a centred Save / Load button in the middle of the tile;
        // double-click (or clicking that button) performs the action.  Loading an empty slot
        // is a no-op, so such slots get no button and ignore clicks.
        boolean slotActionable = !(screenMode == SaveLoadMode.LOAD && summary == null);
        Runnable slotAction = () -> handleSlotClick(context, category, slot, summary, afterSave,
                screenMode, callerSnapshot, callerRoute);
        Button slotActionButton = slotActionable
                ? buildSlotActionButton(context, screenMode, slotAction)
                : null;
        if (slotActionButton != null) {
            tile.getChildren().add(slotActionButton);
        }

        // Hover: brighten the tile border to a 2 px white outline, attach a soft white
        // outer glow, and bump the caption text to pure white — three coordinated cues
        // so the hovered tile reads clearly as the click target without dimming
        // surrounding tiles.  Exit reverses all three back to the resting palette.
        tile.setOnMouseEntered(event -> {
            tile.setStyle(tileHoverStyle);
            tile.setEffect(hoverGlow);
            caption.setStyle(captionHoverStyle);
        });
        tile.setOnMouseExited(event -> {
            tile.setStyle(tileRestStyle);
            tile.setEffect(null);
            caption.setStyle(captionRestStyle);
            // Hide the action button again so it doesn't linger on every visited tile.
            if (slotActionButton != null) {
                slotActionButton.setVisible(false);
            }
        });
        // Single click reveals the centred Save / Load button (so a casual click can't
        // trigger a destructive overwrite or a game-discarding load); double-click performs
        // the action directly for players who prefer the shortcut.
        tile.setOnMouseClicked(event -> {
            if (!slotActionable) {
                return;
            }
            if (event.getClickCount() >= 2) {
                slotAction.run();
            } else if (slotActionButton != null) {
                slotActionButton.setVisible(true);
            }
        });
        // NOTE: do not call tile.setStyle(...) here — that would overwrite the inline
        // border / padding / cursor style set above and revert to the default styling
        // (which re-introduces the 12 px padding and the bugs this method just fixed).
        // Cursor:hand is already part of the main setStyle block.
        return tile;
    }

    /** Maps a (category, slot) pair to the empty-slot caption string — "Slot 1",
     *  "Quick 1", "Auto 1" — per the save_text.json keys. */
    private static String emptyTileLabel(SaveSlotCategory category, int slot) {
        String prefixKey = switch (category) {
            case NORMAL -> "label.empty-slot.normal";
            case QUICK  -> "label.empty-slot.quick";
            case AUTO   -> "label.empty-slot.auto";
        };
        return screenText(prefixKey) + " " + slot;
    }

    // ---- List view ---------------------------------------------------------------------

    private static VBox buildList(RouteContext context,
                                   SaveSlotCategory category,
                                   Map<Integer, SaveSlotSummary> summaries,
                                   Runnable afterSave,
                                   SaveLoadMode screenMode,
                                   WritableImage callerSnapshot,
                                   String callerRoute,
                                   int page) {
        VBox rows = new VBox(6);
        rows.setPadding(new Insets(8));
        // Header row.
        rows.getChildren().add(listHeaderRow());
        int pageOffset = Math.max(0, page - 1) * SLOT_COUNT;
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slot = pageOffset + i + 1;
            rows.getChildren().add(buildListRow(
                    context, category, slot, summaries.get(slot), afterSave,
                    screenMode, callerSnapshot, callerRoute));
        }
        return rows;
    }

    private static HBox listHeaderRow() {
        // "Game date" sits between Saved-at (real-world wall-clock) and Size — it's the
        // in-game day/slot label the host supplier persisted at save time, useful for
        // picking the save that matches "where you were in the story" rather than just
        // "when you saved".  Blank for legacy saves written before the field existed.
        HBox header = new HBox(12,
                columnLabel(screenText("column.slot"), 60),
                columnLabel(screenText("column.description"), 240),
                columnLabel(screenText("column.saved-at"), 140),
                columnLabel(screenText("column.game-date"), 140),
                columnLabel(screenText("column.size"), 80),
                spacerColumn(),
                columnLabel(screenText("column.preview"), LIST_THUMB_WIDTH));
        header.setPadding(new Insets(4, 8, 4, 8));
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private static Label columnLabel(String text, int width) {
        Label label = new Label(text);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.getStyleClass().add(ScreenShell.LAYOUT_SECTION_TITLE_STYLE_CLASS);
        return label;
    }

    private static Region spacerColumn() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private static Node buildListRow(RouteContext context, SaveSlotCategory category,
                                       int slot, SaveSlotSummary summary, Runnable afterSave,
                                       SaveLoadMode screenMode, WritableImage callerSnapshot,
                                       String callerRoute) {
        String description;
        String savedAt;
        String gameDate;
        String sizeText;
        if (summary == null) {
            description = screenText("label.empty");
            savedAt = "";
            gameDate = "";
            sizeText = "";
        } else {
            description = summary.description().isBlank()
                    ? screenText("label.unnamed-save")
                    : summary.description();
            savedAt = formatSavedAt(summary.savedAt());
            gameDate = summary.gameDate();
            sizeText = formatSlotSize(context.saveLoadService(), category, slot, summary);
        }
        Label slotCell = rowCell(Integer.toString(slot), 60);
        Label descCell = rowCell(description, 240);
        Label savedCell = rowCell(savedAt, 140);
        Label gameDateCell = rowCell(gameDate, 140);
        Label sizeCell = rowCell(sizeText, 80);
        ImageView thumb = thumbnailViewFor(summary, LIST_THUMB_WIDTH, LIST_THUMB_HEIGHT);

        // Used-slot rows get a delete button pinned to the far right after the preview;
        // empty rows just show the preview placeholder.  Stack puts the button on top of
        // the thumb at TOP_RIGHT so the row's overall width / spacing stays unchanged.
        StackPane previewStack = new StackPane(thumb);
        if (summary != null) {
            Button deleteBtn = buildDeleteButton(context, category, slot, afterSave);
            StackPane.setAlignment(deleteBtn, Pos.TOP_LEFT);
            StackPane.setMargin(deleteBtn, new Insets(0, 0, 0, 0));
            previewStack.getChildren().add(deleteBtn);
        }

        HBox row = new HBox(12, slotCell, descCell, savedCell, gameDateCell, sizeCell, spacerColumn(), previewStack);
        row.setPadding(new Insets(6, 8, 6, 8));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(ScreenShell.LAYOUT_CARD_STYLE_CLASS);
        row.setStyle(row.getStyle() + " -fx-cursor: hand;");

        // Same single-click-reveals / double-click-acts behaviour as the grid tiles: wrap the
        // row in a StackPane so the centred Save / Load button can overlay the middle of it.
        StackPane rowStack = new StackPane(row);
        boolean slotActionable = !(screenMode == SaveLoadMode.LOAD && summary == null);
        Runnable slotAction = () -> handleSlotClick(
                context, category, slot, summary, afterSave, screenMode, callerSnapshot, callerRoute);
        Button slotActionButton = slotActionable
                ? buildSlotActionButton(context, screenMode, slotAction)
                : null;
        if (slotActionButton != null) {
            rowStack.getChildren().add(slotActionButton);
        }
        rowStack.setOnMouseClicked(event -> {
            if (!slotActionable) {
                return;
            }
            if (event.getClickCount() >= 2) {
                slotAction.run();
            } else if (slotActionButton != null) {
                slotActionButton.setVisible(true);
            }
        });
        rowStack.setOnMouseExited(event -> {
            if (slotActionButton != null) {
                slotActionButton.setVisible(false);
            }
        });
        return rowStack;
    }

    private static Label rowCell(String text, int width) {
        Label label = new Label(text);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.getStyleClass().add(ScreenShell.SCREEN_TEXT_STYLE_CLASS);
        label.setWrapText(true);
        return label;
    }

    // ---- Save action -------------------------------------------------------------------

    /** Mode-aware tile click dispatcher.  SAVE → triggerSave (overwrite confirm + write).
     *  LOAD on an empty slot is a no-op (no-op is friendlier than a "cannot load empty slot"
     *  popup since the player can see the slot is empty); LOAD on a filled slot navigates
     *  to the slot's recorded startupRoute via triggerLoad. */
    private static void handleSlotClick(RouteContext context, SaveSlotCategory category, int slot,
                                          SaveSlotSummary existing, Runnable afterSave,
                                          SaveLoadMode screenMode, WritableImage callerSnapshot,
                                          String callerRoute) {
        if (screenMode == SaveLoadMode.LOAD) {
            if (existing != null) {
                // A game is in progress when this screen was opened from a gameplay scene (the host
                // prepared a caller route — see prepareCallerRoute); loading would discard it, so
                // confirm first.  Opened from the main menu (no caller route) → load straight away.
                if (callerRoute != null && !callerRoute.isBlank()
                        && !SceneRouter.MAIN_MENU_ROUTE.equals(callerRoute)) {
                    confirmThenLoad(context, existing);
                } else {
                    triggerLoad(context, existing);
                }
            }
            return;
        }
        triggerSave(context, category, slot, existing, afterSave, callerSnapshot, callerRoute);
    }

    /** Builds the centred "Save" / "Load" button shown in the middle of a slot after a single
     *  click.  Starts hidden; a single click on the slot reveals it, leaving the slot hides it
     *  again, and clicking it — or double-clicking the slot — performs the action.  The button
     *  consumes its own mouse click so it doesn't bubble back to the slot's reveal handler. */
    private static Button buildSlotActionButton(RouteContext context, SaveLoadMode screenMode,
                                                  Runnable act) {
        Button btn = new Button(screenMode == SaveLoadMode.LOAD
                ? screenText("mode.load")
                : screenText("mode.save"));
        UiTheme theme = context.uiTheme();
        String accent = (theme != null && theme.accentColor() != null && !theme.accentColor().isBlank())
                ? theme.accentColor() : "#143869";
        btn.setFocusTraversable(false);
        btn.setVisible(false);
        btn.setStyle(
                "-fx-background-color: " + accent + ";"
              + " -fx-text-fill: white;"
              + " -fx-font-weight: bold;"
              + " -fx-font-size: 15px;"
              + " -fx-padding: 8 24 8 24;"
              + " -fx-background-radius: 6;"
              + " -fx-border-color: white;"
              + " -fx-border-width: 1;"
              + " -fx-border-radius: 6;"
              + " -fx-cursor: hand;");
        btn.setOnAction(e -> act.run());
        // Don't let a click on the button bubble up to the slot's single-click reveal handler.
        btn.setOnMouseClicked(e -> e.consume());
        StackPane.setAlignment(btn, Pos.CENTER);
        return btn;
    }

    /** "Are you sure?" gate before {@link #triggerLoad} when a game is already in progress — uses
     *  the same in-scene {@link DialogMessages} overlay as the delete / overwrite prompts. */
    private static void confirmThenLoad(RouteContext context, SaveSlotSummary summary) {
        Scene activeScene = context.primaryStage() == null ? null : context.primaryStage().getScene();
        DialogMessages.confirm(activeScene, context.uiTheme(),
                screenText("dialog.load.title"),
                screenText("dialog.load.header"),
                screenText("dialog.load.content"),
                result -> {
                    if (result == DialogMessages.Result.OK) {
                        triggerLoad(context, summary);
                    }
                });
    }

    /** Resting / hover background hex codes for the delete pip — pulled out as constants
     *  so the {@link #buildDeleteButton} hover-handlers and the resting style share the
     *  same red and there's a single place to recolour the chrome. */
    private static final String DELETE_BTN_REST_BG  = "#c0392b";
    private static final String DELETE_BTN_HOVER_BG = "#e74c3c";

    /** Builds the small red "X" delete button overlaid on a used slot's thumbnail.  The
     *  button consumes its own mouse events so a click on it doesn't bubble up to the
     *  tile / row's save/load click handler; on action it routes through
     *  {@link #triggerDelete} which prompts for confirmation before deleting the slot. */
    private static Button buildDeleteButton(RouteContext context, SaveSlotCategory category,
                                              int slot, Runnable afterChange) {
        Button btn = new Button("✕");
        // Explicit non-focusable so JavaFX never paints its default green focus ring
        // around the button.  Combined with the inline focus/border overrides below, the
        // delete pip stays a clean red disc regardless of keyboard-focus state.
        btn.setFocusTraversable(false);
        // Button is 20×20 (was 18×18) so the ✕ glyph — which carries slight right-side
        // optical weight in most sans-serif renderings — sits visually centred without
        // needing asymmetric padding.  The extra two pixels of width are absorbed by the
        // -fx-background-radius: 10 → perfect circle at the new size.
        String restStyle = deleteButtonStyle(DELETE_BTN_REST_BG);
        String hoverStyle = deleteButtonStyle(DELETE_BTN_HOVER_BG);
        btn.setStyle(restStyle);
        btn.setOnMouseEntered(event -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(event -> btn.setStyle(restStyle));
        Tooltip.install(btn, new Tooltip(screenText("item.delete.tooltip")));
        // Consume MOUSE_CLICKED + the Action so the tile/row's onMouseClicked save/load
        // handler doesn't ALSO fire when the player aims at the delete pip.
        btn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                javafx.event.Event::consume);
        btn.setOnAction(event -> {
            event.consume();
            triggerDelete(context, category, slot, afterChange);
        });
        return btn;
    }

    /** Inline-style template for the delete pip — accepts a background colour so the
     *  resting and hover states share every other property (size, focus suppression,
     *  text rendering, cursor) and only swap the fill. */
    private static String deleteButtonStyle(String backgroundColor) {
        return "-fx-background-color: " + backgroundColor + ";"
                + " -fx-text-fill: white;"
                + " -fx-font-weight: bold;"
                + " -fx-font-size: 11px;"
                + " -fx-background-radius: 10;"
                + " -fx-border-color: transparent;"
                + " -fx-border-width: 0;"
                + " -fx-focus-color: transparent;"
                + " -fx-faint-focus-color: transparent;"
                + " -fx-background-insets: 0;"
                + " -fx-min-width: 20px;"
                + " -fx-min-height: 20px;"
                + " -fx-max-width: 20px;"
                + " -fx-max-height: 20px;"
                // 2px of RIGHT padding (vs 0 elsewhere) squeezes the inner content area
                // to 18×20 and centres the ✕ glyph 1 px to the left of the geometric
                // button centre.  The glyph carries slight right-side optical weight in
                // most sans-serif fonts, so this lands it visually centred without
                // changing the disc's drawn rect (background-radius / min/max sizes are
                // all still 20×20, so the red circle stays symmetric).
                + " -fx-padding: 0 2 0 0;"
                + " -fx-cursor: hand;";
    }

    /** Confirmation-gated slot delete via the in-scene {@link DialogMessages} overlay so
     *  the prompt surfaces correctly even when the host is running in fullscreen (stock
     *  Alert dialogs hide behind the fullscreen window).  Two-line copy mirrors the
     *  overwrite dialog so the two destructive actions read in family. */
    private static void triggerDelete(RouteContext context, SaveSlotCategory category, int slot,
                                       Runnable afterChange) {
        Scene activeScene = context.primaryStage() == null ? null : context.primaryStage().getScene();
        DialogMessages.confirm(activeScene, context.uiTheme(),
                screenText("dialog.delete.title"),
                String.format(screenText("dialog.delete.header"), slot),
                screenText("dialog.delete.content"),
                result -> {
                    if (result != DialogMessages.Result.OK) {
                        return;
                    }
                    try {
                        context.saveLoadService().deleteSlot(category, slot);
                    } catch (RuntimeException ex) {
                        DialogMessages.error(activeScene, context.uiTheme(),
                                screenText("dialog.delete-error.title"),
                                String.format(screenText("dialog.delete-error.header"), slot),
                                ex.getMessage(),
                                afterChange);
                        System.err.println("[SaveScreen] Failed to delete slot "
                                + category + " " + slot + ": " + ex);
                        return;
                    }
                    afterChange.run();
                });
    }

    /** Navigates to the route captured in the slot summary — minimum-viable "load" until a
     *  host wires deeper game-state hydration through SaveLoadService.
     *
     *  <p>Path priority:</p>
     *  <ol>
     *    <li>If the summary recorded a specific non-menu route (saves written with the
     *        caller-route baton — i.e. from a gameplay screen), open that route fresh.</li>
     *    <li>Otherwise — older saves predate the baton fix and have the immutable boot
     *        {@code GameState.startupRoute()} (typically the main menu) persisted, OR the
     *        recorded route IS the main menu — fall back to {@code navigateBack} to pop the
     *        scene the player came from before opening this save screen.  When the player
     *        opened save / load from a gameplay scene that scene was pushed onto the back
     *        stack by {@code pushAndNavigateTo}, so popping it restores the gameplay scene
     *        the player came from — which is the desired "load → take me into the game"
     *        outcome even without a valid recorded route.</li>
     *    <li>Last resort — no back stack entry, no usable route — navigate to the recorded
     *        route as-is so the player isn't stranded on the save screen.</li>
     *  </ol>
     */
    static void triggerLoad(RouteContext context, SaveSlotSummary summary) {
        // Restore the gameplay state BEFORE navigating so the destination scene
        // constructs against the loaded NPCs / map / time / etc., not the boot-default
        // state.  restoreSlotSnapshot returns false for legacy / metadata-only saves
        // (no snapshot file on disk) — in that case we just navigate without restore,
        // matching pre-snapshot behaviour so existing saves still "work" (they just
        // don't carry game state).
        try {
            boolean restored = context.saveLoadService().restoreSlotSnapshot(
                    summary.category(), summary.slot(), context.gameState());
            if (!restored) {
                System.err.println("[SaveScreen] Slot " + summary.category() + " "
                        + summary.slot() + " has no snapshot file — loading navigates"
                        + " to recorded route but gameplay state stays at boot defaults.");
            }
        } catch (RuntimeException ex) {
            // Snapshot decode / restore failure — log and proceed with navigation so
            // the player isn't stranded on the save screen, but warn them in the log
            // so a corrupt save is debuggable.
            System.err.println("[SaveScreen] Failed to restore snapshot for slot "
                    + summary.category() + " " + summary.slot() + ": " + ex);
        }
        String startupRoute = summary.startupRoute();
        boolean hasGameplayRoute = startupRoute != null
                && !startupRoute.isBlank()
                && !SceneRouter.MAIN_MENU_ROUTE.equals(startupRoute);
        if (hasGameplayRoute) {
            context.navigateTo(startupRoute);
            return;
        }
        // Fallback: restore the calling scene from the back stack — the gameplay scene
        // the player was on when they clicked Save / Load in the footer.
        if (context.navigateBack()) {
            return;
        }
        // No back-stack entry either — best we can do is navigate to whatever the summary
        // had (main menu) so the screen at least moves on rather than getting stuck.
        if (startupRoute != null && !startupRoute.isBlank()) {
            context.navigateTo(startupRoute);
        }
    }

    private static void triggerSave(RouteContext context, SaveSlotCategory category, int slot,
                                     SaveSlotSummary existing, Runnable afterSave,
                                     WritableImage callerSnapshot, String callerRoute) {
        Scene activeScene = context.primaryStage() == null ? null : context.primaryStage().getScene();
        if (existing != null) {
            DialogMessages.confirm(activeScene, context.uiTheme(),
                    screenText("dialog.overwrite.title"),
                    String.format(screenText("dialog.overwrite.header"), slot),
                    screenText("dialog.overwrite.content"),
                    result -> {
                        if (result == DialogMessages.Result.OK) {
                            promptForNameThenWrite(context, category, slot, existing, afterSave,
                                    callerSnapshot, callerRoute, activeScene);
                        }
                    });
            return;
        }
        promptForNameThenWrite(context, category, slot, /*existing*/ null, afterSave,
                callerSnapshot, callerRoute, activeScene);
    }

    /** Shows the "name your save" prompt, then writes the slot using the entered name
     *  as the save description.  Cancelling the prompt aborts the save with no side
     *  effects on disk — same as cancelling the overwrite confirm.  The default text
     *  prefers the existing description when overwriting (so a quick Enter renames the
     *  save with the same label it already had); for an empty slot we synthesise
     *  "{Category} {Slot} — {timestamp}" as a sensible starting point. */
    private static void promptForNameThenWrite(RouteContext context, SaveSlotCategory category,
                                                int slot, SaveSlotSummary existing,
                                                Runnable afterSave, WritableImage callerSnapshot,
                                                String callerRoute, Scene activeScene) {
        String defaultName = defaultSaveName(category, slot, existing);
        DialogMessages.prompt(activeScene, context.uiTheme(),
                screenText("dialog.name.title"),
                String.format(screenText("dialog.name.header"), slot),
                screenText("dialog.name.content"),
                defaultName,
                entered -> {
                    if (entered == null) {
                        // Cancel / Escape / backdrop click — abort the save entirely.
                        return;
                    }
                    String description = entered.isBlank() ? defaultName : entered.trim();
                    doWriteSave(context, category, slot, description, afterSave,
                            callerSnapshot, callerRoute, activeScene);
                });
    }

    /** Builds the default save-name prepopulated in the prompt.  Priority order:
     *  <ol>
     *    <li>Existing slot description (overwriting a named save reuses its name so the
     *        player can re-save with a quick Enter without losing the label).</li>
     *    <li>Host-supplied default via {@link #setDefaultSaveNameSupplier} (game-context
     *        aware — typically the MC's current map location or scene name).</li>
     *    <li>Engine fallback "{tab label} {slot} — {yyyy-MM-dd HH:mm}" — used when no
     *        host supplier is registered or it returned null/blank.</li>
     *  </ol> */
    private static String defaultSaveName(SaveSlotCategory category, int slot,
                                            SaveSlotSummary existing) {
        if (existing != null && !existing.description().isBlank()) {
            return existing.description();
        }
        java.util.function.Supplier<String> supplier = defaultSaveNameSupplier;
        if (supplier != null) {
            try {
                String supplied = supplier.get();
                if (supplied != null && !supplied.isBlank()) {
                    return supplied;
                }
            } catch (RuntimeException ex) {
                // Host supplier threw — don't blow up the prompt; fall through to the
                // engine default and log so the host can find the bug.
                System.err.println("[SaveScreen] Default-save-name supplier threw: " + ex);
            }
        }
        return emptyTileLabel(category, slot) + " — " + formatSavedAt(Instant.now());
    }

    /** The actual save-write step extracted so {@link #triggerSave}'s overwrite-confirm
     *  branch can call it from a callback after the DialogMessages overlay resolves.
     *  {@code description} is the human-readable save name the player entered via the
     *  name-prompt dialog (previously this slot was incorrectly being filled with
     *  {@code gameState.startupRoute()} — a route id string, not a save name). */
    private static void doWriteSave(RouteContext context, SaveSlotCategory category, int slot,
                                      String description,
                                      Runnable afterSave, WritableImage callerSnapshot,
                                      String callerRoute, Scene activeScene) {
        try {
            // Prefer the caller-supplied snapshot (the gameplay scene the player came from);
            // skip the thumbnail entirely if no snapshot was prepared.  Falling back to
            // context.primaryStage().getScene() here would write the SAVE SCREEN itself as
            // the preview, which is the exact bug the prepare-snapshot baton avoids.
            //
            // callerRoute (also captured by the host before navigation) is recorded as the
            // saved summary's startupRoute — see the SaveLoadService overload that takes
            // a targetRoute parameter.  Without this, loading the save would navigate to
            // the boot startup route on GameState (typically main menu) instead of back to
            // the gameplay scene the player saved from.
            context.saveLoadService().writeSlotSummary(
                    category,
                    slot,
                    context.gameState(),
                    description,
                    Instant.now(),
                    callerSnapshot,
                    callerRoute,
                    // Pull the in-game date label from the host-registered supplier so
                    // it persists with the slot.  Empty string when no host has wired
                    // a supplier — the list view's Game-date column shows blank for
                    // that case without breaking the save write.
                    currentGameDateString());
            // Full gameplay-state snapshot — writes <prefix>NNN.snapshot.json alongside
            // the metadata .properties.  Captures every part of GameState plus every
            // host-registered custom rollback section, so loading this slot later
            // restores the full game state, not just navigation metadata.  Skipped when
            // the host hasn't registered a GameDateTime supplier (e.g. tests / menu-only
            // saves where there's no live game time) — the save then writes metadata
            // only, and load falls back to navigate-without-restore.
            com.eb.javafx.gamesupport.GameDateTime snapshotTime = currentGameDateTime();
            if (snapshotTime != null) {
                context.saveLoadService().captureSlotSnapshot(
                        category, slot, context.gameState(), snapshotTime);
            }
        } catch (RuntimeException ex) {
            // Error surface is async (callback-resolved) too, so we have to defer the
            // afterSave refresh until the player dismisses the dialog — otherwise the
            // slot list could rebuild underneath the still-open overlay and the player
            // would see stale chrome briefly.  No auto-return on error: the player stays
            // on the save screen so they can try a different slot or back out manually.
            DialogMessages.error(activeScene, context.uiTheme(),
                    screenText("dialog.save-error.title"),
                    String.format(screenText("dialog.save-error.header"), slot),
                    ex.getMessage(),
                    afterSave);
            System.err.println("[SaveScreen] Failed to save slot " + category + " " + slot + ": " + ex);
            return;
        }
        // Successful save: return to the calling screen automatically — mirrors the
        // load flow's "do the work, then take the player back to gameplay" UX.  The
        // refresh-tab afterSave step is intentionally skipped here because we're about
        // to leave the save screen entirely; refreshing the grid right before
        // navigating away would be wasted layout work.
        returnToCaller(context, callerRoute);
    }

    /** Pops the back-stack to restore the calling screen after a successful save / load.
     *  Same fallback chain as {@link #triggerLoad} so saves opened from the main menu
     *  (no real gameplay route to return to) still find their way somewhere sensible
     *  instead of stranding the player on the save screen. */
    private static void returnToCaller(RouteContext context, String callerRoute) {
        boolean hasGameplayRoute = callerRoute != null
                && !callerRoute.isBlank()
                && !SceneRouter.MAIN_MENU_ROUTE.equals(callerRoute);
        if (hasGameplayRoute) {
            context.navigateTo(callerRoute);
            return;
        }
        if (context.navigateBack()) {
            return;
        }
        if (callerRoute != null && !callerRoute.isBlank()) {
            context.navigateTo(callerRoute);
        }
    }

    // ---- Helpers -----------------------------------------------------------------------

    private static Map<Integer, SaveSlotSummary> indexSummariesBySlot(SaveLoadService service,
                                                                         SaveSlotCategory category) {
        Map<Integer, SaveSlotSummary> indexed = new HashMap<>();
        List<SaveSlotSummary> summaries;
        try {
            summaries = service.listSlotSummaries(category);
        } catch (RuntimeException ex) {
            System.err.println("[SaveScreen] Unable to list save slots for " + category + ": " + ex);
            return indexed;
        }
        for (SaveSlotSummary s : summaries) {
            indexed.put(s.slot(), s);
        }
        return indexed;
    }

    private static ImageView thumbnailViewFor(SaveSlotSummary summary, int width, int height) {
        ImageView view = new ImageView();
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        if (summary != null && summary.thumbnail() != null) {
            try {
                Path path = summary.thumbnail();
                Image image = new Image(path.toUri().toString());
                view.setImage(image);
            } catch (RuntimeException ex) {
                // Fall through to empty placeholder; logging would spam if missing thumbnails
                // become common (e.g. older saves without screenshots).
            }
        }
        if (view.getImage() == null) {
            // Placeholder background so empty slots still occupy the cell footprint and the
            // grid layout doesn't shift between empty / filled.
            // Corner radius lives in CSS (.save-thumb-placeholder).
            view.getStyleClass().add("save-thumb-placeholder");
            view.setStyle("-fx-background-color: rgba(255,255,255,0.06);");
        }
        return view;
    }

    /** Builds one round-bordered page chip ("1", "2", …).  Selected chip draws a
     *  rounded accent border + filled accent background + bold text; un-selected
     *  chips are flat-text with a transparent border, and hover lifts them to a
     *  faint accent fill so the cursor target is obvious.  Click sets the bound
     *  {@code selectedPage} property which the screen's listener uses to swap
     *  which slot range the grid renders. */
    private static Label buildPageChip(int pageNum,
                                         javafx.beans.property.IntegerProperty selectedPage,
                                         String accent, String textHex) {
        Label chip = new Label(String.valueOf(pageNum));
        chip.getStyleClass().add("save-page-chip");
        chip.setMinWidth(28);
        chip.setAlignment(Pos.CENTER);
        boolean selected = selectedPage.get() == pageNum;
        // Corner radius lives in CSS (.save-page-chip); the per-state inline styles carry only the
        // state-dependent colours / weight.
        String selectedStyle =
                "-fx-text-fill: white;"
              + " -fx-font-size: 13px;"
              + " -fx-font-weight: bold;"
              + " -fx-background-color: " + accent + ";"
              + " -fx-border-color: white;"
              + " -fx-border-width: 1.5;"
              + " -fx-padding: 4 12 4 12;"
              + " -fx-cursor: hand;";
        String restStyle =
                "-fx-text-fill: " + textHex + ";"
              + " -fx-font-size: 13px;"
              + " -fx-font-weight: normal;"
              + " -fx-background-color: transparent;"
              + " -fx-border-color: transparent;"
              + " -fx-border-width: 1.5;"
              + " -fx-padding: 4 12 4 12;"
              + " -fx-cursor: hand;";
        String hoverStyle =
                "-fx-text-fill: white;"
              + " -fx-font-size: 13px;"
              + " -fx-font-weight: normal;"
              + " -fx-background-color: rgba(255,255,255,0.12);"
              + " -fx-border-color: " + accent + ";"
              + " -fx-border-width: 1.5;"
              + " -fx-padding: 4 12 4 12;"
              + " -fx-cursor: hand;";
        chip.setStyle(selected ? selectedStyle : restStyle);
        if (!selected) {
            chip.setOnMouseEntered(event -> chip.setStyle(hoverStyle));
            chip.setOnMouseExited(event -> chip.setStyle(restStyle));
        }
        chip.setOnMouseClicked(event -> selectedPage.set(pageNum));
        return chip;
    }

    /** Builds the "+" chip that grows {@code pageCount} by one (clamped to
     *  {@link #MAX_PAGES}) and jumps {@code selectedPage} to the newly-added page
     *  so the player lands directly on the empty slot grid.  Styled to match the
     *  number chips so the strip reads as one continuous control. */
    private static Label buildAddPageChip(javafx.beans.property.IntegerProperty pageCount,
                                            javafx.beans.property.IntegerProperty selectedPage,
                                            String accent, String textHex) {
        Label chip = new Label("+");
        chip.getStyleClass().add("save-page-chip");
        chip.setMinWidth(28);
        chip.setAlignment(Pos.CENTER);
        // Corner radius lives in CSS (.save-page-chip); inline styles carry state colours only.
        String restStyle =
                "-fx-text-fill: " + textHex + ";"
              + " -fx-font-size: 16px;"
              + " -fx-font-weight: bold;"
              + " -fx-background-color: transparent;"
              + " -fx-border-color: " + accent + ";"
              + " -fx-border-width: 1.5;"
              + " -fx-padding: 2 12 2 12;"
              + " -fx-cursor: hand;";
        String hoverStyle =
                "-fx-text-fill: white;"
              + " -fx-font-size: 16px;"
              + " -fx-font-weight: bold;"
              + " -fx-background-color: " + accent + ";"
              + " -fx-border-color: white;"
              + " -fx-border-width: 1.5;"
              + " -fx-padding: 2 12 2 12;"
              + " -fx-cursor: hand;";
        chip.setStyle(restStyle);
        chip.setOnMouseEntered(event -> chip.setStyle(hoverStyle));
        chip.setOnMouseExited(event -> chip.setStyle(restStyle));
        chip.setOnMouseClicked(event -> {
            int next = Math.min(MAX_PAGES, pageCount.get() + 1);
            if (next != pageCount.get()) {
                pageCount.set(next);
                selectedPage.set(next);
            }
        });
        return chip;
    }

    private static String formatSavedAt(Instant instant) {
        if (instant == null) {
            return "";
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(SAVED_AT_FORMAT);
    }

    /** Returns the on-disk size of a save slot — sum of the save-data file ({@link
     *  SaveLoadService#slotPath} → the .properties payload) plus the thumbnail file
     *  when one was captured.  Formatted as a single short string ("12 KB", "1.4 MB")
     *  so it fits the narrow size column without truncation.  Returns an empty
     *  string on I/O failure rather than throwing — the list view should still
     *  render the row even if a slot's file is missing or unreadable. */
    private static String formatSlotSize(SaveLoadService service, SaveSlotCategory category,
                                          int slot, SaveSlotSummary summary) {
        try {
            long total = 0L;
            java.nio.file.Path dataPath = service.slotPath(category, slot);
            if (dataPath != null && java.nio.file.Files.exists(dataPath)) {
                total += java.nio.file.Files.size(dataPath);
            }
            java.nio.file.Path thumbPath = summary == null ? null : summary.thumbnail();
            if (thumbPath != null && java.nio.file.Files.exists(thumbPath)) {
                total += java.nio.file.Files.size(thumbPath);
            }
            // Include the snapshot file size — for saves with full game state this is
            // typically the largest component (hundreds of KB to a few MB), so the
            // column otherwise misrepresents how much disk the slot actually uses.
            java.nio.file.Path snapshotPath = service.slotSnapshotPath(category, slot);
            if (snapshotPath != null && java.nio.file.Files.exists(snapshotPath)) {
                total += java.nio.file.Files.size(snapshotPath);
            }
            return humanReadableBytes(total);
        } catch (java.io.IOException ex) {
            // Don't spam the log — a missing/unreadable file just renders as blank.
            return "";
        }
    }

    /** Compact byte-size formatter — picks B / KB / MB / GB so the value fits in the
     *  narrow Size column.  Uses 1024-based units (matching how disk tools on Windows /
     *  Linux render save-file sizes) with one decimal place for KB+.  Saves are
     *  generally KB-scale, so the typical reading will be "12.4 KB" / "186 KB". */
    private static String humanReadableBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(java.util.Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(java.util.Locale.ROOT, "%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format(java.util.Locale.ROOT, "%.1f GB", gb);
    }

    private static String screenText(String key) {
        return ScreenTextResources.text(ScreenTextResources.SAVE, key);
    }

    private static List<ScreenShell.FooterOption> footerOptions() {
        return ScreenShell.defaultFooterOptions().stream()
                .map(option -> option.withEnabled(ENABLED_FOOTER_IDS.contains(option.id())))
                .toList();
    }

    private static void wireFooter(HBox footer, Runnable closeAction) {
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label
                    && label.getUserData() instanceof ScreenShell.FooterOption option
                    && PREFERENCES_ID.equals(option.id())) {
                label.setOnMouseClicked(event -> {
                    if (!ScreenShell.isFooterOptionEnabled(label)) {
                        return;
                    }
                    closeAction.run();
                });
            }
        }
    }

    private static Scene themedSaveScene(RouteContext context, BorderPane root, double width, double height) {
        Parent sceneRoot = ScreenShell.withConfiguredBackground(
                root,
                context.applicationRoot(),
                context.resourceConfig().defaultPreferencesScreenBackgroundColor(),
                context.resourceConfig().defaultPreferencesScreenBackgroundImage(),
                context.resourceConfig().defaultPreferencesScreenBackgroundImageTransparency());
        return new Scene(sceneRoot, width, height);
    }
}
