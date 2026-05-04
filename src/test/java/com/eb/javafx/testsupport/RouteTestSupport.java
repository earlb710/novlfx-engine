package com.eb.javafx.testsupport;

import com.eb.javafx.routing.RouteCategory;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.SceneRouter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reusable route assertions for downstream application route modules. */
public final class RouteTestSupport {
    private RouteTestSupport() {
    }

    public static void assertRoute(SceneRouter router, String routeId, RouteCategory category, boolean migrated) {
        assertTrue(router.routeDescriptors().containsKey(routeId), () -> "Missing route " + routeId);
        RouteDescriptor descriptor = router.routeDescriptors().get(routeId);
        assertEquals(category, descriptor.category());
        assertEquals(migrated, descriptor.migrated());
    }
}
