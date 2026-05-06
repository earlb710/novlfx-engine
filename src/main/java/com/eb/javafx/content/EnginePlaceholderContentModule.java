package com.eb.javafx.content;

import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.routing.SceneRouter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registers reusable placeholder content for the engine shell and manual demos. */
public final class EnginePlaceholderContentModule implements StaticContentModule {
    @Override
    public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        Map<String, String> definitions = new LinkedHashMap<>();
        definitions.put("application.name", "novlfx-engine");
        definitions.put("startup.route", SceneRouter.MAIN_MENU_ROUTE);
        definitions.put("ui.mainMenu.title", "Main Menu");
        definitions.put("ui.mainMenu.status", "Reusable JavaFX engine shell is ready.");
        definitions.put("ui.preferences.title", "Preferences");
        definitions.put("ui.saveLoad.title", "Save / Load");
        definitions.put("ui.dialogue.title", "Dialogue");
        definitions.put("ui.choice.title", "Choice Menu");
        definitions.put("ui.hud.title", "HUD");
        definitions.put("ui.notification.title", "Notification Overlay");
        definitions.put("ui.tooltip.title", "Tooltip");
        definitions.put("ui.conversationHistory.title", "Conversation History");
        definitions.put("ui.displayBindings.title", "Display Bindings Preview");
        definitions.put("ui.captureTest.title", "Capture Test Screen");
        definitions.put("ui.complexFooterBarTest.title", "Complex Footer Bar Test");
        definitions.put("ui.footer.back.label", "Back");
        definitions.put("ui.footer.back.tooltip", "Return to the previous screen.");
        definitions.put("ui.footer.history.label", "History");
        definitions.put("ui.footer.history.tooltip", "Open conversation history.");
        definitions.put("ui.footer.skip-mode.label", "Skip mode");
        definitions.put("ui.footer.skip-mode.tooltip", "Toggle skip mode.");
        definitions.put("ui.footer.load.label", "Load");
        definitions.put("ui.footer.load.tooltip", "Open the load screen.");
        definitions.put("ui.footer.save.label", "Save");
        definitions.put("ui.footer.save.tooltip", "Open the save screen.");
        definitions.put("ui.footer.quick-save.label", "Quick save");
        definitions.put("ui.footer.quick-save.tooltip", "Save to the quick-save slot.");
        definitions.put("ui.footer.preferences.label", "Preferences");
        definitions.put("ui.footer.preferences.tooltip", "Open preferences.");
        definitions.put("ui.footer.forward.label", "Forward");
        definitions.put("ui.footer.forward.tooltip", "Advance the scene.");
        definitions.put("scene.demo.dialogue.line", "Reusable scene-flow demo line without authored game content.");
        definitions.put("scene.demo.choice.continue", "Complete demo scene");
        definitions.put("scene.demo.choice.return", "Return to demo dialogue");

        contentRegistry.registerRequiredDefinition("application.name");
        contentRegistry.registerRequiredDefinition("startup.route");
        contentRegistry.registerDefinitions(definitions);
    }

    @Override
    public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        // Placeholder content is validated by the registry and route validation phases.
    }
}
