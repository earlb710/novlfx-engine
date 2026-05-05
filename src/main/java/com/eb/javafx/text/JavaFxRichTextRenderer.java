package com.eb.javafx.text;

import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.util.Validation;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Renders parsed visual-novel text tokens into JavaFX rich text nodes. */
public final class JavaFxRichTextRenderer {
    private static final String EFFECT_PROPERTY = "novlfx.text.effects";
    private static final String ICON_PROPERTY = "novlfx.text.iconId";
    private static final String DEFAULT_GRADIENT = "#ffffff,#80d8ff";
    private static final double DEFAULT_INLINE_ICON_HEIGHT = 18.0;
    private static final double DEFAULT_INLINE_ICON_BASELINE_OFFSET = 3.0;

    private final TextTagParser parser;
    private final ImageDisplayRegistry imageDisplayRegistry;
    private final Function<String, Node> inlineIconFactory;
    private final boolean playKineticEffects;

    public JavaFxRichTextRenderer() {
        this(new TextTagParser(), null, null, false);
    }

    public JavaFxRichTextRenderer(TextTagParser parser) {
        this(parser, null, null, false);
    }

    public JavaFxRichTextRenderer(ImageDisplayRegistry imageDisplayRegistry) {
        this(new TextTagParser(), imageDisplayRegistry, null, false);
    }

    public JavaFxRichTextRenderer(TextTagParser parser, boolean playKineticEffects) {
        this(parser, null, null, playKineticEffects);
    }

    public JavaFxRichTextRenderer(TextTagParser parser, ImageDisplayRegistry imageDisplayRegistry) {
        this(parser, imageDisplayRegistry, null, false);
    }

    public JavaFxRichTextRenderer(TextTagParser parser, ImageDisplayRegistry imageDisplayRegistry, boolean playKineticEffects) {
        this(parser, imageDisplayRegistry, null, playKineticEffects);
    }

    JavaFxRichTextRenderer(TextTagParser parser, Function<String, Node> inlineIconFactory) {
        this(parser, null, inlineIconFactory, false);
    }

    private JavaFxRichTextRenderer(TextTagParser parser, ImageDisplayRegistry imageDisplayRegistry,
                                   Function<String, Node> inlineIconFactory, boolean playKineticEffects) {
        this.parser = Validation.requireNonNull(parser, "Text tag parser is required.");
        this.imageDisplayRegistry = imageDisplayRegistry;
        this.inlineIconFactory = inlineIconFactory;
        this.playKineticEffects = playKineticEffects;
    }

    /** Parses tagged source text and renders it into a {@link TextFlow}. */
    public TextFlow render(String source) {
        return render(parser.parse(source));
    }

    /** Renders pre-parsed text tokens into a {@link TextFlow}. */
    public TextFlow render(List<TextToken> tokens) {
        TextFlow flow = new TextFlow();
        flow.getChildren().addAll(nodes(tokens));
        return flow;
    }

    /** Converts renderable tokens to JavaFX text/image nodes; pause tokens are rendering-neutral. */
    public List<Node> nodes(List<TextToken> tokens) {
        Validation.requireNonNull(tokens, "Text tokens are required.");
        List<Node> nodes = new ArrayList<>();
        for (TextToken token : tokens) {
            if (token.type() == TextTokenType.TEXT) {
                nodes.add(textNode(token.text(), token.style()));
            } else if (token.type() == TextTokenType.ICON) {
                nodes.add(iconNode(token.iconId()));
            } else if (token.type() == TextTokenType.PARAGRAPH) {
                nodes.add(textNode(System.lineSeparator(), TextStyle.plain()));
            }
        }
        return List.copyOf(nodes);
    }

    /** Converts text and paragraph tokens to JavaFX text nodes; pause tokens are rendering-neutral. */
    public List<Text> textNodes(List<TextToken> tokens) {
        Validation.requireNonNull(tokens, "Text tokens are required.");
        List<Text> nodes = new ArrayList<>();
        for (TextToken token : tokens) {
            if (token.type() == TextTokenType.TEXT) {
                nodes.add(textNode(token.text(), token.style()));
            } else if (token.type() == TextTokenType.PARAGRAPH) {
                nodes.add(textNode(System.lineSeparator(), TextStyle.plain()));
            }
        }
        return List.copyOf(nodes);
    }

    private Text textNode(String value, TextStyle style) {
        Text text = new Text(value);
        applyStyle(text, style);
        applyEffects(text, style.effects());
        return text;
    }

    private Node iconNode(String iconId) {
        if (inlineIconFactory != null) {
            Node iconNode = inlineIconFactory.apply(iconId);
            if (iconNode != null) {
                return tagIconNode(iconId, iconNode);
            }
        }
        if (imageDisplayRegistry != null) {
            try {
                return imageDisplayRegistry.createImageView(iconId)
                        .<Node>map(imageView -> tagIconNode(iconId, configureInlineIcon(imageView)))
                        .orElseGet(() -> fallbackIconNode(iconId));
            } catch (IllegalStateException exception) {
                return fallbackIconNode(iconId);
            }
        }
        return fallbackIconNode(iconId);
    }

    private Node tagIconNode(String iconId, Node node) {
        node.getProperties().put(ICON_PROPERTY, iconId);
        return node;
    }

    private ImageView configureInlineIcon(ImageView imageView) {
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(0.0);
        imageView.setFitHeight(DEFAULT_INLINE_ICON_HEIGHT);
        imageView.setTranslateY(DEFAULT_INLINE_ICON_BASELINE_OFFSET);
        return imageView;
    }

    private Text fallbackIconNode(String iconId) {
        Text fallback = textNode("[" + iconId + "]", TextStyle.plain());
        fallback.getProperties().put(ICON_PROPERTY, iconId);
        return fallback;
    }

    private void applyStyle(Text text, TextStyle style) {
        TextStyle checkedStyle = Validation.requireNonNull(style, "Text style is required.");
        String family = checkedStyle.fontFamily() == null || checkedStyle.fontFamily().isBlank()
                ? Font.getDefault().getFamily()
                : checkedStyle.fontFamily();
        FontWeight weight = checkedStyle.bold() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = checkedStyle.italic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        text.setFont(Font.font(family, weight, posture, Font.getDefault().getSize()));
        if (checkedStyle.color() != null && !checkedStyle.color().isBlank()) {
            text.setFill(paintOrNull(checkedStyle.color(), text.getFill()));
        }
    }

    private void applyEffects(Text text, Map<String, String> effects) {
        if (effects.isEmpty()) {
            return;
        }
        text.getProperties().put(EFFECT_PROPERTY, effects);
        effects.forEach((id, value) -> applyEffect(text, id, value));
    }

    private void applyEffect(Text text, String id, String value) {
        switch (id) {
            case "gradient" -> text.setFill(gradient(value));
            case "kinetic" -> applyKinetic(text, value);
            case "glitch" -> applyGlitch(text, value);
            default -> text.getProperties().put("novlfx.text.effect." + id, value);
        }
    }

    private Paint gradient(String value) {
        String[] parts = (value == null || value.isBlank() ? DEFAULT_GRADIENT : value).split(",");
        List<Stop> stops = new ArrayList<>();
        for (int index = 0; index < parts.length; index++) {
            Paint paint = paintOrNull(parts[index].trim(), null);
            if (paint instanceof Color color) {
                double offset = parts.length == 1 ? 1.0 : (double) index / (double) (parts.length - 1);
                stops.add(new Stop(offset, color));
            }
        }
        if (stops.isEmpty()) {
            stops.add(new Stop(0.0, Color.WHITE));
            stops.add(new Stop(1.0, Color.LIGHTBLUE));
        } else if (stops.size() == 1) {
            stops.add(new Stop(1.0, stops.get(0).getColor()));
        }
        return new LinearGradient(0.0, 0.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE, stops);
    }

    private Paint paintOrNull(String value, Paint fallback) {
        try {
            return Paint.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private void applyKinetic(Text text, String value) {
        String mode = value == null ? "" : value.trim().toLowerCase();
        if ("pulse".equals(mode)) {
            FadeTransition transition = new FadeTransition(Duration.millis(650), text);
            transition.setFromValue(0.65);
            transition.setToValue(1.0);
            configureLooping(transition, text);
            return;
        }
        TranslateTransition transition = new TranslateTransition(Duration.millis("float".equals(mode) ? 900 : 120), text);
        if ("float".equals(mode)) {
            transition.setFromY(-2.0);
            transition.setToY(2.0);
        } else {
            transition.setFromX(-1.5);
            transition.setToX(1.5);
        }
        configureLooping(transition, text);
    }

    private void configureLooping(Animation animation, Node node) {
        animation.setAutoReverse(true);
        animation.setCycleCount(Animation.INDEFINITE);
        node.getProperties().put("novlfx.text.kineticAnimation", animation);
        if (playKineticEffects) {
            animation.play();
        }
    }

    private void applyGlitch(Text text, String value) {
        DropShadow shadow = new DropShadow();
        shadow.setRadius("heavy".equalsIgnoreCase(value) ? 3.0 : 1.5);
        shadow.setOffsetX("heavy".equalsIgnoreCase(value) ? 2.0 : 1.0);
        shadow.setOffsetY(0.0);
        shadow.setColor(Color.rgb(255, 0, 80, 0.8));
        text.setEffect(shadow);
        text.getProperties().put("novlfx.text.glitch", value == null ? "" : value);
    }
}
