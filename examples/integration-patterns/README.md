# Integration patterns

Three ways to put content into the engine, beyond the minimal `../starter-template`. Each file is a
focused, illustrative source sketch (not compiled by the build) showing one approach.

| File | Pattern | Use when |
|---|---|---|
| `ExplicitWiringGame.java` | **Explicit wiring** — build `BootstrapOptions.fromConfig(...)` and pass module lists via `with*`; no SPI. | The host owns all content and you don't need to discover third-party provider jars. Most direct. |
| `JsonContentProvider.java` | **JSON content** — author scenes/display as JSON; contribute `JsonSceneModule` / `JsonDisplayContentModule` (or declare them in `config/app-load.json`). | Writers/designers iterate on narrative content without recompiling. |
| `ResourceContributingProvider.java` | **Resource-shipping provider** — bundle fonts / CSS / images in the provider's jar and contribute them through `ModuleContext` (`fonts()`, `addStylesheet`, `resourceRoots()`). | A provider (plugin or game) ships its own assets and must stay JPMS-clean across modules. |

## How they relate

- **`../starter-template`** is the canonical SPI approach: one `EngineModuleProvider`, discovered via
  `ServiceLoader`, booted through `BootstrapOptions.discovering(...)`. Start there.
- **Explicit wiring** is the same boot, minus discovery — you assemble the options yourself. The two
  can be mixed: `discovering(...)` accepts explicit providers *and* discovers others.
- **JSON content** and **resource-shipping** are not alternatives to the above — they're *what a
  provider's `contribute()` does*. Drop either into the starter's `StarterModuleProvider`.

## The one rule that prevents the classic JPMS trap

A provider **owns its in-jar resources**: load fonts/CSS/JSON through *its own* module
(`getClass()` / `getClass().getResource(...)`), keep bundled assets in a resource-only package or
`opens` them, and never let the engine load your jar's resource by a bare path. On-disk resources
(backgrounds, external CSS, `config.json`) are filesystem I/O and JPMS-immune.

See `docs/USER_MANUAL.md` §14 (the provider contract) and `docs/SPI_PLAN.md` for the full design.
