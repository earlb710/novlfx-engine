import com.eb.javafx.audio.AudioPlaybackCommand;
import com.eb.javafx.audio.AudioService;
import com.eb.javafx.audio.SoundRequest;
import com.eb.javafx.prefs.PreferencesService;

public final class AudioServiceDemo {
    private AudioServiceDemo() {
    }

    public static void main(String[] args) {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        preferencesService.saveMasterVolume(0.8);

        AudioService audioService = new AudioService();
        audioService.initialize(preferencesService);
        audioService.setChannelVolume(AudioService.MUSIC_CHANNEL, 0.5);

        AudioPlaybackCommand command = audioService.play(
                new SoundRequest(AudioService.MUSIC_CHANNEL, "audio/bgm/opening-theme.ogg", true, 0.75));

        System.out.println(command.channelId());
        System.out.println(command.sourcePath());
        System.out.println(command.effectiveVolume());
    }
}
