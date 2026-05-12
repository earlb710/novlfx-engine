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
