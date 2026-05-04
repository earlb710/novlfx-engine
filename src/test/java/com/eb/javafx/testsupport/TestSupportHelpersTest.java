package com.eb.javafx.testsupport;

import com.eb.javafx.routing.RouteCategory;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.ui.HudStatusRowViewModel;
import org.junit.jupiter.api.Test;

import java.util.Map;

final class TestSupportHelpersTest {
    @Test
    void recordAssertionsInspectViewModelComponents() {
        RecordAssertions.assertRecordComponents(HudStatusRowViewModel.class, "label", "value", "visible");
        RecordAssertions.assertComponentValues(new HudStatusRowViewModel("Day", "1", true),
                Map.of("label", "Day", "value", "1", "visible", true));
    }

    @Test
    void routeHelperAssertsDescriptorShape() {
        SceneRouter router = new SceneRouter();
        router.registerRoute(new RouteDescriptor("main", "title", RouteCategory.MENU, true, "ready"),
                routeContext -> null);

        RouteTestSupport.assertRoute(router, "main", RouteCategory.MENU, true);
    }

    @Test
    void sceneFlowFixturesBuildValidatedRegistry() {
        SceneDefinition intro = SceneFlowTestFixtures.completeScene("intro");
        SceneRegistry registry = SceneFlowTestFixtures.registryWith(intro);

        registry.requireScene("intro");
    }
}
