import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.ActionResult;
import com.eb.javafx.gamesupport.GameAction;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.gamesupport.RequirementResult;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.state.GameState;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public final class SupportServicesDemo {
    private SupportServicesDemo() {
    }

    public static void main(String[] args) {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        preferencesService.saveWindowSize(1440, 900);

        GameRandomService randomService = new GameRandomService();
        randomService.initialize();

        GameSupportService gameSupportService = new GameSupportService();
        gameSupportService.initialize();

        GameAction restAction = new GameAction(
                "rest",
                "Rest",
                "daily-life",
                List.of(context -> RequirementResult.allowed()),
                List.of(context -> ActionResult.success("Recovered energy.")));
        gameSupportService.actionRegistry().register(restAction);

        GameState gameState = new GameState("main-menu");
        ActionContext actionContext = new ActionContext(gameState, randomService, gameSupportService.gameClock());
        System.out.println(restAction.execute(actionContext).message());

        SaveLoadService saveLoadService = new SaveLoadService(Path.of("/tmp/novlfx-engine-demo-saves"));
        saveLoadService.initialize();
        saveLoadService.writeSlotSummary(1, gameState, "Before opening scene", Instant.now());

        System.out.println("Gameplay seed: " + randomService.gameplaySeed());
        System.out.println("Saved slots: " + saveLoadService.listSlotSummaries().size());
    }
}
