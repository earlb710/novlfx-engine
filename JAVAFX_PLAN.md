# JavaFX Engine Remaining Plan

This local plan tracks reusable `novlfx-engine` work that remains after the current generic JSON-loader and contract extraction slice. Keep app-authored story content, LR2Alt IDs, game rules, and concrete media assets in consuming application repositories.

## Still to do

- [ ] Expand JSON loading beyond simple display and scene definitions only when reusable schemas are stable.
- [ ] Add application-side examples that resolve JSON paths through `ApplicationResourceConfig`.
- [ ] Add richer scene validation for unreachable branches, scene graph summaries, and speaker/display reference checks supplied by app registries.
- [ ] Connect `SceneFlowStateJson` to future save/load snapshot contracts without making the engine own app save schemas.
- [ ] Implement app-owned JavaFX media adapters against `AudioPlaybackAdapter`.
- [x] Replace placeholder screens with reusable view-model driven screens while keeping app-specific controls in app modules.
- [ ] Add integration tests around options-based bootstrap loading JSON-backed content modules.
- [ ] Keep `docs/PORT_JAVAFX_PLAN.md` updated whenever reusable package scope changes.
