# API Stability

This document classifies the engine's exported packages so consumers know what they can depend on
safely. It is the authoritative companion to `module-info.java`. Classifications:

- **Stable** — exercised by a shipping game; changes follow semantic versioning (no breaking change
  without a major-version bump).
- **Experimental** — exported and usable, but not yet proven by a shipping consumer; types may
  change in a minor release. The package's `package-info.java` carries the same notice. Depend on it
  only if you can tolerate churn.
- **Internal** — not exported; developer tooling or implementation detail. Not part of the API.

> The boundary is drawn from real consumption: a package is **Stable** when the reference game
> (AltLife) actually imports it, **Experimental** when it is exported but unconsumed, and
> **Internal** when it is deliberately unexported.

## Stable

These 27 packages are imported by the reference game and form the supported surface:

`achievements`, `audio`, `bootstrap`, `characters`, `content`, `debug`, `diagnostics`, `display`,
`events`, `gallery`, `gamesupport`, `inventory`, `messages`, `organizations`, `prefs`, `progress`,
`random`, `resources`, `routing`, `save`, `scene`, `state`, `storyline`, `text`, `transitions`,
`ui`, `util`

(all under `com.eb.javafx.*`)

## Experimental

These 8 packages are exported but not yet exercised by a shipping game. Each carries an
`@apiNote`-style stability banner in its `package-info.java`:

| Package | Purpose |
|---|---|
| `accessibility` | accessibility profile / settings model |
| `assets` | asset catalogue + validation model |
| `input` | input action / binding / device / trigger mapping |
| `journal` | journal entry definitions + state |
| `localization` | localized text bundles + extraction (wired, effectively single-locale today) |
| `globalApi` | global route-action adapter / SPI hook |
| `settings` | settings definitions + store |
| `timeline` | timeline sequence player + steps |

A package is promoted **Experimental → Stable** once a shipping game uses it and its shape settles.

## Internal (not exported)

| Package | Why |
|---|---|
| `testscreen` | developer tooling / editor applications (screen designer, conversation editor, code-table management, test screens). Launched from the test runtime classpath by Gradle run tasks, so it is not a general module export. It is **qualified-exported to `com.altlife.javafx`** only (`exports … to com.altlife.javafx`) so the reference admin app can reflectively launch `ManagementApplication.launchEmbedded()` — it remains invisible to arbitrary consumers. |

## Policy

- Before publishing a release, review new packages and place each into one of the three buckets —
  default to **Experimental** for anything a consumer hasn't proven, and **Internal** for tooling.
- Widening the surface later (export a package, promote Experimental → Stable) is non-breaking.
  Narrowing it (unexport, or break an Experimental type) is expected and allowed within the rules
  above — which is the whole reason the line is drawn deliberately and early.
