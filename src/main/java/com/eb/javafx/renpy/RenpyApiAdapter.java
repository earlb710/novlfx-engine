package com.eb.javafx.renpy;

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
 * Section 1.6 adapter for Ren'Py-style global API calls.
 *
 * <p>The adapter does not emulate Ren'Py. It provides stable Java methods for
 * migrated code that still thinks in terms of {@code renpy.random},
 * {@code renpy.jump}, {@code renpy.call}, and screen show/hide requests while
 * delegating validation to JavaFX services.</p>
 */
public final class RenpyApiAdapter {
    private final GameRandomService randomService;
    private final SceneRouter sceneRouter;
    private final AudioService audioService;
    private final Set<String> visibleScreens = new LinkedHashSet<>();
    private RenpyRouteRequest lastRouteRequest;

    public RenpyApiAdapter(GameRandomService randomService, SceneRouter sceneRouter, AudioService audioService) {
        if (randomService == null || sceneRouter == null || audioService == null) {
            throw new IllegalArgumentException("Ren'Py API adapter requires random, route, and audio services.");
        }
        this.randomService = randomService;
        this.sceneRouter = sceneRouter;
        this.audioService = audioService;
    }

    /** Returns a deterministic gameplay random value in place of {@code renpy.random}. */
    public int randomInt(int bound) {
        return randomService.nextGameplayInt(bound);
    }

    /** Validates and records a route transition that replaces {@code renpy.jump}. */
    public RenpyRouteRequest jump(String routeId) {
        return routeRequest(RenpyRouteAction.JUMP, routeId);
    }

    /** Validates and records a resumable route transition that replaces {@code renpy.call}. */
    public RenpyRouteRequest call(String routeId) {
        return routeRequest(RenpyRouteAction.CALL, routeId);
    }

    /** Validates and marks a screen route visible in place of {@code renpy.show_screen}. */
    public RenpyRouteRequest showScreen(String routeId) {
        RenpyRouteRequest request = routeRequest(RenpyRouteAction.SHOW_SCREEN, routeId);
        visibleScreens.add(routeId);
        return request;
    }

    /** Validates and marks a screen route hidden in place of {@code renpy.hide_screen}. */
    public RenpyRouteRequest hideScreen(String routeId) {
        RenpyRouteRequest request = routeRequest(RenpyRouteAction.HIDE_SCREEN, routeId);
        visibleScreens.remove(routeId);
        return request;
    }

    /** Returns immutable visible screen IDs recorded through this adapter. */
    public Set<String> visibleScreens() {
        return Collections.unmodifiableSet(visibleScreens);
    }

    /** Returns the last validated navigation/screen request. */
    public Optional<RenpyRouteRequest> lastRouteRequest() {
        return Optional.ofNullable(lastRouteRequest);
    }

    /** Validates and records a sound request through the JavaFX audio service. */
    public AudioPlaybackCommand playSound(String channelId, String sourcePath) {
        return audioService.play(new SoundRequest(channelId, sourcePath, false, 1.0));
    }

    private RenpyRouteRequest routeRequest(RenpyRouteAction action, String routeId) {
        if (!sceneRouter.routeDescriptors().containsKey(routeId)) {
            throw new IllegalStateException("Missing route for Ren'Py adapter request: " + routeId);
        }
        lastRouteRequest = new RenpyRouteRequest(action, routeId);
        return lastRouteRequest;
    }
}
