# Change summary

## Problem

The Preferences screen's Close button always navigated to the main menu, even
when the user opened Preferences from a mid-game scene. Players opening the
menu to tweak a setting were dumped back to the title screen instead of being
returned to where they were.

## Engine changes

### `RouteContext` navigation back-stack

- **`pushAndNavigateTo(routeId)`** — pushes the current scene root (and
  stylesheet list, and the previously-active route id) onto an internal
  back-stack, then performs the normal `navigateTo(routeId)`. Use for
  transient overlays the user expects to dismiss back to where they were
  (Preferences, save/load picker, in-game help, settings popovers...).
- **`navigateBack()`** — pops the back-stack and swaps the saved `Parent`
  back into the primary stage's existing `Scene` (same scene instance the
  normal navigate flow already reuses for its root swap). Returns `true`
  on success and `false` when the stack was empty, so callers can fall
  back deterministically (e.g. Preferences falls back to the main menu).
- **`canNavigateBack()`** / **`activeRouteId()`** — small read helpers for
  hosts that want to decide between "restore" and "fallback" before
  calling either.

Saved roots survive intact: any state held in their JavaFX node tree
(scrollbar position, scene-flow `stepHistory` captured in closures, dialog
cursor, character text inputs, ...) is restored exactly because we swap
the same node back in — we don't reconstruct the scene from a route
factory.

### `PreferencesSummaryScreen` close behaviour

All three close paths — the on-screen Close button, the footer Close icon,
and the `Ctrl+P` shortcut — now go through one `closeAction`:

```java
if (!context.navigateBack()) {
    context.navigateTo(SceneRouter.MAIN_MENU_ROUTE);
}
```

- Opened via `pushAndNavigateTo` (the recommended path) → close restores
  the caller's scene with its state intact.
- Opened via plain `navigateTo` (older callers, edge cases) → close falls
  back to the main menu, preserving the previous behaviour so nobody is
  ever stranded.

The on-screen Close button was previously hard-wired to
`ScreenNavigation.button(..., MAIN_MENU_ROUTE)`; it's now built inline so
the back-stack-aware `closeAction` reaches it. The Main Menu button
(separate from Close) keeps its confirmation dialog and still navigates
straight to `MAIN_MENU_ROUTE` — that's a deliberate "leave the game"
action, not a "close the menu" one.

### `DefaultRouteModule`

The Preferences footer test screen's "Open preferences" launcher uses
`pushAndNavigateTo` so the test fixture exercises the back-stack path
the same way real hosts will.

## Engine tests

New `RouteContextBackStackTest`:

- `pushAndNavigateRecordsCurrentRootThenNavigateBackRestoresIt` — proves
  `navigateBack` restores the **same** `Parent` instance that was current
  when `pushAndNavigateTo` ran. Same instance = preserved state.
- `navigateBackReturnsFalseAndNoOpsWhenBackStackIsEmpty` — guards the
  fallback contract; an empty stack must not mutate the scene.
- `plainNavigateToDoesNotPushSoTheBackStackStaysEmpty` — plain
  `navigateTo` is unchanged; only the explicit `pushAndNavigateTo` opt-in
  records a back-stack entry.

Existing `PreferencesSummaryScreenTest` still passes.

## AltLife changes (separate repo)

- `AltLifeFooterControls.wireFooter` switched the footer's "Preferences"
  click from `navigateTo` → `pushAndNavigateTo`, so closing Preferences
  during gameplay returns the player to the active scene rather than the
  main menu.
- `gradle.properties` comment refreshed to reflect that the local engine
  override now tracks the `claude/prefs-back-nav` branch (still the
  AltLife-side composite-build mechanism documented in earlier commits).

## Validation

- `./gradlew --no-daemon test --tests com.eb.javafx.routing.RouteContextBackStackTest` — passing.
- `./gradlew --no-daemon test --tests com.eb.javafx.ui.PreferencesSummaryScreenTest` — passing.
- AltLife composite-build compile (`-PnovlfxEngineCompositeDir=...`) — clean.

## Notes

- The back-stack lives on `RouteContext`, not `SceneRouter`, because
  `RouteContext` is where the actual `setRoot`/scene-swap logic lives and
  where the stack needs to survive across factory calls.
- The contract is opt-in: existing `navigateTo` calls behave exactly as
  before, so no surprise behaviour change for hosts that don't adopt
  `pushAndNavigateTo`.
