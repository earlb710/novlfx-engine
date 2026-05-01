import com.eb.javafx.text.TextTagParser;
import com.eb.javafx.text.TextToken;
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

public final class TextAndUtilityDemo {
    private TextAndUtilityDemo() {
    }

    public static void main(String[] args) {
        TextTagParser parser = new TextTagParser();
        List<TextToken> tokens = parser.parse("{b}Alert{/b}{w=0.25}{p}Continue");

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
        System.out.println(metadata);
        System.out.println(imagePath);
        System.out.println(quoted);
        System.out.println(elapsed);
    }
}
