package com.eb.javafx.bootstrap;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.ui.GlobalStylesheets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BootstrapOptionsDiscoveryTest {

    // No META-INF/services file ships in test resources, so ServiceLoader.load() inside
    // orderProviders() yields nothing and these assertions depend only on the explicit providers.
    // The ServiceLoader transport itself is standard JDK behaviour, exercised end-to-end in Phase 4
    // when AltLife declares `provides EngineModuleProvider with ...`.

    @BeforeEach
    @AfterEach
    void resetGlobalStylesheets() {
        GlobalStylesheets.clear();
    }

    private static final class StubContentModule implements StaticContentModule {
        @Override public void register(ContentRegistry c, ImageDisplayRegistry i) { }
        @Override public void validate(ContentRegistry c, ImageDisplayRegistry i) { }
    }

    private static EngineModuleProvider provider(String id, int priority, Set<String> deps,
            Consumer<ModuleContext> body) {
        return new EngineModuleProvider() {
            @Override public String id() { return id; }
            @Override public int priority() { return priority; }
            @Override public Set<String> dependsOn() { return deps; }
            @Override public void contribute(ModuleContext context) { body.accept(context); }
        };
    }

    private static List<String> idsOfInterest(List<EngineModuleProvider> ordered, Set<String> keep) {
        return ordered.stream().map(EngineModuleProvider::id).filter(keep::contains).toList();
    }

    @Test
    void explicitProviderContributesModuleAndStylesheet(@TempDir Path root) {
        StubContentModule marker = new StubContentModule();
        EngineModuleProvider p = provider("p", 0, Set.of(), ctx -> {
            ctx.addStaticContentModule(marker);
            ctx.addStylesheet("file:/x/provider.css");
        });

        BootstrapOptions options = BootstrapOptions.discovering(
                root, ApplicationResourceConfig.defaults(), List.of(p));

        assertTrue(options.staticContentModules().contains(marker),
                "discovered provider's static content module should be in the assembled options");
        assertTrue(GlobalStylesheets.all().contains("file:/x/provider.css"),
                "provider stylesheet should be registered globally");
    }

    @Test
    void failingProviderIsIsolatedAndOthersStillContribute(@TempDir Path root) {
        StubContentModule good = new StubContentModule();
        EngineModuleProvider boom = provider("boom", -1, Set.of(), ctx -> {
            throw new IllegalStateException("provider blew up");
        });
        EngineModuleProvider ok = provider("ok", 0, Set.of(), ctx -> ctx.addStaticContentModule(good));

        BootstrapOptions options = BootstrapOptions.discovering(
                root, ApplicationResourceConfig.defaults(), List.of(boom, ok));

        assertTrue(options.staticContentModules().contains(good),
                "a throwing provider must not abort discovery of the others");
    }

    @Test
    void orderDedupsByIdExplicitWinsAndSortsByPriority() {
        EngineModuleProvider a10 = provider("a", 10, Set.of(), c -> { });
        EngineModuleProvider a0 = provider("a", 0, Set.of(), c -> { });   // same id — must be dropped
        EngineModuleProvider b = provider("b", -5, Set.of(), c -> { });

        List<EngineModuleProvider> ordered = BootstrapOptions.orderProviders(List.of(a10, a0, b));
        assertEquals(List.of("b", "a"), idsOfInterest(ordered, Set.of("a", "b")),
                "b (priority -5) before a (priority 10); the second 'a' is de-duplicated");

        // The retained 'a' is the first occurrence (explicit-wins), i.e. priority 10.
        EngineModuleProvider retainedA = ordered.stream().filter(p -> p.id().equals("a")).findFirst().orElseThrow();
        assertEquals(10, retainedA.priority());
    }

    @Test
    void dependsOnOverridesPriorityOrder() {
        // 'dep' has lower priority (would sort first) but declares it depends on 'base'.
        EngineModuleProvider base = provider("base", 0, Set.of(), c -> { });
        EngineModuleProvider dep = provider("dep", -100, Set.of("base"), c -> { });

        List<EngineModuleProvider> ordered = BootstrapOptions.orderProviders(List.of(base, dep));
        assertEquals(List.of("base", "dep"), idsOfInterest(ordered, Set.of("base", "dep")),
                "base must contribute before dep despite dep's lower priority");
    }

    @Test
    void unknownDependencyIsIgnored() {
        EngineModuleProvider only = provider("only", 0, Set.of("does-not-exist"), c -> { });
        List<EngineModuleProvider> ordered = BootstrapOptions.orderProviders(List.of(only));
        assertFalse(idsOfInterest(ordered, Set.of("only")).isEmpty(),
                "a dependsOn id that isn't present must be ignored, not drop the provider");
    }
}
