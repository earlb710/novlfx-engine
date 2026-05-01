import com.eb.javafx.audio.AudioService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.text.TextTagParser;

public final class ModuleUsageExample {
    private ModuleUsageExample() {
    }

    public static void main(String[] args) {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        AudioService audioService = new AudioService();
        audioService.initialize(preferencesService);

        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();
        contentRegistry.registerDefinition("application.name", "demo-app");
        contentRegistry.registerDefinition("startup.route", SceneRouter.MAIN_MENU_ROUTE);
        contentRegistry.validateRules();

        TextTagParser parser = new TextTagParser();
        System.out.println("Loaded channels: " + audioService.channels().keySet());
        System.out.println("Parsed tokens: " + parser.parse("{b}Hello{/b} from novlfx-engine").size());
    }
}
