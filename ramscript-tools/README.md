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

ROM modification is outside the project boundary.

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
