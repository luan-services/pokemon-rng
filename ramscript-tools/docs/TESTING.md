# Testing and validation

Validation is intentionally tracked separately from implementation support.

## Labels

- **GAME-VALIDATED** — exact preset + usage/deployment + ROM/version path exercised successfully.
- **BUILD-TESTED** — builders/tests/checksums/layout pass, but the exact in-game path is not recorded as exercised.
- **SUPPORTED / NOT TESTED** — symbols/profile/implementation exist without exact runtime validation.
- **LEGACY / VALIDATED / SUPERSEDED** — old mechanism worked but must not be selected for new production builds.
- **REJECTED** — failed approach retained to prevent accidental revival.

Never promote FR1.1/LG1.0/LG1.1 to GAME-VALIDATED merely because a `RomProfile` exists.

## Compile policy

Compile the source tree **recursively**. `src/*.java` is not the project-wide validation command because active/legacy/research sources may live below subdirectories.

The production regression gate must continue covering at least object bindings, early hotkey installers, Trade Evolution, Custom Trainer V2, Gym Leader presets and Cleaner.

## Runtime changes

Frozen runtimes require a higher bar than preset changes. A runtime modification needs:

1. a demonstrated blocking feature;
2. isolated byte/layout impact;
3. regression coverage;
4. exact in-game revalidation of affected paths.

Preset/bridge/message changes that do not alter frozen resident blocks should be labeled/tested at their own layer rather than triggering unnecessary runtime redesign.

## Documentation discipline

Record exact mode/profile when a new in-game test succeeds. Do not use “all supported” as a synonym for “all tested”. `PRESETS.md` is the compact current summary; detailed old matrices remain under `reference/internals/` for provenance.
