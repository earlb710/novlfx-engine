package com.eb.javafx.content;

import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.gamesupport.SystemCodeTables;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.ui.ScreenTextResources;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registers reusable placeholder content for the engine shell and manual demos. */
public final class EnginePlaceholderContentModule implements StaticContentModule {
    @Override
    public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        Map<String, String> definitions = new LinkedHashMap<>();
        definitions.put("application.name", "novlfx-engine");
        definitions.put("startup.route", SceneRouter.MAIN_MENU_ROUTE);
        definitions.put("ui.mainMenu.title", ScreenTextResources.title(ScreenTextResources.MAIN_MENU));
        definitions.put("ui.mainMenu.status", ScreenTextResources.text(ScreenTextResources.MAIN_MENU, "line.status"));
        definitions.put("ui.preferences.title", ScreenTextResources.title(ScreenTextResources.PREFERENCES));
        definitions.put("ui.saveLoad.title", ScreenTextResources.title(ScreenTextResources.SAVE_LOAD));
        definitions.put("ui.dialogue.title", ScreenTextResources.title(ScreenTextResources.DIALOGUE));
        definitions.put("ui.choice.title", ScreenTextResources.title(ScreenTextResources.CHOICE));
        definitions.put("ui.hud.title", ScreenTextResources.title(ScreenTextResources.HUD));
        definitions.put("ui.notification.title", ScreenTextResources.title(ScreenTextResources.NOTIFICATION));
        definitions.put("ui.tooltip.title", ScreenTextResources.title(ScreenTextResources.TOOLTIP));
        definitions.put("ui.conversationHistory.title", ScreenTextResources.title(ScreenTextResources.CONVERSATION_HISTORY));
        definitions.put("ui.displayBindings.title", ScreenTextResources.title(ScreenTextResources.DISPLAY_BINDINGS));
        definitions.put("ui.captureTest.title", "Capture Test Screen");
        definitions.put("ui.complexFooterBarTest.title", "Complex Footer Bar Test");
        definitions.put("ui.preferencesFooterTest.title", "Preferences Footer Test");
        definitions.put("ui.footer.back.label", SystemCodeTables.defaultMessage("footer.back.label"));
        definitions.put("ui.footer.back.tooltip", SystemCodeTables.defaultMessage("footer.back.tooltip"));
        definitions.put("ui.footer.history.label", SystemCodeTables.defaultMessage("footer.history.label"));
        definitions.put("ui.footer.history.tooltip", SystemCodeTables.defaultMessage("footer.history.tooltip"));
        definitions.put("ui.footer.skip-mode.label", SystemCodeTables.defaultMessage("footer.skip-mode.label"));
        definitions.put("ui.footer.skip-mode.tooltip", SystemCodeTables.defaultMessage("footer.skip-mode.tooltip"));
        definitions.put("ui.footer.load.label", SystemCodeTables.defaultMessage("footer.load.label"));
        definitions.put("ui.footer.load.tooltip", SystemCodeTables.defaultMessage("footer.load.tooltip"));
        definitions.put("ui.footer.save.label", SystemCodeTables.defaultMessage("footer.save.label"));
        definitions.put("ui.footer.save.tooltip", SystemCodeTables.defaultMessage("footer.save.tooltip"));
        definitions.put("ui.footer.quick-save.label", SystemCodeTables.defaultMessage("footer.quick-save.label"));
        definitions.put("ui.footer.quick-save.tooltip", SystemCodeTables.defaultMessage("footer.quick-save.tooltip"));
        definitions.put("ui.footer.preferences.label", SystemCodeTables.defaultMessage("footer.preferences.label"));
        definitions.put("ui.footer.preferences.tooltip", SystemCodeTables.defaultMessage("footer.preferences.tooltip"));
        definitions.put("ui.footer.forward.label", SystemCodeTables.defaultMessage("footer.forward.label"));
        definitions.put("ui.footer.forward.tooltip", SystemCodeTables.defaultMessage("footer.forward.tooltip"));
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
