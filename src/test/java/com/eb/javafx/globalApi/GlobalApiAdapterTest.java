package com.eb.javafx.globalApi;

import com.eb.javafx.audio.AudioPlaybackCommand;
import com.eb.javafx.audio.AudioService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.ui.DisplayDefaults;
import com.eb.javafx.ui.UiTheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GlobalApiAdapterTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
        DisplayDefaults.resetActive();
    }

    @Test
    void validatesRouteRequestsAndScreenVisibility() {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();
        new EnginePlaceholderContentModule().register(contentRegistry, null);
        SceneRouter sceneRouter = new SceneRouter();
        SaveLoadService saveLoadService = new SaveLoadService();
        saveLoadService.initialize();
        UiTheme uiTheme = new UiTheme();
        uiTheme.initialize(preferencesService);
        sceneRouter.registerDefaultRoutes(null, preferencesService, contentRegistry, new ImageDisplayRegistry(), saveLoadService, uiTheme);

        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        AudioService audioService = new AudioService();
        audioService.initialize(preferencesService);
        GlobalApiAdapter adapter = new GlobalApiAdapter(randomService, sceneRouter, audioService);

        assertEquals(GlobalRouteAction.JUMP, adapter.jump(SceneRouter.MAIN_MENU_ROUTE).action());
        assertEquals(GlobalRouteAction.SHOW_SCREEN, adapter.showScreen(SceneRouter.HUD_ROUTE).action());
        assertTrue(adapter.visibleScreens().contains(SceneRouter.HUD_ROUTE));
        assertEquals(GlobalRouteAction.HIDE_SCREEN, adapter.hideScreen(SceneRouter.HUD_ROUTE).action());
        assertTrue(adapter.visibleScreens().isEmpty());
        assertThrows(IllegalStateException.class, () -> adapter.jump("missing"));
    }

    @Test
    void backgroundMusicLoopsWhileOtherChannelsPlayOnce() {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        SceneRouter sceneRouter = new SceneRouter();
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        AudioService audioService = new AudioService();
        audioService.initialize(preferencesService);
        GlobalApiAdapter adapter = new GlobalApiAdapter(randomService, sceneRouter, audioService);

        // Background music repeats when finished.
        AudioPlaybackCommand music = adapter.playSound(AudioService.MUSIC_CHANNEL, "music/theme.ogg");
        assertTrue(music.loop(), "Music channel playback should loop.");

        AudioPlaybackCommand explicitMusic = adapter.playMusic("music/theme.ogg");
        assertTrue(explicitMusic.loop(), "playMusic should loop.");

        // Other channels stay one-shot.
        AudioPlaybackCommand sound = adapter.playSound(AudioService.SOUND_CHANNEL, "sfx/click.ogg");
        assertFalse(sound.loop(), "Non-music channels should play once.");
    }
}
