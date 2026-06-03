# novlfx-engine examples

Three families of examples, from "copy this to start" to per-feature reference.

| Folder | What it is | Start here if… |
|---|---|---|
| [`starter-template/`](starter-template/README.md) | A complete minimal game — copy the directory, rename, build, run. SPI-based (one provider, discovered via `ServiceLoader`, booted through `BootstrapOptions.discovering(...)`). | You're starting a new project. |
| [`integration-patterns/`](integration-patterns/README.md) | Focused source sketches of different ways to contribute content: explicit wiring (no SPI), JSON-authored content, and a resource-shipping provider. | You know the basics and want the right shape for your content. |
| [`user-manual/`](user-manual/README.md) | Per-section demos mirroring `docs/USER_MANUAL.md`, each runnable from the test screen or `javac`/`java`. | You're learning a specific subsystem (audio, scenes, save, display, …). |

Authored sample JSON (config, scenes, display, conversations, screens, …) lives under
[`resources/json/`](resources/json).

## Recommended path

1. Skim [`starter-template/`](starter-template/README.md) to see the whole boot in four lines.
2. Pick the matching shape from [`integration-patterns/`](integration-patterns/README.md) for how
   your content is authored (code vs. JSON) and whether you ship bundled assets.
3. Reach into [`user-manual/`](user-manual/README.md) for the subsystem you're wiring next.

Reference docs: `docs/USER_MANUAL.md` (esp. §13 config knobs, §14 the provider SPI),
`docs/SPI_PLAN.md` (SPI design), `docs/MODDING_SETUP.md` (setup-only modding),
`docs/API_STABILITY.md` (what's safe to depend on).
