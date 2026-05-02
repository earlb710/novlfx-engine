import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.ActionResult;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameAction;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.gamesupport.RequirementResult;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.state.GameState;
import com.eb.javafx.state.GameStateFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Demonstrates reusable support services for preferences, saves, random values, game state, actions, and clocks.
 *
 * <p>Expected output prints saved state, preference, random roll, action, save slot, and time advancement values.</p>
 */
public final class SupportServicesDemo {
    private SupportServicesDemo() {
    }

    public static void main(String[] args) {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        preferencesService.saveWindowSize(1440, 900);
        preferencesService.saveUiVisibility(true, false, true);
        preferencesService.saveUiOpacity(0.9, 0.8);
        preferencesService.saveFontPreferences("Serif", 1.1);
        preferencesService.saveAccessibilityPreferences(true, false);
        preferencesService.saveInputMode("keyboard");
        preferencesService.saveMasterVolume(0.65);

        GameRandomService randomService = new GameRandomService();
        randomService.initialize();

        GameSupportService gameSupportService = new GameSupportService();
        gameSupportService.initialize();

        CodeTableDefinition timeSlots = new CodeTableDefinition(
                "time-slots",
                "Time Slots",
                List.of(
                        new CodeDefinition("morning", "Morning", 10, List.of("day")),
                        new CodeDefinition("evening", "Evening", 20, List.of("day"))));
        GameClock gameClock = new GameClock(timeSlots);
        gameClock.advanceSlot();

        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();
        contentRegistry.registerDefinition("application.name", "demo-app");
        contentRegistry.registerDefinition("startup.route", "main-menu");
        GameState gameState = new GameStateFactory().createNewGame(contentRegistry);

        GameAction restAction = new GameAction(
                "rest",
                "Rest",
                "daily-life",
                List.of(context -> RequirementResult.allowed()),
                List.of(context -> ActionResult.success("Recovered energy.")));
        gameSupportService.actionRegistry().register(restAction);

        ActionContext actionContext = new ActionContext(gameState, randomService, gameClock);
        System.out.println(restAction.execute(actionContext).message());

        SaveLoadService saveLoadService = new SaveLoadService(Path.of("/tmp/novlfx-engine-demo-saves"));
        saveLoadService.initialize();
        saveLoadService.writeSlotSummary(1, gameState, "Before opening scene", Instant.now());
        SaveLoadService.SaveSlotSummary slotSummary = saveLoadService.readSlotSummary(1);

        System.out.println("Gameplay seed: " + randomService.gameplaySeed());
        System.out.println("Next gameplay roll: " + randomService.nextGameplayInt(10));
        System.out.println("Next UI roll: " + randomService.nextUiInt(10));
        System.out.println("Current time slot: " + gameClock.currentTime().timeSlotId());
        System.out.println("Startup route from factory: " + gameState.startupRoute());
        System.out.println("Preferences input mode: " + preferencesService.inputMode());
        System.out.println("Saved slot route: " + slotSummary.startupRoute());
        System.out.println("Saved slots: " + saveLoadService.listSlotSummaries().size());
    }
}
