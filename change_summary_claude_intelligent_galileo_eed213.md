# Change Summary — claude/intelligent-galileo-eed213

## Task: OverlayDescriptor and SceneRouter overlay support (Spec 4.1)

### New files
- `src/main/java/com/eb/javafx/routing/OverlayDescriptor.java` — immutable record describing a persistent overlay screen (id, factory, initiallyVisible). Validated with `Validation.requireNonBlank` and `Validation.requireNonNull`.
- `src/test/java/com/eb/javafx/routing/OverlayDescriptorTest.java` — 3 tests covering field storage, null-id rejection, and initial visibility.
- `src/test/java/com/eb/javafx/routing/SceneRouterOverlayTest.java` — 5 tests covering register/show/hide, activeOverlays filtering, and unknown-id exception throwing.

### Modified files
- `src/main/java/com/eb/javafx/routing/RouteModule.java` — added default `registerOverlays(SceneRouter)` no-op method; interface remains a valid `@FunctionalInterface`.
- `src/main/java/com/eb/javafx/routing/SceneRouter.java` — added `overlayDescriptors` and `overlayVisibility` maps; added `registerOverlay`, `showOverlay`, `hideOverlay`, `isOverlayVisible`, `activeOverlays`, and `requireRegisteredOverlay` methods. Unknown overlay ids throw `IllegalArgumentException`.

### Test results
- 8 new tests pass (3 OverlayDescriptorTest + 5 SceneRouterOverlayTest).
- No routing regressions.
