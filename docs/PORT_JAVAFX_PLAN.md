# Portable JavaFX Plan Extract

This document extracts the reusable JavaFX engine guidance from the referenced JavaFX port planning material into `novlfx-engine`. It is intentionally limited to cross-project foundation code and omits application-specific story content, assets, route names, migration metadata, and game rules from Lr2Alt or any other source project : https://github.com/earlb710/Lr2Alt.git

## Portability filter

Include code in this engine only when it is useful to multiple visual novel or JavaFX game ports:

- lifecycle orchestration and startup validation
- reusable static content registries and module registration contracts
- route descriptors, route modules, and scene navigation primitives
- generic UI shells, placeholder screens, theme helpers, and startup error reporting
- save/load, preferences, random, time, and game-state abstractions
- audio request and channel abstractions
- reusable scene-flow definitions, validation, execution, choice modeling, and resumable flow snapshots
- image display definitions, layered character definitions, transforms, interpolation, and animation playback
- text tokenization and style parsing that is independent of authored dialogue
- utility classes for validation, immutable collections, paths, time formatting, JSON string escaping, and initialization guards

Exclude code that belongs in an application repository:

- application entry points and launcher wiring
- project-specific screen content, story scripts, characters, locations, assets, or save schemas
- extracted source scripts, generated metadata, or one-off migration helpers
- hard-coded project names, labels, routes, or asset paths that are not defaults for reusable tests
- bespoke gameplay rules that are not represented as reusable interfaces or extension points
- source-engine-specific names or references in code identifiers, string literals, or comments

## Reusable startup sequence

The portable lifecycle is organized as explicit bootstrap phases so ports do not depend on script import order or global side effects:

1. **Core services**: construct foundational services such as preferences, save/load, audio, random, route support, and content registries.
2. **Static content registries**: register reusable defaults and application-provided content modules before mutable runtime state exists.
3. **Game rules**: validate required definitions, display bindings, and extension-provided rules before UI routes can consume them.
4. **UI routes and controllers**: register route metadata, route factories, global API adapters, and screen-navigation entry points.
5. **Runtime state**: create per-save state only after static data and routes have been validated.

The engine should keep these phases deterministic, explicit, and testable. Application repositories can supply content and route modules, but the engine owns the phase ordering and validation surface.

## Reusable module responsibilities

### `bootstrap`

Provide the startup phase model, boot context, bootstrap service, and reporting objects. Bootstrap code should coordinate reusable services without depending on any authored game content.

### `content`

Provide registries and content-module interfaces for static definitions loaded during startup. Content registries should expose immutable views and validate required IDs before runtime state or UI routes use them.

### `routing` and `globalApi`

Provide route descriptors, route modules, route factories, route contexts, and adapters for global navigation-style actions. Route definitions should describe reusable metadata and delegate screen construction to application or test modules.

### `scene`

Provide the reusable section 1.3 dialogue and scene-scripting foundation. Scene code should model stable scene IDs, typed dialogue/narration/action/choice/transition steps, menu-choice requirements and effects, explicit jump/call/return/complete transitions, resumable flow snapshots, scene modules, registry validation, and UI-neutral view models. Application repositories should provide authored dialogue, character IDs, plotline scenes, domain-specific conditions, and game-specific effects through modules rather than embedding them in the engine.

### `ui`

Provide generic screens, shell/navigation helpers, theme support, startup error reporting, and manual/test UI surfaces. UI classes should avoid game-specific text beyond reusable defaults or test placeholders.

### `display`

Provide image asset definitions, display layers, transforms, interpolation, animation steps, animation playback, and registries for display bindings. Display code should model generic composition and playback behavior rather than project-specific characters or assets.

### `audio`

Provide sound requests, playback commands, channel definitions, and audio service behavior that can be reused by any JavaFX game. Application repositories should provide concrete audio files and channel policies when they are game-specific.

### `gamesupport`, `state`, `save`, `prefs`, and `random`

Provide reusable abstractions for actions, requirements, effects, generic definition registries, generic code tables, location descriptors, location occupancy, configurable game clock/date-time, mutable game state creation, save/load workflows, preferences, and deterministic random behavior. Keep domain-specific rules outside the engine unless represented as generic extension points.

### `text` and `util`

Provide text tag parsing, token/style models, validation helpers, immutable collection helpers, path helpers, time formatting, JSON string escaping, and initialization guards. These packages should remain independent of any source game's script syntax beyond generic visual novel text needs.

## Extraction guidance

When porting code from an application into `novlfx-engine`:

1. Identify the reusable seam first: service contract, registry model, route abstraction, display primitive, parser, or utility.
2. Remove source-project names, labels, IDs, authored content, asset paths, and migration-only assumptions.
3. Replace concrete game data with constructor parameters, records, interfaces, module registration, or test fixtures.
4. Preserve validation and deterministic ordering so failures appear during bootstrap instead of during gameplay.
5. Add focused tests for the reusable behavior and keep application-specific coverage in the application repository.
6. Update this document when a new reusable package or extraction rule is added.

## Current reusable scope in `novlfx-engine`

The engine currently publishes the `com.novlfx.engine` Java module with reusable `com.eb.javafx.*` packages for audio, bootstrap, content, display, game support, preferences, random, global API adapters, routing, save/load, scene flow, state, text, UI, and utilities.

Validation for this repository should continue to use the checked-in Gradle wrapper:

```bash
./gradlew --no-daemon build
```
