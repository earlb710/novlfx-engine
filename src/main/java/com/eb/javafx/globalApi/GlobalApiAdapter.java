package com.eb.javafx.globalApi;

import com.eb.javafx.audio.AudioPlaybackCommand;
import com.eb.javafx.audio.AudioService;
import com.eb.javafx.audio.SoundRequest;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.routing.SceneRouter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Section 1.6 adapter for global API calls.
 *
 * <p>The adapter provides stable Java methods for migrated code that needs
 * global random, route, screen, and sound requests while delegating validation
 * to JavaFX services.</p>
 */
public final class GlobalApiAdapter {
    private final GameRandomService randomService;
    private final SceneRouter sceneRouter;
    private final AudioService audioService;
    private final Set<String> visibleScreens = new LinkedHashSet<>();
    private GlobalRouteRequest lastRouteRequest;

    public GlobalApiAdapter(GameRandomService randomService, SceneRouter sceneRouter, AudioService audioService) {
        if (randomService == null || sceneRouter == null || audioService == null) {
            throw new IllegalArgumentException("Global API adapter requires random, route, and audio services.");
        }
        this.randomService = randomService;
        this.sceneRouter = sceneRouter;
        this.audioService = audioService;
    }

    /** Returns a deterministic gameplay random value for global API callers. */
    public int randomInt(int bound) {
        return randomService.nextGameplayInt(bound);
    }

    /** Validates and records a global route transition. */
    public GlobalRouteRequest jump(String routeId) {
        return routeRequest(GlobalRouteAction.JUMP, routeId);
    }

    /** Validates and records a resumable global route transition. */
    public GlobalRouteRequest call(String routeId) {
        return routeRequest(GlobalRouteAction.CALL, routeId);
    }

    /** Validates and marks a global screen route visible. */
    public GlobalRouteRequest showScreen(String routeId) {
        GlobalRouteRequest request = routeRequest(GlobalRouteAction.SHOW_SCREEN, routeId);
        visibleScreens.add(routeId);
        return request;
    }

    /** Validates and marks a global screen route hidden. */
    public GlobalRouteRequest hideScreen(String routeId) {
        GlobalRouteRequest request = routeRequest(GlobalRouteAction.HIDE_SCREEN, routeId);
        visibleScreens.remove(routeId);
        return request;
    }

    /** Returns immutable visible screen IDs recorded through this adapter. */
    public Set<String> visibleScreens() {
        return Collections.unmodifiableSet(visibleScreens);
    }

    /** Returns the last validated navigation/screen request. */
    public Optional<GlobalRouteRequest> lastRouteRequest() {
        return Optional.ofNullable(lastRouteRequest);
    }

    /** Validates and records a sound request through the JavaFX audio service. */
    public AudioPlaybackCommand playSound(String channelId, String sourcePath) {
        return audioService.play(new SoundRequest(channelId, sourcePath, false, 1.0));
    }

    private GlobalRouteRequest routeRequest(GlobalRouteAction action, String routeId) {
        if (!sceneRouter.routeDescriptors().containsKey(routeId)) {
            throw new IllegalStateException("Missing route for Global API adapter request: " + routeId);
        }
        lastRouteRequest = new GlobalRouteRequest(action, routeId);
        return lastRouteRequest;
    }
}
