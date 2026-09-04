# ramscript-tools

Toolkit for building, composing, installing and validating custom Pokémon FireRed/LeafGreen Wonder Card RamScripts for vanilla/original-cartridge-compatible workflows.

## Start here

The project documentation is intentionally optimized for fast human/LLM recovery. Read:

1. `docs/CURRENT_ARCHITECTURE.md` — hard boundaries and frozen production architecture.
2. `docs/PRESETS.md` — current feature catalog and validation overview.
3. `docs/HISTORY.md` — what was built and the important lessons behind current decisions.
4. `docs/ROADMAP.md` — future work and explicitly frozen/deferred research.
5. `docs/TESTING.md` — validation policy.
6. `docs/AUTHORING_AND_UI.md` — future editor/composer direction.

`docs/reference/`, `docs/history/`, `docs/research/` and `docs/legacy/` are supporting/provenance material and do not override the canonical files above.

## Current production baseline

- `HotkeyRuntimeV1` — frozen single-hotkey runtime.
- `SharedHotkeyRuntime` — frozen production runtime for 2..8 composable bindings.
- `MultiHotkeyRuntimeV1` — legacy/validated/superseded; preserved under `src/legacy/validated/`, never selected for new builds.
- persistent SB1 gateway -> SB2 payload architecture and shared native staging.
- reusable existing-object RamScript bindings.
- Trade Evolution V1 dedicated/exclusive deployment.
- Custom Trainer Runtime V2 + Compact Trainer Transport V1.
- eight Kanto Gym Leader rematch presets.
- InstallationManifest + Toolkit Cleaner.
- special Oak Lab Aide early-RNG deployment; Shared `Seed + SID + Party IV` is game-validated on LG1.0.
- `mute-music` production hotkey preset for FR1.0/FR1.1/LG1.0/LG1.1 (`R+DOWN`), with immediate mute/restore GAME-VALIDATED on real LG1.0 cartridge both standalone and in Shared `Seed + SID + Party IV + Mute`, and source/profile + build support on the other English versions; no extra resident IWRAM.
  - Shared deployment requires a 4-byte-aligned SB2 Field Script base because its current native installer uses `CPU_SET_BLOCK`; this constraint is enforced by the planner and regression-tested.

ROM modification is outside the project boundary.

## Experimental Run Anywhere V1 toggle probe

This branch also contains the standalone `RunAnywhereHotkeyRuntimeV1` experiment. The previously tested one-way build is GAME-VALIDATED on real LeafGreen 1.0 for installation, indoor running and persistence across map transitions (including after Brock). The ON/OFF build in this package is a new experimental refinement and still requires hardware validation.

- hotkey: `R+SELECT`
- ON/OFF feedback: sound effect `0x66` (click)
- fixed EWRAM reservation: `0x02022B08..0x02022B4B` (68 bytes validated by the earlier hardware probe)
- resident bytes used by this build: 54 bytes
- toggle helper: 64-byte one-shot native staged at `gStringVar4+0x140` through the existing `CPU_SET_BLOCK` helper installer
- OFF restores the current map's captured stock `allowRunning` bit instead of blindly clearing it
- session-only: reset/reboot removes both Runtime V1 and the EWRAM sidecar

Build a WC3 with:

```text
java -cp out Main legacy build-run-anywhere-hotkey-v1-wc3 lg10 input.wc3 output.wc3
```

## Build and production test

Compile recursively; do not use `src/*.java` as the whole-project validation command.

PowerShell:

```powershell
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
New-Item -ItemType Directory out | Out-Null
$sources = Get-ChildItem src,tests -Recurse -Filter *.java
javac -encoding UTF-8 -d out $sources.FullName
java -cp out ProductionTestRunner
```

The historical `TestRunner` is retained as a broader regression/provenance suite and may contain known stale assertions. `ProductionTestRunner` is the focused production gate.

## Project layout

```text
src/                 active production builders/runtimes/presets
src/deployment/      composition/deployment policy
src/persistence/     persistent placement/gateway primitives
src/legacy/          superseded or rejected implementations
src/probes/          retained executable probes
src/research/        executable research fixtures
tests/               production and historical regression suites
docs/                canonical docs + reference/history/research/legacy material
```

The current development rule is simple: build new presets/content above the validated infrastructure; do not reopen frozen runtimes without a demonstrated blocking feature.

## CLI quick start

The normal `Main` namespace exposes only current production commands. Start with:

```text
java -cp out Main commands
java -cp out Main presets
```

Canonical preset builds use `build-preset-wc3`; object-hosted builds use `build-preset-object-wc3`; cleanup uses `build-toolkit-cleaner-wc3`. Historical and research entrypoints are intentionally hidden from normal discovery but remain reproducible with `java -cp out Main legacy <old-command> ...`.
