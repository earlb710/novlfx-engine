import com.eb.javafx.text.TextTagParser;
import com.eb.javafx.text.TextToken;
import com.eb.javafx.text.TextTokenType;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.InitializationGuard;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.PathUtils;
import com.eb.javafx.util.Result;
import com.eb.javafx.util.TimeFormatting;
import com.eb.javafx.util.Validation;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates text tag parsing together with validation, result, JSON, path, collection, and time helpers.
 *
 * <p>Expected output prints parsed token details plus representative utility helper results.</p>
 */
public final class TextAndUtilityDemo {
    private TextAndUtilityDemo() {
    }

    public static void main(String[] args) {
        TextTagParser parser = new TextTagParser();
        List<TextToken> tokens = parser.parse("{b}Alert{/b}{w=0.25}{p}Continue");
        TextToken firstToken = tokens.get(0);

        Validation.requirePositive(tokens.size(), "Expected parsed tokens.");
        Map<String, String> metadata = ImmutableCollections.copyMap(Map.of("speaker", "narrator"));
        Result<String> summary = Result.success("Parsed " + tokens.size() + " tokens.");

        Path imagePath = PathUtils.resolveChild(Path.of("/games/demo"), "assets/ui/frame.png");
        String quoted = JsonStrings.quote("speaker=\"narrator\"");
        String elapsed = TimeFormatting.formatElapsedMillis(Instant.now().minusSeconds(2), Instant.now());

        InitializationGuard guard = new InitializationGuard("Demo service not initialized.");
        guard.markInitialized();
        guard.requireInitialized();

        System.out.println(summary.orElse("No summary"));
        tokens.forEach(token -> System.out.println(token.type()
                + " -> text='" + token.text()
                + "', bold=" + token.style().bold()
                + ", italic=" + token.style().italic()
                + ", color=" + token.style().color()
                + ", effects=" + token.style().effects()
                + ", duration=" + token.durationSeconds()));
        System.out.println("Text token count: " + tokens.stream().filter(token -> token.type() == TextTokenType.TEXT).count());
        System.out.println("First token bold: " + firstToken.style().bold());
        System.out.println(metadata);
        System.out.println(imagePath);
        System.out.println(quoted);
        System.out.println(elapsed);
    }
}
