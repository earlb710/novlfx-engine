package com.eb.javafx.ui;

/**
 * UI-neutral four-side inset values applied around a main-app-layout slot's content.
 *
 * <p>Used for the {@code storyInsets} and {@code dialogInsets} screen metadata keys parsed by
 * {@link MainAppLayoutPlan}. Authors supply the values as a CSS-style shorthand string:</p>
 * <ul>
 *   <li>{@code "10"} — all four sides at 10</li>
 *   <li>{@code "10, 5"} — top/bottom 10, left/right 5</li>
 *   <li>{@code "10, 5, 8, 3"} — top 10, right 5, bottom 8, left 3</li>
 * </ul>
 *
 * <p>Values must be non-negative; the renderer translates them to a JavaFX {@code Insets} object
 * when applying padding to the corresponding slot.</p>
 */
public record MainAppLayoutInsets(double top, double right, double bottom, double left) {
    public static final MainAppLayoutInsets EMPTY = new MainAppLayoutInsets(0.0, 0.0, 0.0, 0.0);

    public MainAppLayoutInsets {
        if (top < 0.0 || right < 0.0 || bottom < 0.0 || left < 0.0) {
            throw new IllegalArgumentException(
                    "Main app layout insets must be non-negative: "
                            + "top=" + top + ", right=" + right + ", bottom=" + bottom + ", left=" + left);
        }
    }

    public boolean isEmpty() {
        return top == 0.0 && right == 0.0 && bottom == 0.0 && left == 0.0;
    }

    /**
     * Parses a CSS-style shorthand value with 1, 2, or 4 numbers. Returns {@link #EMPTY} for
     * {@code null} or blank input.
     *
     * @throws IllegalArgumentException if the value is malformed or has 3 or more-than-4 entries
     */
    public static MainAppLayoutInsets parse(String shorthand, String fieldDescription) {
        if (shorthand == null || shorthand.isBlank()) {
            return EMPTY;
        }
        String[] tokens = shorthand.trim().split("\\s*,\\s*");
        double[] values = new double[tokens.length];
        for (int index = 0; index < tokens.length; index++) {
            try {
                values[index] = Double.parseDouble(tokens[index]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        fieldDescription + " must be a number or a comma-separated list of numbers, found: "
                                + shorthand, exception);
            }
        }
        return switch (values.length) {
            case 1 -> new MainAppLayoutInsets(values[0], values[0], values[0], values[0]);
            case 2 -> new MainAppLayoutInsets(values[0], values[1], values[0], values[1]);
            case 4 -> new MainAppLayoutInsets(values[0], values[1], values[2], values[3]);
            default -> throw new IllegalArgumentException(
                    fieldDescription + " must have 1, 2, or 4 comma-separated values (CSS shorthand), found: "
                            + shorthand);
        };
    }
}
