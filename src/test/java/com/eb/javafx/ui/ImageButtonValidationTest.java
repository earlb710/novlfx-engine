package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImageButtonValidationTest {

    private static ScreenDesignBlock block() {
        return new ScreenDesignBlock("main", "Main");
    }

    private static ScreenDesignModel model(ScreenDesignItem item) {
        return new ScreenDesignModel(
                "test-screen",
                "Test Screen",
                ScreenLayoutType.FORM,
                Map.of(),
                List.of(block()),
                List.of(item),
                List.of());
    }

    /** Step 1 – enum constant exists. */
    @Test
    void imageButtonEnumConstantExists() {
        assertNotNull(ScreenDesignItemType.IMAGE_BUTTON);
        assertEquals(ScreenDesignItemType.IMAGE_BUTTON, ScreenDesignItemType.valueOf("IMAGE_BUTTON"));
    }

    /** Step 2 – IMAGE_BUTTON with idleImageRef passes validation. */
    @Test
    void imageButtonWithIdleImageRefPassesValidation() {
        ScreenDesignItem item = new ScreenDesignItem(
                "btn1", "main",
                ScreenDesignItemType.IMAGE_BUTTON,
                null, null, null, null,
                null,
                Map.of("idleImageRef", "images/idle.png"));
        ScreenDesignModel design = model(item);

        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);

        assertTrue(
                problems.stream().noneMatch(p -> p.path().contains("btn1")),
                "Expected no validation problems for btn1, but found: " + problems);
    }

    /** Step 3 – IMAGE_BUTTON WITHOUT idleImageRef fails validation. */
    @Test
    void imageButtonWithoutIdleImageRefFailsValidation() {
        ScreenDesignItem item = new ScreenDesignItem(
                "btn2", "main",
                ScreenDesignItemType.IMAGE_BUTTON,
                null, null, null, null,
                null,
                Map.of());
        ScreenDesignModel design = model(item);

        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);

        assertTrue(
                problems.stream().anyMatch(p -> p.path().contains("btn2")),
                "Expected a validation problem for btn2 (missing idleImageRef), but found none.");
    }

    /** Step 4 – IMAGE_BUTTON with idleImageRef + hoverImageRef + selectedImageRef passes. */
    @Test
    void imageButtonWithAllImageRefsPassesValidation() {
        ScreenDesignItem item = new ScreenDesignItem(
                "btn3", "main",
                ScreenDesignItemType.IMAGE_BUTTON,
                null, null, null, null,
                null,
                Map.of(
                        "idleImageRef", "images/idle.png",
                        "hoverImageRef", "images/hover.png",
                        "selectedImageRef", "images/selected.png"));
        ScreenDesignModel design = model(item);

        List<ScreenDesignValidationProblem> problems = ScreenDesignValidator.validate(design);

        assertTrue(
                problems.stream().noneMatch(p -> p.path().contains("btn3")),
                "Expected no validation problems for btn3, but found: " + problems);
    }
}
