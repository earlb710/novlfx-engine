package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class LayeredImageResolverTest {

    private LayeredImageDefinition heroDefinition() {
        return new LayeredImageDefinition("hero", "hero_tag", List.of(
            new LayeredImageLayer("body", List.of(
                new LayeredImageVariant("body/default.png", null)
            )),
            new LayeredImageLayer("expression", List.of(
                new LayeredImageVariant("expr/happy.png", "flag:hero_happy"),
                new LayeredImageVariant("expr/neutral.png", null)
            ))
        ));
    }

    @Test
    void selectsConditionalVariantWhenConditionPasses() {
        LayeredImageResolver resolver = new LayeredImageResolver();
        LayeredImageComposition comp = resolver.resolve(heroDefinition(), expr -> "flag:hero_happy".equals(expr));
        assertEquals(2, comp.entries().size());
        assertEquals("expr/happy.png", comp.entries().get(1).imageRef());
    }

    @Test
    void fallsBackToUnconditionalVariantWhenConditionFails() {
        LayeredImageResolver resolver = new LayeredImageResolver();
        LayeredImageComposition comp = resolver.resolve(heroDefinition(), expr -> false);
        assertEquals("expr/neutral.png", comp.entries().get(1).imageRef());
    }

    @Test
    void omitsLayerWhenNoVariantMatches() {
        LayeredImageDefinition def = new LayeredImageDefinition("item", "item_tag", List.of(
            new LayeredImageLayer("glow", List.of(
                new LayeredImageVariant("glow.png", "flag:glow_active")
            ))
        ));
        LayeredImageResolver resolver = new LayeredImageResolver();
        LayeredImageComposition comp = resolver.resolve(def, expr -> false);
        assertTrue(comp.entries().isEmpty());
    }

    @Test
    void preservesDefinitionId() {
        LayeredImageResolver resolver = new LayeredImageResolver();
        LayeredImageComposition comp = resolver.resolve(heroDefinition(), expr -> false);
        assertEquals("hero", comp.definitionId());
    }
}
