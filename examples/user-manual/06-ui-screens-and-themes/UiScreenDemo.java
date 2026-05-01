import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.ui.CaptureTestScreen;
import com.eb.javafx.ui.UiTheme;
import javafx.application.Application;
import javafx.stage.Stage;

public final class UiScreenDemo extends Application {
    @Override
    public void start(Stage stage) {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme uiTheme = new UiTheme();
        uiTheme.initialize(preferencesService);

        stage.setTitle("novlfx-engine UI demo");
        stage.setScene(CaptureTestScreen.createScene(
                "Capture Demo",
                preferencesService,
                uiTheme,
                stage::close));
        stage.show();
    }
}
