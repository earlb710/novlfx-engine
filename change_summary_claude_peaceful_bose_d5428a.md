# Phase 2 Display Completeness — Change Summary

Branch: `claude/peaceful-bose-d5428a`

## Group 1 — Layered Image Composition (committed earlier)

- `LayeredImageVariant`, `LayeredImageLayer`, `LayeredImageDefinition`: immutable model for multi-layer images
- `LayeredImageRegistry`: standard register/find/require registry
- `LayeredCompositionEntry`, `LayeredImageComposition`: resolved composition result
- `LayeredImageResolver`: evaluates conditions via `Predicate<String>` to build a `LayeredImageComposition`

## Group 2 — Zorder Support (committed earlier)

- `DisplayTransform`: added `zorder` field, 7-arg constructor, `zorder()` accessor, `withZorder()` copy method (6-arg constructor still valid)
- `DisplayTagDefinition`: added `zorder` field, 5-arg constructor, `zorder()` accessor, `withZorder()` copy method

## Group 3 — Hotspot Map System

- `HotspotDefinition` (`scene`): named clickable region with fractional bounds, optional condition, target scene
- `HotspotMapDefinition` (`scene`): background image + list of hotspots
- `HotspotMapRegistry` (`scene`): register/find/require registry for maps
- `HotspotOptionViewModel` (`scene`): evaluated hotspot option for UI rendering
- `HotspotMapViewModel` (`scene`): evaluated map (background + options) for UI rendering
- `SceneStepType`: added `HOTSPOT_MAP`
- `SceneExecutionStatus`: added `WAITING_FOR_HOTSPOT`
- `SceneStep`: added `hotspotMap(id, mapId)` factory and `hotspotMapId()` metadata accessor
- `SceneExecutionResult`: added `Optional<HotspotMapViewModel> hotspotMapViewModel()`
- `SceneExecutor`: added `HotspotMapRegistry` field (4-arg constructor), `HOTSPOT_MAP` case in advance loops, `selectHotspot()` method, `buildHotspotMapViewModel()` helper

## Group 4 — ATL Animation Completeness

- `AnimationBlockType` enum: `IDLE`, `TALKING`, `EVENT`
- `AnimationEventTrigger` enum: `SHOW`, `HIDE`, `CLICK`
- `AnimationBlock`: groups a `DisplayAnimation` with its block type and optional event trigger
- `DisplayAnimation`: added `trigger` field, `trigger()` accessor, `withTrigger()` copy method, `forTrigger()` static filter

## Group 5 — Talking Animations

- `TalkingAnimationCue` (`scene`): value type pairing a speaker ID with their talking animation ID
- `CharacterTemplate`: added `talkingAnimationId()` and `idleAnimationId()` accessors reading from `metadata`
- `SceneExecutionResult`: added `Optional<TalkingAnimationCue> talkingCue()`
- `SceneExecutor`: added `CharacterTemplateRegistry` field (4-arg constructor), `buildTalkingCue()` helper, populates `talkingCue` on `DIALOGUE` steps
