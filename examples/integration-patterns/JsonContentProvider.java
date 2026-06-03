package com.example.patterns;

import com.eb.javafx.bootstrap.EngineModuleProvider;
import com.eb.javafx.bootstrap.ModuleContext;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.scene.JsonSceneModule;

import java.net.URL;

/**
 * Pattern 3 — Author content as JSON, no per-content Java classes.
 *
 * <p>Scenes and display definitions can be authored in JSON and contributed via the engine's
 * JSON-backed modules. The provider just resolves the JSON resources (from its own module) and
 * registers them. This keeps narrative content out of code so writers / designers can iterate
 * without recompiling.</p>
 *
 * <p>Two ways to do it:</p>
 * <ul>
 *   <li><b>This provider</b> — explicit {@code JsonSceneModule} / {@code JsonDisplayContentModule}
 *       from URLs you control (shown below).</li>
 *   <li><b>Config-driven</b> — declare the JSON directories in {@code config/app-load.json} and the
 *       engine loads them automatically during {@code fromConfig(...)} / {@code discovering(...)};
 *       no provider code at all. See {@code examples/resources/json/config/app-load.json}.</li>
 * </ul>
 */
public final class JsonContentProvider implements EngineModuleProvider {

    public JsonContentProvider() {
    }

    @Override
    public void contribute(ModuleContext context) {
        // Resolve the JSON from THIS module (same-module getResource is always allowed under JPMS).
        URL scenes = getClass().getResource("/com/example/patterns/scenes/scene-definitions.json");
        URL display = getClass().getResource("/com/example/patterns/display/display-definitions.json");

        context.addSceneModule(new JsonSceneModule(scenes));
        context.addStaticContentModule(new JsonDisplayContentModule(display));
        // Conversations: new JsonConversationContentModule(url) implements BOTH StaticContentModule
        // and SceneModule, so add it to both:
        //   var convo = new JsonConversationContentModule(getClass().getResource(".../conversations.json"));
        //   context.addStaticContentModule(convo);
        //   context.addSceneModule(convo);
    }
}
