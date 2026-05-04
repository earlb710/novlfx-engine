import com.eb.javafx.audio.AudioAdapterPolicy;
import com.eb.javafx.audio.AudioPlaybackLifecycleEvent;
import com.eb.javafx.audio.AudioService;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AudioAdapterPolicyDemo {
    public static void main(String[] args) {
        AudioAdapterPolicy policy = new AudioAdapterPolicy(
                sourcePath -> Optional.of(URI.create("file:///app/audio/" + sourcePath)),
                Map.of(AudioService.MUSIC_CHANNEL, 1, AudioService.SOUND_CHANNEL, 8),
                Duration.ofMillis(300),
                true,
                List.of(
                        AudioPlaybackLifecycleEvent.PRELOAD,
                        AudioPlaybackLifecycleEvent.PLAY,
                        AudioPlaybackLifecycleEvent.FADE,
                        AudioPlaybackLifecycleEvent.CROSSFADE,
                        AudioPlaybackLifecycleEvent.STOP));

        System.out.println("Resolved theme: " + policy.assetResolver().resolve("theme.ogg").orElseThrow());
        System.out.println("Music players: " + policy.playerPoolSizes().get(AudioService.MUSIC_CHANNEL));
        System.out.println("Fade ms: " + policy.defaultFadeDuration().toMillis());
        System.out.println("Preload: " + policy.preloadEnabled());
    }
}
