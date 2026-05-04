package com.eb.javafx.bootstrap;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.globalApi.GlobalApiAdapter;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.routing.RouteCategory;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.state.GameState;
import com.eb.javafx.ui.UiTheme;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BootstrapCompletenessPolicyTest {
    @Test
    void policyReportsMissingReusableRequirements() {
        BootContext context = contextWith(EnumSet.of(BootstrapPhase.CORE_SERVICES), false, false);
        BootstrapCompletenessPolicy policy = new BootstrapCompletenessPolicy(
                EnumSet.allOf(BootstrapPhase.class),
                List.of("startup"),
                List.of("application.name"));

        BootstrapCompletenessReport report = policy.evaluate(context);

        assertFalse(report.complete());
        assertEquals(6, report.problems().size());
        assertThrows(IllegalStateException.class, report::requireComplete);
    }

    @Test
    void policyPassesWhenPhasesRoutesAndContentExist() {
        BootContext context = contextWith(EnumSet.allOf(BootstrapPhase.class), true, true);
        BootstrapCompletenessPolicy policy = new BootstrapCompletenessPolicy(
                EnumSet.allOf(BootstrapPhase.class),
                List.of("startup"),
                List.of("application.name"));

        assertTrue(policy.evaluate(context).complete());
    }

    private BootContext contextWith(EnumSet<BootstrapPhase> phases, boolean route, boolean content) {
        ContentRegistry contentRegistry = new ContentRegistry();
        if (content) {
            contentRegistry.registerDefinition("application.name", "Demo");
        }
        SceneRouter router = new SceneRouter();
        if (route) {
            router.registerRoute(new RouteDescriptor("startup", "application.name", RouteCategory.MENU, true, "ready"),
                    routeContext -> null);
        }
        PreferencesService preferencesService = new PreferencesService();
        GameRandomService randomService = new GameRandomService();
        AudioService audioService = new AudioService();
        return new BootContext(
                preferencesService,
                contentRegistry,
                new ImageDisplayRegistry(),
                new SaveLoadService(),
                randomService,
                audioService,
                new GameSupportService(),
                new SceneRegistry(),
                new SceneExecutor(new SceneRegistry()),
                new GlobalApiAdapter(randomService, router, audioService),
                router,
                new UiTheme(),
                new GameState("startup"),
                new BootstrapReport(Instant.EPOCH, Instant.EPOCH, phases, Map.of()));
    }
}
