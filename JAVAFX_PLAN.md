# JavaFX Engine Remaining Plan

This local plan tracks reusable `novlfx-engine` work that remains after the current generic JSON-loader and contract extraction slice. Keep app-authored story content, LR2Alt IDs, game rules, and concrete media assets in consuming application repositories.

## Still to do

- [ ] Review [`earlb710/Lr2Alt`](https://github.com/earlb710/Lr2Alt.git) for content-agnostic JavaFX helpers that should be ported into this engine.
- [ ] Expand JSON loading beyond simple display and scene definitions only when reusable schemas are stable.
- [x] Add application-side examples that resolve JSON paths through `ApplicationResourceConfig`.
- [x] Add richer scene validation for unreachable branches, scene graph summaries, and speaker/display reference checks supplied by app registries.
- [x] Connect `SceneFlowStateJson` to future save/load snapshot contracts without making the engine own app save schemas.
- [x] Implement app-owned JavaFX media adapters against `AudioPlaybackAdapter`.
- [x] Define reusable audio adapter expectations for asset discovery, fades, crossfades, and channel-specific player pools while keeping concrete media files application-owned.
- [x] Replace placeholder screens with reusable view-model driven screens while keeping app-specific controls in app modules.
- [x] Add integration tests around options-based bootstrap loading JSON-backed content modules.
- [x] Keep `docs/PORT_JAVAFX_PLAN.md` updated whenever reusable package scope changes.
