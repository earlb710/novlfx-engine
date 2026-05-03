import com.eb.javafx.audio.AudioPlaybackAdapter;
import com.eb.javafx.audio.AudioPlaybackCommand;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates an application-owned adapter that binds validated audio commands to JavaFX media APIs.
 *
 * <p>Expected behavior: each channel resolves authored asset paths from the application root and plays
 * them through {@link MediaPlayer} or {@link AudioClip} depending on loop behavior.</p>
 */
public final class JavaFxAudioPlaybackAdapterDemo implements AudioPlaybackAdapter {
    private final Path applicationRoot;
    private final Map<String, MediaPlayer> loopingPlayers = new HashMap<>();
    private final Map<String, AudioClip> clips = new HashMap<>();

    public JavaFxAudioPlaybackAdapterDemo(Path applicationRoot) {
        this.applicationRoot = applicationRoot.toAbsolutePath().normalize();
    }

    @Override
    public void play(AudioPlaybackCommand command) {
        stopChannel(command.channelId());

        String mediaUri = applicationRoot.resolve(command.sourcePath()).normalize().toUri().toString();
        if (command.loop()) {
            MediaPlayer player = new MediaPlayer(new Media(mediaUri));
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(command.effectiveVolume());
            loopingPlayers.put(command.channelId(), player);
            player.play();
            return;
        }

        AudioClip clip = new AudioClip(mediaUri);
        clip.setVolume(command.effectiveVolume());
        clips.put(command.channelId(), clip);
        clip.play();
    }

    @Override
    public void stopChannel(String channelId) {
        MediaPlayer player = loopingPlayers.remove(channelId);
        if (player != null) {
            player.stop();
            player.dispose();
        }

        AudioClip clip = clips.remove(channelId);
        if (clip != null) {
            clip.stop();
        }
    }
}
