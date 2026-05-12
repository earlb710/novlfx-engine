package com.eb.javafx.routing;

import com.eb.javafx.accessibility.AccessibilityProfile;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class ScreenVariantResolverTest {

    private static AccessibilityProfile defaults() {
        return AccessibilityProfile.defaults();
    }

    @Test
    void noVariantsReturnsEmpty() {
        RouteDescriptor descriptor = new RouteDescriptor("id", "title", RouteCategory.SCREEN, false, "active");
        ScreenVariantResolver resolver = new ScreenVariantResolver();
        Optional<String> result = resolver.resolve(descriptor, WindowSizeClass.COMPACT, defaults());
        assertTrue(result.isEmpty());
    }

    @Test
    void matchingVariantReturnsJsonPath() {
        ScreenVariantCriteria compact = ScreenVariantCriteria.forSizeClass(WindowSizeClass.COMPACT);
        RouteDescriptor descriptor = new RouteDescriptor("id", "title", RouteCategory.SCREEN, false, "active")
                .withVariant(compact, "screens/id_compact.json");
        ScreenVariantResolver resolver = new ScreenVariantResolver();
        Optional<String> result = resolver.resolve(descriptor, WindowSizeClass.COMPACT, defaults());
        assertEquals("screens/id_compact.json", result.orElseThrow());
    }

    @Test
    void nonMatchingVariantReturnsEmpty() {
        ScreenVariantCriteria compact = ScreenVariantCriteria.forSizeClass(WindowSizeClass.COMPACT);
        RouteDescriptor descriptor = new RouteDescriptor("id", "title", RouteCategory.SCREEN, false, "active")
                .withVariant(compact, "screens/id_compact.json");
        ScreenVariantResolver resolver = new ScreenVariantResolver();
        Optional<String> result = resolver.resolve(descriptor, WindowSizeClass.EXPANDED, defaults());
        assertTrue(result.isEmpty());
    }

    @Test
    void firstMatchingVariantWinsInRegistrationOrder() {
        ScreenVariantCriteria compact = ScreenVariantCriteria.forSizeClass(WindowSizeClass.COMPACT);
        ScreenVariantCriteria compactHighContrast = ScreenVariantCriteria.forSizeClass(WindowSizeClass.COMPACT)
                .withHighContrast(true);
        RouteDescriptor descriptor = new RouteDescriptor("id", "title", RouteCategory.SCREEN, false, "active")
                .withVariant(compact, "screens/id_compact.json")
                .withVariant(compactHighContrast, "screens/id_compact_hc.json");
        ScreenVariantResolver resolver = new ScreenVariantResolver();
        AccessibilityProfile hc = new AccessibilityProfile(1.0, true, false, false, false);
        Optional<String> result = resolver.resolve(descriptor, WindowSizeClass.COMPACT, hc);
        assertEquals("screens/id_compact.json", result.orElseThrow());
    }

    @Test
    void highContrastOnlyCriteriaMatchesProfile() {
        ScreenVariantCriteria highContrast = ScreenVariantCriteria.forHighContrast(true);
        RouteDescriptor descriptor = new RouteDescriptor("id", "title", RouteCategory.SCREEN, false, "active")
                .withVariant(highContrast, "screens/id_hc.json");
        ScreenVariantResolver resolver = new ScreenVariantResolver();
        AccessibilityProfile hc = new AccessibilityProfile(1.0, true, false, false, false);
        Optional<String> result = resolver.resolve(descriptor, WindowSizeClass.MEDIUM, hc);
        assertEquals("screens/id_hc.json", result.orElseThrow());
    }

    @Test
    void highContrastCriteriaDoesNotMatchDefaultProfile() {
        ScreenVariantCriteria highContrast = ScreenVariantCriteria.forHighContrast(true);
        RouteDescriptor descriptor = new RouteDescriptor("id", "title", RouteCategory.SCREEN, false, "active")
                .withVariant(highContrast, "screens/id_hc.json");
        ScreenVariantResolver resolver = new ScreenVariantResolver();
        Optional<String> result = resolver.resolve(descriptor, WindowSizeClass.MEDIUM, defaults());
        assertTrue(result.isEmpty());
    }
}
