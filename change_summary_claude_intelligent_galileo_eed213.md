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

---

## Task: Code quality fixes for overlay support (review follow-up)

### Modified files
- `src/main/java/com/eb/javafx/routing/SceneRouter.java`
  - `registerRouteModule` now calls both `routeModule.registerRoutes(this)` and `routeModule.registerOverlays(this)` — fixes dead-code hook.
  - `registerOverlay` uses `Validation.requireNonNull` instead of `Objects.requireNonNull`; removed `java.util.Objects` import, added `com.eb.javafx.util.Validation` import.
  - `isOverlayVisible` uses direct unboxed `overlayVisibility.get(overlayId)` instead of `Boolean.TRUE.equals(...)`.
- `src/main/java/com/eb/javafx/routing/RouteModule.java` — removed redundant inline comment from `registerOverlays` body.
- `src/test/java/com/eb/javafx/routing/OverlayDescriptorTest.java`
  - Changed exception type in `overlayDescriptorRejectsNullId` from `Exception.class` to `IllegalArgumentException.class`.
  - Added `overlayDescriptorRejectsBlankId` — verifies blank id `"  "` throws `IllegalArgumentException`.
  - Added `overlayDescriptorRejectsNullFactory` — verifies null factory throws `IllegalArgumentException`.
- `src/test/java/com/eb/javafx/routing/SceneRouterOverlayTest.java`
  - Added `registerRouteModuleWiresOverlays` — verifies that a `RouteModule` implementing `registerOverlays` correctly registers overlays when passed to `registerRouteModule`.

### Test results
- All 11 tests pass (6 OverlayDescriptorTest + 5 SceneRouterOverlayTest + 1 new wiring test = 12 total across both files).
