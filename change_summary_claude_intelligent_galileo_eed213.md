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
