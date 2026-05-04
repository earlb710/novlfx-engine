import com.eb.javafx.accessibility.AccessibilityProfile;
import com.eb.javafx.assets.AssetCatalog;
import com.eb.javafx.assets.AssetDefinition;
import com.eb.javafx.assets.AssetType;
import com.eb.javafx.assets.AssetValidationReport;
import com.eb.javafx.diagnostics.DiagnosticProblem;
import com.eb.javafx.diagnostics.DiagnosticRegistry;
import com.eb.javafx.diagnostics.DiagnosticSeverity;
import com.eb.javafx.events.GameEvent;
import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.input.InputAction;
import com.eb.javafx.input.InputBinding;
import com.eb.javafx.input.InputDevice;
import com.eb.javafx.input.InputMap;
import com.eb.javafx.input.InputTrigger;
import com.eb.javafx.localization.LocalizedTextBundle;
import com.eb.javafx.localization.LocalizationService;
import com.eb.javafx.progress.ProgressSnapshotCodec;
import com.eb.javafx.progress.ProgressSupport;
import com.eb.javafx.progress.ProgressTracker;
import com.eb.javafx.save.SaveSnapshotSection;
import com.eb.javafx.settings.SettingDefinition;
import com.eb.javafx.settings.SettingType;
import com.eb.javafx.settings.SettingsStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Demonstrates content-neutral support modules for localization, assets, input, events, progress, and settings.
 *
 * <p>Expected output prints localized text lookup, asset diagnostics, resolved input actions, event history,
 * progress save metadata, and player-facing setting/accessibility values.</p>
 */
public final class GenericSupportModulesDemo {
    private GenericSupportModulesDemo() {
    }

    public static void main(String[] args) throws Exception {
        LocalizationService localization = new LocalizationService();
        localization.registerBundle(new LocalizedTextBundle("en", Map.of(
                "menu.start", "Start",
                "menu.settings", "Settings")));
        localization.registerBundle(new LocalizedTextBundle("fr", Map.of(
                "menu.start", "Démarrer")));
        localization.selectLanguage("fr");
        System.out.println("Localized start label: " + localization.textOrId("menu.start"));
        System.out.println("Missing text ids: " + localization.missingTextIds(List.of(
                "menu.start",
                "menu.settings")));

        Path assetRoot = Files.createTempDirectory("novlfx-assets-demo");
        Files.createDirectories(assetRoot.resolve("images"));
        Files.writeString(assetRoot.resolve("images/main-menu.png"), "demo image bytes");
        AssetCatalog assetCatalog = new AssetCatalog();
        assetCatalog.register(new AssetDefinition(
                "main-menu-bg",
                AssetType.IMAGE,
                "images/main-menu.png",
                true,
                List.of("menu")));
        assetCatalog.register(new AssetDefinition(
                "theme-song",
                AssetType.AUDIO,
                "audio/theme.ogg",
                true,
                List.of("music")));
        AssetValidationReport assetReport = assetCatalog.validateExisting(assetRoot);
        System.out.println("Preload asset ids: " + assetCatalog.preloadAssets().stream()
                .map(AssetDefinition::id)
                .toList());
        System.out.println("Asset diagnostic count: " + assetReport.problems().size());

        InputMap inputMap = new InputMap();
        InputTrigger enterKey = new InputTrigger(InputDevice.KEYBOARD, "ENTER", Set.of());
        inputMap.registerAction(new InputAction("menu.confirm", "Confirm", "menu", true, List.of("primary")));
        inputMap.registerAction(new InputAction("dialogue.advance", "Advance dialogue", "dialogue", true, List.of("primary")));
        inputMap.bind(new InputBinding("menu.confirm", enterKey));
        inputMap.bind(new InputBinding("dialogue.advance", enterKey));
        System.out.println("Menu action for Enter: " + inputMap.actionForTrigger("menu", enterKey).orElseThrow().id());
        System.out.println("Dialogue action for Enter: " + inputMap.actionForTrigger("dialogue", enterKey).orElseThrow().id());

        GameEventBus eventBus = new GameEventBus();
        eventBus.subscribe("route.changed", event -> System.out.println("Route event payload: " + event.payload()));
        eventBus.publish(new GameEvent(
                "route.changed",
                "router",
                Map.of("route", "main-menu"),
                Instant.EPOCH));
        System.out.println("Route event history size: " + eventBus.history("route.changed").size());

        ProgressTracker progress = new ProgressTracker();
        ProgressSupport.setFlag(progress, "intro-complete").apply(null);
        progress.incrementCounter("visits.main-menu", 1);
        progress.completeMilestone("first-launch");
        progress.unlock("gallery.main-menu");
        SaveSnapshotSection progressSection = new ProgressSnapshotCodec().toSection(progress.snapshot());
        System.out.println("Progress section: " + progressSection.sectionId() + " v" + progressSection.schemaVersion());
        System.out.println("Intro requirement allowed: "
                + ProgressSupport.requireFlag(progress, "intro-complete").evaluate(null).isAllowed());

        SettingsStore settings = new SettingsStore();
        settings.register(new SettingDefinition("textSpeed", "Text speed", SettingType.INTEGER, "5"));
        settings.register(new SettingDefinition("language", "Language", SettingType.TEXT, localization.selectedLanguageId()));
        settings.set("textSpeed", "8");
        AccessibilityProfile accessibility = new AccessibilityProfile(1.25, true, false, true, true);
        System.out.println("Text speed setting: " + settings.value("textSpeed"));
        System.out.println("Language setting default: " + settings.value("language"));
        System.out.println("Accessibility font scale: " + accessibility.fontScale());

        DiagnosticRegistry diagnostics = new DiagnosticRegistry();
        diagnostics.register(() -> assetReport.problems().stream()
                .map(problem -> new DiagnosticProblem(
                        "assets",
                        DiagnosticSeverity.ERROR,
                        problem.message(),
                        problem.assetId()))
                .toList());
        System.out.println("Diagnostics have errors: " + diagnostics.runChecks().hasErrors());
    }
}
