package com.example.starter;

import com.eb.javafx.bootstrap.EngineModuleProvider;
import com.eb.javafx.bootstrap.ModuleContext;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.routing.SceneRouter;

/**
 * The game's single {@link EngineModuleProvider}. The engine's built-in defaults already supply the
 * shell routes (main menu, preferences, save/load, …), so the smallest possible game just names
 * itself and picks a startup route. Grow it from here by adding scene / route modules and real
 * content (see {@code examples/integration-patterns} for richer providers).
 *
 * <p>Discovered by the engine via {@link java.util.ServiceLoader} because {@code module-info.java}
 * declares it with {@code provides … with …} (and {@code StarterGame} also passes it explicitly).</p>
 */
public final class StarterModuleProvider implements EngineModuleProvider {

    /** Public no-arg constructor — required by {@link java.util.ServiceLoader}. */
    public StarterModuleProvider() {
    }

    @Override
    public void contribute(ModuleContext context) {
        context.addStaticContentModule(new StarterContent());
        // Next steps, as your game grows:
        //   context.addSceneModule(new MyScenes());
        //   context.addRouteModule(new MyRoutes());
        //   context.fonts().registerFromModule(getClass(), "/com/example/starter/fonts/Game.ttf", 12);
        //   context.addStylesheet(getClass().getResource("/com/example/starter/theme.css").toExternalForm());
    }

    /** Minimal content: a display name and the route the game opens on launch. */
    private static final class StarterContent implements StaticContentModule {
        @Override
        public void register(ContentRegistry content, ImageDisplayRegistry images) {
            content.registerDefinition("application.name", "Starter Game");
            content.registerDefinition("startup.route", SceneRouter.MAIN_MENU_ROUTE);
        }

        @Override
        public void validate(ContentRegistry content, ImageDisplayRegistry images) {
            // Nothing to validate for the minimal starter.
        }
    }
}
