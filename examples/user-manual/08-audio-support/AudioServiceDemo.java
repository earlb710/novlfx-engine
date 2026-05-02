import com.eb.javafx.audio.AudioChannelDefinition;
import com.eb.javafx.audio.AudioPlaybackCommand;
import com.eb.javafx.audio.AudioPlaybackAdapter;
import com.eb.javafx.audio.AudioService;
import com.eb.javafx.audio.SoundRequest;
import com.eb.javafx.prefs.PreferencesService;

/**
 * Demonstrates audio channel registration and playback-command calculation without real media playback.
 *
 * <p>Expected output logs music, ambience, muted sound commands, stop requests, and effective volume values.</p>
 */
public final class AudioServiceDemo {
    private AudioServiceDemo() {
    }

    public static void main(String[] args) {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        preferencesService.saveMasterVolume(0.8);

        AudioService audioService = new AudioService();
        audioService.initialize(preferencesService);
        audioService.registerChannel(new AudioChannelDefinition("ambience", "Looping ambient beds.", true, 2, 0.6));
        audioService.setChannelVolume(AudioService.MUSIC_CHANNEL, 0.5);
        audioService.setChannelVolume("ambience", 0.7);
        AudioPlaybackAdapter playbackAdapter = new LoggingAudioAdapter();

        AudioPlaybackCommand musicCommand = audioService.play(
                new SoundRequest(AudioService.MUSIC_CHANNEL, "audio/bgm/opening-theme.ogg", true, 0.75));
        playbackAdapter.play(musicCommand);
        AudioPlaybackCommand ambienceCommand = audioService.play(
                new SoundRequest("ambience", "audio/ambience/rain.ogg", true, 0.5));
        playbackAdapter.play(ambienceCommand);

        audioService.setMuted(true);
        AudioPlaybackCommand mutedCommand = audioService.play(
                new SoundRequest(AudioService.EFFECTS_CHANNEL, "audio/sfx/click.ogg", false, 1.0));
        audioService.stopChannel("ambience");
        playbackAdapter.stopChannel("ambience");

        System.out.println(musicCommand.channelId());
        System.out.println(musicCommand.sourcePath());
        System.out.println(musicCommand.effectiveVolume());
        System.out.println("Ambience channel capacity: " + audioService.channels().get("ambience").simultaneousSounds());
        System.out.println("Last music command present: " + audioService.lastPlaybackCommand(AudioService.MUSIC_CHANNEL).isPresent());
        System.out.println("Muted effect volume: " + mutedCommand.effectiveVolume());
        System.out.println("Ambience active after stop: " + audioService.lastPlaybackCommand("ambience").isPresent());
    }

    private static final class LoggingAudioAdapter implements AudioPlaybackAdapter {
        @Override
        public void play(AudioPlaybackCommand command) {
            System.out.println("Adapter play -> " + command.channelId() + " @ " + command.effectiveVolume());
        }

        @Override
        public void stopChannel(String channelId) {
            System.out.println("Adapter stop -> " + channelId);
        }
    }
}
