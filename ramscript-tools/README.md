# ramscript-tools

Toolkit for building, composing, installing and validating custom FireRed/LeafGreen Wonder Card RamScripts.

The project has moved beyond isolated RamScript experiments. Its current focus is a conservative production baseline for reusable presets, persistent SaveBlock-backed payloads, object-bound events, hotkeys, Trade Evolution, Custom Trainer battles and future UI-driven event authoring.

## Current production baseline

The following architecture is considered frozen unless a real feature is blocked by a demonstrated limitation:

- `HotkeyRuntimeV1` / `MultiHotkeyRuntimeV1` keep their validated resident layouts.
- persistent Field Scripts use the validated SaveBlock1 gateway -> SaveBlock2 payload architecture;
- shared native modules use the validated persistent staging/catalog infrastructure;
- object-bound RamScripts use `ObjectEventTarget` + `ObjectEventRamScriptBinding` and relocation-aware continuation runtimes;
- Trade Evolution is a dedicated/exclusive production preset;
- Custom Trainer uses Runtime V2 + Compact Trainer Transport V1;
- Gym Leader authoring is deliberately limited to eight fixed Kanto rematch presets in V1;
- `InstallationManifest` + `ToolkitCleanerPreset` own and clean the reserved toolkit storage safely.

Do not replace these baselines with older probes/rejected implementations merely to save bytes.

## Production features

### General toolkit

- Wonder Card / RamScript builders and inspection helpers
- Field Script writer and Gen III text codec
- FR/LG ROM profiles
- persistent SB1/SB2 storage and relocation handling
- installation manifest and toolkit cleaner
- object-event bindings
- standalone and shared hotkey runtimes
- composition/capacity planning
- preset metadata, validation status and deployment policies

### Presets and mechanics

- Seed Modifier
- Repel hotkey
- Secret ID viewer
- Party IV / EV viewers
- compact Lead IV / EV viewers
- Trade Evolution with Party selector and stock evolution scene
- Custom Trainer Runtime V2
- eight Kanto Gym Leader rematch presets

See `docs/PRESET-CAPABILITY-MATRIX.md`, `docs/TRADE_EVOLUTION.md` and `docs/GYM_LEADER_REMATCH_PRESETS.md` for exact support and validation status.

## Build and test

```powershell
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
New-Item -ItemType Directory out | Out-Null
$sources = Get-ChildItem src,tests -Recurse -Filter *.java
javac -encoding UTF-8 -d out $sources.FullName
java -cp out ProductionTestRunner
java -cp out TestRunner
```

Expected status for this revision:

- `ProductionTestRunner`: passes.
- `TestRunner`: still stops at the known historical Party-IV AUTO-planner assertion: `Party-IV-only AUTO plan should preserve standalone HotkeyRuntimeV1 when cheaper`.

The historical aggregate-suite failure is unrelated to the current production Custom Trainer / Gym Leader path and must not be reported as green.

## Gym Leader Rematches V1

Install the shared Custom Trainer runtime once:

```powershell
java -cp out Main build-custom-trainer-runtime-wc3 fr10 input.wc3 custom-trainer-runtime.wc3
```

Then build one of the fixed presets:

```text
build-custom-trainer-brock-wc3
build-custom-trainer-misty-wc3
build-custom-trainer-lt-surge-wc3
build-custom-trainer-erika-wc3
build-custom-trainer-koga-wc3
build-custom-trainer-sabrina-wc3
build-custom-trainer-blaine-wc3
build-custom-trainer-giovanni-wc3
```

Giovanni uses the Five Island Fisherman as the object host. The stock `!` movement plays before the fade; the loaded object is changed to Giovanni during the fade; his battle uses the vanilla `BOSS GIOVANNI` identity and `MUS_VS_CHAMPION`.

### FR1.0 validation snapshot

- Brock, Misty and Lt. Surge: exact current presets emulator-tested.
- Giovanni: Five Island host, exclamation, fade, live sprite swap, dialogue and battle start emulator-tested.
- Erika, Koga, Sabrina and Blaine: Java/build-tested through the same validated production pipeline, but not individually emulator-tested.
- FR1.1/LG1.0/LG1.1: profile/build-supported where documented; exact current Gym presets are not claimed as in-game validated there.

This means Gym Leader V1 is production-usable on the validated FR1.0 pipeline, but the validation matrix remains intentionally precise rather than treating every profile/preset combination as game-tested.

## Trade Evolution

Trade Evolution V1 is mechanically complete on FireRed 1.0 for its validated deployment modes. It includes Party selection/cancel, `GetEvolutionTargetSpecies`, National Dex behavior, the stock evolution scene, SaveBlock relocation recovery, Deliveryman execution and existing-NPC object binding.

It remains `EXCLUSIVE` by policy because its validated wrapper/runtime ownership conflicts with shared composition infrastructure. See `docs/TRADE_EVOLUTION.md`.

## Cleaner and uninstall path

Build the cleaner with:

```powershell
java -cp out Main build-toolkit-cleaner-wc3 fr10 input.wc3 toolkit-cleaner.wc3
```

The cleaner validates the toolkit manifest before clearing the reserved SB1/SB2 regions and toolkit completion flags. Automated checks cover the implementation.

One manual closeout regression is still intentionally tracked:

```text
Cleaner -> normal save -> restart -> reinstall runtime -> reinstall trainer -> verify fresh storage/flags
```

## UI / composer foundation

The core is already structured so a future UI does not need to know raw offsets or rebuild the runtime architecture.

Existing reusable layers include:

- `PresetDefinition` / `PresetCatalog` — user-facing preset metadata;
- `PresetDeploymentCost` / `PresetInfrastructure` — capacity and shared-cost modeling;
- `PresetCompositionPlanner` / `CompositionInstallationPlanner` — placement and compatibility decisions;
- `PresetSelectionPolicy` — including exclusive presets such as Trade Evolution;
- `RomProfile` — version-specific symbols/addresses;
- `ObjectEventCatalog` / `ObjectEventTarget` — named event hosts instead of raw map ids;
- `InstallationPlan` / `InstallationManifest` — deterministic install ownership;
- `CustomTrainerAuthoringReport` and compact transport validation — authoring/capacity feedback;
- `RamScriptBuilder` / `FieldScriptWriter` — low-level generation beneath high-level recipes.

The intended UI split remains:

```text
high-level preset/editor
        -> validated configuration
        -> planner / capacity report
        -> event / Wonder Card generation
        -> selected installation transport
```

The UI should expose friendly concepts such as preset, ROM version, object host, hotkey, dialogue, party and capacity/conflicts; raw SaveBlock offsets and runtime addresses should remain internal.

See `docs/COMPOSER_DESIGN.md`, `docs/PRESET_CATALOG.md`, `docs/AUTOMATIC_COMPOSITION_PLANNER.md` and `docs/ROADMAP.md`.

## Project layout

- `src/` — active builders, runtimes, presets, ROM profiles and CLI.
- `src/deployment/` — deployment/composition policy.
- `src/persistence/` — persistent placement/gateway primitives.
- `src/legacy/` — compatibility/rejected historical implementations still referenced by aggregate tests/CLI.
- `src/probes/` and `src/research/` — retained executable research fixtures; not production baselines.
- `tests/ProductionTestRunner.java` — focused production gate.
- `tests/TestRunner.java` — historical aggregate regression runner.
- `docs/` — architecture, validation, feature and roadmap documentation.

## What is frozen vs what is next

Frozen unless a demonstrated feature requires change:

- HotkeyRuntimeV1 layouts
- persistent SB1 -> SB2 gateway architecture
- Custom Trainer Runtime V2
- Compact Trainer Transport V1
- Gym Leader V1 battle runtime
- Trade Evolution V1 runtime architecture
- Run Anywhere persistent shared-resident approach (rejected/frozen)

Next work is primarily **new content/mechanics on top of the baseline**, not another runtime rewrite. See `docs/ROADMAP.md`.
