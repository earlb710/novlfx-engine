import com.eb.javafx.audio.AudioPlaybackAdapter;
import com.eb.javafx.audio.JavaFxAudioPlaybackAdapter;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Demonstrates an application-owned adapter that binds validated audio commands to JavaFX media APIs.
 *
 * <p>Expected behavior: the application owns media files and path policy while the reusable adapter maps
 * validated commands to JavaFX MediaPlayer/AudioClip playback.</p>
 */
public final class JavaFxAudioPlaybackAdapterDemo {
    private final AudioPlaybackAdapter adapter;

    public JavaFxAudioPlaybackAdapterDemo(Path applicationRoot) {
        Path root = applicationRoot.toAbsolutePath().normalize();
        this.adapter = new JavaFxAudioPlaybackAdapter(sourcePath ->
                Optional.of(root.resolve(sourcePath).normalize().toUri()).filter(URI::isAbsolute));
    }

    public AudioPlaybackAdapter adapter() {
        return adapter;
    }
}
