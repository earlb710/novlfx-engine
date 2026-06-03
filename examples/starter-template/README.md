# Starter template — a minimal novlfx game

The smallest complete game you can build on the engine: it boots, shows the engine's main-menu
shell, and is wired through the extension-discovery SPI so you grow it by adding modules — never by
editing engine code.

## Layout

```
starter-template/
├── build.gradle                         # application + JavaFX + the novlfx-engine dependency
├── config.json                          # app config (here: just the window title)
└── src/main/
    ├── java/
    │   ├── module-info.java             # requires com.novlfx.engine + provides the SPI provider
    │   └── com/example/starter/
    │       ├── StarterGame.java         # JavaFX Application: discover → boot → open route → show
    │       └── StarterModuleProvider.java   # contributes this game's content
    └── resources/META-INF/services/
        └── com.eb.javafx.bootstrap.EngineModuleProvider   # class-path discovery (mirrors `provides`)
```

## What each piece does

- **`StarterGame`** — four lines of real work: `BootstrapOptions.discovering(config, provider)` →
  `new BootstrapService(options).boot(stage)` → `sceneRouter().open(startupRoute())` →
  `stage.setScene(...)` / `show()`.
- **`StarterModuleProvider`** — implements `EngineModuleProvider`; its `contribute(ModuleContext)`
  registers a display name and the startup route. The engine's built-in defaults supply the shell
  routes (main menu, preferences, save/load), so this is all a minimal game needs.
- **`module-info.java`** — `requires com.novlfx.engine` and `provides EngineModuleProvider with
  StarterModuleProvider` so the engine discovers it on the module path.
- **`META-INF/services/...`** — the class-path equivalent of the `provides` clause; keep both so the
  game runs either way.
- **`config.json`** — optional engine configuration; see `docs/USER_MANUAL.md` §13 for every knob.

## Build & run

```bash
# point the build at the engine first (see build.gradle: JitPack, composite, or a published coord)
./gradlew run
```

You should get a window titled **Starter Game** showing the engine's main-menu shell.

## Grow it

| Add… | How |
|---|---|
| Scenes / dialogue | author JSON and contribute `new JsonSceneModule(url)` in the provider, or declare them in `config/app-load.json` |
| Routes / screens | `context.addRouteModule(new MyRoutes())` |
| Bundled fonts / CSS | `context.fonts().registerFromModule(...)` / `context.addStylesheet(...)` |
| Theme / window / audio | `config.json` keys (USER_MANUAL §13) |

See **`../integration-patterns/`** for explicit-wiring, resource-contributing, and JSON-content
variants, and **`docs/USER_MANUAL.md` §14** for the full SPI / provider contract.
