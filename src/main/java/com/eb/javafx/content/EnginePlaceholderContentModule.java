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
        definitions.put("application.name", "NovlFX Engine");
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
        definitions.put("ui.displayBindings.title", "Display Bindings Preview");
        definitions.put("ui.captureTest.title", "Capture Test Screen");

        contentRegistry.registerRequiredDefinition("application.name");
        contentRegistry.registerRequiredDefinition("startup.route");
        contentRegistry.registerDefinitions(definitions);
    }

    @Override
    public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        // Placeholder content is validated by the registry and route validation phases.
    }
}
