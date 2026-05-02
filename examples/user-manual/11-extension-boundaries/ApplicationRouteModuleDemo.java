import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.routing.RouteCategory;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.ui.ScreenActionViewModel;
import com.eb.javafx.ui.ScreenViewModel;
import com.eb.javafx.ui.ViewModelScreen;

import java.util.List;

/**
 * Demonstrates application-owned content and route modules extending the reusable engine routing boundary.
 *
 * <p>Expected output prints the custom route descriptor, inventory screen model, and registered content value.</p>
 */
public final class ApplicationRouteModuleDemo {
    private ApplicationRouteModuleDemo() {
    }

    public static final class DemoContentModule implements StaticContentModule {
        @Override
        public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
            contentRegistry.registerDefinition("application.name", "demo-app");
            contentRegistry.registerDefinition("startup.route", "inventory");
            contentRegistry.registerDefinition("ui.inventory.title", "Inventory");
        }

        @Override
        public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
            contentRegistry.definition("ui.inventory.title");
        }
    }

    public static final class DemoInventoryRouteModule implements RouteModule {
        @Override
        public void registerRoutes(com.eb.javafx.routing.SceneRouter router) {
            router.registerRoute(
                    new RouteDescriptor(
                            "inventory",
                            "ui.inventory.title",
                            RouteCategory.MENU,
                            true,
                            "Application-owned inventory screen with app-specific controls."),
                    context -> ViewModelScreen.createScene(context, new ScreenViewModel(
                            context.contentRegistry().definition("ui.inventory.title"),
                            List.of("Keep engine screens generic and move domain controls into app route modules."),
                            List.of(new ScreenActionViewModel("Back to main menu", "main-menu", true)))));
        }
    }
}
