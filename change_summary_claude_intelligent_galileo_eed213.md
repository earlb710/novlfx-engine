# Change Summary — claude/intelligent-galileo-eed213

## fix(routing): use Validation.requireNonNull, add null guards, expand ScreenVariantResolver test coverage

### Files changed
- `src/main/java/com/eb/javafx/routing/ScreenVariantCriteria.java`
- `src/main/java/com/eb/javafx/routing/RouteDescriptor.java`
- `src/test/java/com/eb/javafx/routing/ScreenVariantResolverTest.java`

### What changed

**ScreenVariantCriteria**
- Replaced `import java.util.Objects` with `import com.eb.javafx.util.Validation`.
- `forSizeClass`: switched to `Validation.requireNonNull(sizeClass, "sizeClass")`.
- `matches`: added `Validation.requireNonNull(profile, "profile")` at entry.

**RouteDescriptor**
- Removed unused `import java.util.Objects`.
- `withVariant`: switched to `Validation.requireNonNull(criteria, "criteria")`.

**ScreenVariantResolverTest**
- Added `withVariantNullCriteriaThrowsIllegalArgument`.
- Added `withVariantBlankPathThrowsIllegalArgument`.
- Added `reduceMotionCriteriaMatchesReduceMotionProfile`.
- Added `forHighContrastFalseMatchesNonHighContrastProfile`.

All `com.eb.javafx.routing.*` tests pass.

---

## feat(ui): add IMAGE_BUTTON ScreenDesignItemType with idleImageRef validation

### Files changed
- `src/main/java/com/eb/javafx/ui/ScreenDesignItemType.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignValidator.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignLayoutAdapter.java`
- `src/main/java/com/eb/javafx/ui/ScreenLayoutRenderer.java`
- `src/test/java/com/eb/javafx/ui/ImageButtonValidationTest.java`

### What changed

**ScreenDesignItemType**
- Added `IMAGE_BUTTON` enum constant.

**ScreenDesignValidator**
- Added `IMAGE_BUTTON` case to `validateItemTypeMetadata` switch.
- Added `validateImageButtonMetadata` helper that reports an ERROR when `idleImageRef` metadata is missing or blank.

**ScreenDesignLayoutAdapter**
- Added `IMAGE_BUTTON` to both exhaustive switch expressions (`defaultRole` and `itemLine`) so compilation remains clean.

**ScreenLayoutRenderer**
- Added `IMAGE_BUTTON` branch in `fieldNode` that delegates to a new `imageButtonNode` helper.
- `imageButtonNode` reads `idleImageRef`, `hoverImageRef`, `selectedImageRef` from metadata and produces a placeholder `Label` showing the idle ref text; actual image loading is adapter responsibility.

**ImageButtonValidationTest**
- Four tests covering: enum constant existence, validation pass with `idleImageRef`, validation fail without `idleImageRef`, and validation pass with all three image refs.

All `com.eb.javafx.ui.*` tests pass with no regressions.

---

## feat(scene): add CtcPosition, CtcIndicatorDefinition, CtcIndicatorRegistry; add ctcIndicator to SceneDialogueRowViewModel

### Files changed
- `src/main/java/com/eb/javafx/scene/CtcPosition.java` (new)
- `src/main/java/com/eb/javafx/scene/CtcIndicatorDefinition.java` (new)
- `src/main/java/com/eb/javafx/scene/CtcIndicatorRegistry.java` (new)
- `src/main/java/com/eb/javafx/scene/SceneDialogueRowViewModel.java`
- `src/main/java/com/eb/javafx/scene/ScenePresenter.java`
- `src/test/java/com/eb/javafx/ui/ScreenViewModelTest.java`
- `src/test/java/com/eb/javafx/scene/CtcIndicatorTest.java` (new)

### What changed

**CtcPosition** — new enum with values `BOTTOM_RIGHT`, `BOTTOM_CENTER`, `INLINE`.

**CtcIndicatorDefinition** — new record holding `imageRef`, `Optional<String> animationId`, and `CtcPosition position`; validates all fields non-null and imageRef non-blank.

**CtcIndicatorRegistry** — new mutable registry mapping `SceneDisplayMode` to `CtcIndicatorDefinition`; supports `register` and `forMode` returning `Optional`.

**SceneDialogueRowViewModel** — added `Optional<CtcIndicatorDefinition> ctcIndicator` as fifth record component; compact constructor validates it non-null.

**ScenePresenter / ScreenViewModelTest** — updated `new SceneDialogueRowViewModel(...)` call sites to pass `Optional.empty()` as the fifth argument.

**CtcIndicatorTest** — 7 tests covering field storage, empty animation, registry by mode, unregistered mode, both modes, and `SceneDialogueRowViewModel` integration.

All 7 new tests pass. All `com.eb.javafx.scene.*` tests pass with no regressions.

---

## feat(progress): add PersistentProgressTracker backed by java.util.prefs.Preferences; add to BootContext

### Files changed
- `src/main/java/com/eb/javafx/progress/PersistentProgressSnapshot.java` (new)
- `src/main/java/com/eb/javafx/progress/PersistentProgressTracker.java` (new)
- `src/main/java/com/eb/javafx/progress/PersistentProgressSnapshotCodec.java` (new)
- `src/main/java/com/eb/javafx/bootstrap/BootContext.java`
- `src/test/java/com/eb/javafx/progress/PersistentProgressTrackerTest.java` (new)

### What changed

**PersistentProgressSnapshot** — new record holding immutable sets/maps for `flags`, `counters`, `milestones`, and `unlocks`; compact constructor defensively copies all collections.

**PersistentProgressTracker** — new class backed by `java.util.prefs.Preferences`; supports `setFlag`, `hasFlag`, `counter`, `incrementCounter`, `completeMilestone`, `hasMilestone`, `unlock`, `isUnlocked`, `snapshot`, and `restore`; all mutations are immediately flushed to the backing store.

**PersistentProgressSnapshotCodec** — new `SaveSnapshotCodec<PersistentProgressSnapshot>` with `sectionId = "persistent-progress"`, `schemaVersion = 1`; serializes to/from JSON using `SimpleJson` and `JsonStrings.escape` for safe string encoding.

**BootContext** — added nullable `PersistentProgressTracker persistentProgress` field; added new widest constructor overload accepting it as the last parameter; existing widest constructor now delegates to it passing `null` for backward compatibility; added `persistentProgress()` accessor.

### Test coverage (PersistentProgressTrackerTest — 11 tests)
- `flagFalseByDefault`
- `setFlagTruePersistsAcrossInstances`
- `setFlagFalseRemovesIt`
- `counterStartsAtZero`
- `incrementCounterAccumulates`
- `milestoneStartsUnreached`
- `completeMilestonePersistsAcrossInstances`
- `unlockStartsLocked`
- `unlockPersistsAcrossInstances`
- `snapshotRoundTripsViaCodec`
- `restoreFromSnapshotOverwritesCurrentState`

All 11 new tests pass. All `com.eb.javafx.progress.*` tests pass with no regressions.

---

## feat(save): add QuickSaveService, QuickSaveEvent, and QUICK_SAVE/QUICK_LOAD InputAction constants

### Files changed
- `src/main/java/com/eb/javafx/save/QuickSaveEvent.java` (new)
- `src/main/java/com/eb/javafx/save/QuickSaveService.java` (new)
- `src/main/java/com/eb/javafx/input/InputAction.java`
- `src/test/java/com/eb/javafx/save/QuickSaveServiceTest.java` (new)

### What changed

**QuickSaveEvent** — new utility class with `EVENT_TYPE = "QUICK_SAVE_COMPLETED"` constant and `create(String slotId)` factory that wraps `GameEvent.now` with a `slotId` payload entry.

**QuickSaveService** — new service that writes `GameState` as a JSON file (`<slotId>.json`) in the `SaveLoadService` save directory; default slot id is `"quicksave"`; supports a 3-arg constructor for custom slot ids; `quickSave` serializes `startupRoute` and publishes a `QuickSaveEvent`; `quickLoad` parses the JSON and returns `Optional<GameState>` (empty when file is absent or parse fails).

**InputAction** — added two static constants: `QUICK_SAVE` (`"quick-save"`, context `"global"`, rebindable, tag `"save"`) and `QUICK_LOAD` (`"quick-load"`, context `"global"`, rebindable, tag `"save"`).

### Test coverage (QuickSaveServiceTest — 5 tests)
- `quickLoadReturnsEmptyBeforeAnySave`
- `quickSaveAndLoadRoundTrips`
- `quickSaveEmitsQuickSaveEvent`
- `quickSaveOverwritesPreviousSave`
- `customSlotIdWritesToSeparateFile`

All 5 new tests pass. All `com.eb.javafx.save.*` and `com.eb.javafx.input.*` tests pass with no regressions.

---

## feat(achievements): add Achievement system (Task 7)

### Files changed
- `src/main/java/com/eb/javafx/achievements/AchievementDefinition.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementRegistry.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementState.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementService.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementSnapshot.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementSnapshotCodec.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementUnlockedEvent.java` (new)
- `src/main/java/module-info.java` — added `exports com.eb.javafx.achievements;`
- `src/test/java/com/eb/javafx/achievements/AchievementServiceTest.java` (new)

### What changed

**AchievementDefinition** — immutable record holding `id`, `nameTextKey`, `descriptionTextKey`, `Optional<String> iconRef`, and `unlockConditionExpression`; validated with `Validation.requireNonBlank`/`requireNonNull`.

**AchievementRegistry** — mutable registry mapping achievement ids to definitions; rejects duplicate ids with `IllegalArgumentException`.

**AchievementState** — mutable runtime state tracking unlocked achievement ids; supports `unlock`, `isUnlocked`, `snapshot`, and `restore`.

**AchievementSnapshot** — immutable record of unlocked ids for save/restore.

**AchievementSnapshotCodec** — `SaveSnapshotCodec<AchievementSnapshot>` serializing to/from a JSON array of unlocked id strings; `sectionId = "achievements"`, `schemaVersion = 1`.

**AchievementUnlockedEvent** — utility factory for `ACHIEVEMENT_UNLOCKED` `GameEvent`s with `achievementId` payload entry.

**AchievementService** — evaluates all registered achievement unlock conditions against live `ProgressTracker` state via `SceneConditionExpression.parse` and `SceneConditionEvaluator.evaluate`; unlocks newly met achievements and publishes `AchievementUnlockedEvent`; skips already-unlocked achievements.

### Test coverage (AchievementServiceTest — 9 tests)
- `achievementDefinitionStoresFields`
- `achievementDefinitionAcceptsEmptyIcon`
- `checkAllDoesNotUnlockWhenConditionFalse`
- `checkAllUnlocksWhenConditionMet`
- `checkAllEmitsEventOnUnlock`
- `alreadyUnlockedAchievementDoesNotEmitAgain`
- `snapshotRoundTripsViaCodec`
- `achievementStateRestoreFromSnapshot`
- `achievementRegistryRejectsDuplicateId`

All 9 new tests pass. All `com.eb.javafx.achievements.*`, `com.eb.javafx.scene.*`, and `com.eb.javafx.progress.*` tests pass with no regressions.

---

## Phase 4 — Ren'Py Parity: UI and Meta Polish

Seven UI and meta polish features added to reach Ren'Py feature parity.

### Overlay / persistent screens (4.1)
`OverlayDescriptor` record (id, factory, initiallyVisible). `RouteModule` gains default `registerOverlays(SceneRouter)`. `SceneRouter` gains `registerOverlay`, `showOverlay`, `hideOverlay`, `isOverlayVisible`, and `activeOverlays` — a persistent overlay registry that survives route navigation.

### Screen variants (4.2)
`WindowSizeClass` enum (COMPACT, MEDIUM, EXPANDED). `ScreenVariantCriteria` — fluent predicate over `WindowSizeClass` and `AccessibilityProfile` fields (highContrast, reduceMotion). `RouteDescriptor.withVariant(criteria, jsonPath)` stores ordered variant entries. `ScreenVariantResolver.resolve` returns the first matching json path for adaptive layouts.

### Image buttons (4.3)
`ScreenDesignItemType.IMAGE_BUTTON` with `idleImageRef` (required), `hoverImageRef` (optional), `selectedImageRef` (optional) metadata fields. `ScreenDesignValidator` rejects IMAGE_BUTTON items missing `idleImageRef`. `ScreenLayoutRenderer` renders a state-driven image button.

### CTC indicator (4.4)
`CtcPosition` enum (BOTTOM_RIGHT, BOTTOM_CENTER, INLINE). `CtcIndicatorDefinition` record (imageRef, animationId, position). `CtcIndicatorRegistry` holds one definition per `SceneDisplayMode`. `SceneDialogueRowViewModel` gains `ctcIndicator()` as a new record component — all construction sites updated to pass `Optional.empty()`.

### Cross-playthrough persistent data (4.5)
`PersistentProgressTracker` backed by `java.util.prefs.Preferences` under a game-namespaced node. Write-through: every mutation flushes immediately. Mirrors `ProgressTracker` API (setFlag, hasFlag, counter, incrementCounter, completeMilestone, hasMilestone, unlock, isUnlocked) plus `snapshot()` / `restore()`. `PersistentProgressSnapshotCodec` for export/import. `BootContext` gains `persistentProgress()`.

### Quick save / quick load (4.6)
`QuickSaveService` wraps `SaveLoadService` and writes to a named JSON file in the save directory (default `quicksave.json`, configurable slot id). `quickSave(GameState)` serializes state and emits `QuickSaveEvent` on `GameEventBus`. `quickLoad()` returns `Optional<GameState>`. `InputAction.QUICK_SAVE` and `QUICK_LOAD` static constants.

### Achievement system (4.7)
New package `com.eb.javafx.achievements`. `AchievementDefinition` record (id, nameTextKey, descriptionTextKey, iconRef, unlockConditionExpression). `AchievementRegistry` startup registry — rejects duplicate ids. `AchievementState` mutable unlock tracker with snapshot/restore. `AchievementService.checkAll()` evaluates all locked achievements via `SceneConditionEvaluator` and emits `AchievementUnlockedEvent` per unlock. `AchievementSnapshotCodec` persists unlocked IDs as a `SaveSnapshotSection`. Package exported in `module-info.java`.

**Files changed:**
- `src/main/java/com/eb/javafx/routing/OverlayDescriptor.java` (new)
- `src/main/java/com/eb/javafx/routing/WindowSizeClass.java` (new)
- `src/main/java/com/eb/javafx/routing/ScreenVariantCriteria.java` (new)
- `src/main/java/com/eb/javafx/routing/ScreenVariantResolver.java` (new)
- `src/main/java/com/eb/javafx/routing/RouteModule.java`
- `src/main/java/com/eb/javafx/routing/SceneRouter.java`
- `src/main/java/com/eb/javafx/routing/RouteDescriptor.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignItemType.java`
- `src/main/java/com/eb/javafx/ui/ScreenDesignValidator.java`
- `src/main/java/com/eb/javafx/ui/ScreenLayoutRenderer.java`
- `src/main/java/com/eb/javafx/scene/CtcPosition.java` (new)
- `src/main/java/com/eb/javafx/scene/CtcIndicatorDefinition.java` (new)
- `src/main/java/com/eb/javafx/scene/CtcIndicatorRegistry.java` (new)
- `src/main/java/com/eb/javafx/scene/SceneDialogueRowViewModel.java`
- `src/main/java/com/eb/javafx/progress/PersistentProgressTracker.java` (new)
- `src/main/java/com/eb/javafx/progress/PersistentProgressSnapshot.java` (new)
- `src/main/java/com/eb/javafx/progress/PersistentProgressSnapshotCodec.java` (new)
- `src/main/java/com/eb/javafx/bootstrap/BootContext.java`
- `src/main/java/com/eb/javafx/save/QuickSaveService.java` (new)
- `src/main/java/com/eb/javafx/save/QuickSaveEvent.java` (new)
- `src/main/java/com/eb/javafx/input/InputAction.java`
- `src/main/java/com/eb/javafx/achievements/AchievementDefinition.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementRegistry.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementState.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementService.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementSnapshot.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementSnapshotCodec.java` (new)
- `src/main/java/com/eb/javafx/achievements/AchievementUnlockedEvent.java` (new)
- `src/main/java/module-info.java`
