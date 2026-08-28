# Current milestone — Custom Trainer production baseline

Custom Trainer Runtime V2 is now the production baseline on FireRed English 1.0. The real Brock and Misty Gym NPC bindings have been validated both before Hall of Fame (exact stock-script fallback) and postgame, and Misty can be installed after Brock while reusing the persisted runtime. Historical trainer probes were moved under `src/research/customtrainer`, `tests/research/customtrainer` and `docs/research/customtrainer`.

Use `java -cp out ProductionTestRunner` for the focused production suite. The historical all-in-one `TestRunner` still contains an older unrelated Party-IV planner assertion and is retained for regression/research coverage.

Current Custom Trainer documentation: `docs/CUSTOM_TRAINER_PRODUCTION.md`. Size work must follow `docs/CUSTOM_TRAINER_SIZE_AUDIT.md`; the validated runtime itself is frozen.

# Current milestone — Trade Evolution production preset

Trade Evolution has moved from the Build 43–48 research series into the production preset/catalog layer. The current deployment is `DEDICATED_LOCAL`: it uses the validated 32-byte relocation continuation slot, does not install the shared hotkey/VBlank runtime, and exposes `selectionPolicy=EXCLUSIVE` for the UI/planner. All ROM-sensitive symbols are resolved through `RomProfile` for FR1.0, FR1.1, LG1.0 and LG1.1. The production preset is validated in-game on FR1.0; the other three profiles remain supported but untested. See `docs/TRADE_EVOLUTION.md`.

Production command:

```powershell
java -cp out Main build-trade-evolution-wc3 fr10 input.wc3 output.wc3
```

# Build 37 note

Build 37 adds an experimental Run Anywhere maintenance path that reuses the existing shared-runtime VBlank tail and stores its 24-byte maintenance code in RamScript/EWRAM. It claims no additional IWRAM block. See `docs/builds/BUILD-37-NOTES.md`.

# RamScript Tools

Build 35 adds a first-class preset validation matrix and moves historical build
notes under `docs/builds/`. Runtime/installers remain byte-identical in behavior
to the validated Build 34 baseline; this build is metadata/documentation only.
Use `java -cp out Main preset-validation <rom>` to inspect the current
preset × usage-mode × ROM validation state.

Example:

```text
java -cp out Main plan-installation fr10 seed-modifier repel party-iv-viewer show-secret-id
```

# ramscript-tools

Build, inspect, compose, import and export FireRed/LeafGreen Wonder Card RamScripts.

## Production architecture

Build 28 consolidates the validated Field Script and shared persistent-native paths as the production baseline:

- simple/local presets may remain directly in RamScript;
- persistent hotkey Field Scripts use the validated deferred `MultiHotkeyRuntimeV1`;
- each persistent entry uses a 10-byte SaveBlock1 gateway (`setvaddress + vgoto`);
- the preset body lives in the validated 1024-byte SaveBlock2 region;
- hotkeys are ignored while another Field Script/dialogue owns field controls.

See `docs/ARCHITECTURE_BASELINE.md` and `docs/SHARED_PERSISTENT_NATIVE.md`. Rejected Build 11–14 implementations are preserved under `src/legacy/rejected/`, not deleted.

The historical `native-ramscript-runtime-v1-multipart` repository remains a research/reference implementation and is not a runtime dependency.

## Build

```powershell
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
New-Item -ItemType Directory out
$sources = Get-ChildItem src,tests -Recurse -Filter *.java
javac -encoding UTF-8 -d out $sources.FullName
java -cp out TestRunner
```

## Trigger smoke test

```powershell
java -cp out Main build-trigger-test-wc3 hotkey fr10 input.wc3 output.wc3
```

For `fr10` with the Hello test payload, the production build is byte-identical to the validated Candidate 5a.


## Custom payloads

User-created Field Scripts may execute directly through the deliveryman or be wrapped in `HotkeyRuntimeV1` for R+SELECT. See `docs/CUSTOM_PAYLOADS.md`.


## Hybrid preset example

`ShowSecretIdPreset` demonstrates a deliveryman Field Script that stages and calls a small native helper, then returns to stock Field Script for number formatting and UI. See `docs/SHOW_SECRET_ID.md`.

`NativeHelperInstaller` supports both direct byte-by-byte `setptr` staging and a compact BIOS CpuSet block-copy path, with `AUTO` selecting the smaller encoding. See `docs/NATIVE_HELPER_INSTALLER.md`.

`PartyIvViewerPreset` uses the same installer abstraction and displays the current party Pokemon nickname plus its IVs across two pages. See `docs/PARTY_IV_VIEWER.md`.

## Configurable hotkeys

Advanced hotkey presets default to `R + SELECT`, but may append `--hotkey <held>-<pressed>`.
For example, `--hotkey r-b` means hold R and press B. Supported names are `a`, `b`, `select`, `start`, `right`, `left`, `up`, `down`, `r`, and `l`.

## Repel hotkey

`RepelHotkeyPreset` provides a lightweight configurable-hotkey shortcut that uses the longest-duration Repel currently available and does not stack while another Repel is active. The modern-style automatic `Use another?` behavior remains a future V2 idea. See `docs/REPEL_HOTKEY.md`.


## Shared multi-hotkey combo

The first two-hotkey shared-runtime build combines Seed Modifier and Repel Hotkey without duplicating the full runtime:

```text
R + SELECT -> Seed Modifier
R + B      -> Repel Hotkey
```

Build example:

```powershell
java -cp out Main build-seed-repel-combo-wc3 fr10 1234 ../dummy-saves/custom-design.wc3 ../dummy-saves/seed-repel-combo.wc3 --seed-hotkey r-select --repel-hotkey r-b
```

This compact path is validated in-game on FireRed English 1.0. It intentionally preserves a restricted two-binding encoding rather than replacing the proven runtime with the failed generic-dispatch experiments. See `docs/MULTI_HOTKEY.md`.


## Persistent storage research

Save-layout research is tracked separately in `docs/PERSISTENCE_RESEARCH.md`.
The first strong candidates are unused/filler bytes already inside the normal
FR/LG save structures, including a 400-byte explicitly-unused region in
`SaveBlock1` and a larger 1024-byte filler region in `SaveBlock2`. They are
candidates only until a write -> normal save -> reset/reload -> verify probe is
validated in-game.

## Build 6: PersistentToolkitStorage V2

V2 replaces the Build 4/V1 fixed single-payload layout with a module table. The first runtime experiment installs two independent Thumb proof modules in the validated SaveBlock2 storage area and lets a separate launcher Wonder Card select one by module ID.

Commands:

    java -cp out Main build-persistent-storage-v2-install-wc3 fr10 ../dummy-saves/custom-design.wc3 ../dummy-saves/persistent-v2-install.wc3
    java -cp out Main build-persistent-storage-v2-launch-wc3 fr10 1 ../dummy-saves/custom-design.wc3 ../dummy-saves/persistent-v2-launch-1.wc3
    java -cp out Main build-persistent-storage-v2-launch-wc3 fr10 2 ../dummy-saves/custom-design.wc3 ../dummy-saves/persistent-v2-launch-2.wc3

Expected runtime result after installing, saving, resetting, and replacing the WC:

- launcher 1: `Persistent module 1 executed!`
- launcher 2: `Persistent module 2 executed!`

This is still a proof format. Both V2 proof modules live in SaveBlock2. Cross-area module allocation (RamScript + SaveBlock1 + SaveBlock2) is the next composer-level step after the table/lookup mechanism is validated in-game.

## Build 10: real persistent presets

The cross-area persistent dispatcher now has a validation build with two real modules: Show Secret ID in SaveBlock1 and a native Seed Modifier core in SaveBlock2. This path is opt-in; the original single-preset RamScript builders remain unchanged.


## Build 13a
Hotkey payloads now establish their own virtual address before any v* opcode. Build 13 is rejected.

## Build 16: current persistent Field Script baseline

The validated persistent Field Script composition path is the Build 15a deferred multi-hotkey runtime with 10-byte SaveBlock1 gateways into SaveBlock2 payloads. Build 16 consolidates this as the baseline without changing runtime bytes. See `docs/builds/BUILD-16-NOTES.md`, `docs/ARCHITECTURE_BASELINE.md`, and `docs/PERSISTENT_HOTKEY_RUNTIME.md`.

## Build 21 status

The Build-20 persistent-native smoke mechanism has been consolidated into generic reusable catalog/bridge primitives. See `docs/builds/BUILD-21-NOTES.md`. The next validation target is a second real hybrid/native preset, preferably Party IV Viewer.

## Build 22 laboratory Party IV shared-native test

Build 22 adds a second real persistent-native preset to the shared architecture: Party IV Viewer. It reuses the existing 296-byte validated helper as an SB2 native module and routes R+A through a deferred Field Script bridge. See `docs/builds/BUILD-22-NOTES.md`.

## Current validated shared composition (Build 24/25)

FireRed English 1.0 in-game validation now includes one shared runtime with `R+SELECT` Seed Modifier, `R+B` Repel and `R+A` Party IV Viewer. Party IV uses a persistent SB2 native body, an aligned persistent bridge, EWRAM staging and stock `callnative`. Build 25 reorganizes that code without changing generated bytes. See `docs/builds/BUILD-25-NOTES.md` and `docs/PERSISTENT_NATIVE_MODULES.md`.

## Build 26 candidate

Build 25 remains the consolidated known-good baseline. Build 26 adds an
experimental shared persistent-native staging service: the expensive generic
native loader transport is paid once in RamScript rather than repeated inside
each SB2 native bridge. See `docs/builds/BUILD-26-NOTES.md`.

## Build 27 candidate — two native presets, one loader

Build 26 was validated in-game. Build 27 adds Show Secret ID as a second real
persistent-native module beside Party IV Viewer. `R+A` (Party IV) and `R+START`
(SID) both reuse one `SharedPersistentNativeStagingService`; `R+B` Repel and
`R+SELECT` Seed remain pure Field Script and do not use the native loader. See
`docs/builds/BUILD-27-NOTES.md`.

## Build 27a note

The dual-native SID message uses `waitButtonPressStrict()` (`waitbuttonpress; nop`) to avoid FR/LG Quest Log's special end-of-script input shortcut from consuming the held hotkey modifier before the player can read the message. See `docs/builds/BUILD-27A-NOTES.md`.

## Build 27B note

When re-running `setvaddress` from a non-zero Field Script position, embed the virtual address of the current opcode (`virtualBase + offset`), not the script-start virtual base. Build 27A violated this while restoring relocation after a shared-service `vcall`; the SID `vmessage` consequently resolved to its own 0xFF terminator and displayed a blank box. `setVAddressHere()` now formalizes the correct rule.


## Build 28: current shared persistent-native baseline

Build 27B was validated in-game with four shared bindings: `R+SELECT` Seed, `R+B` Repel, `R+A` Party IV and `R+START` SID. Party IV and SID reuse one `SharedPersistentNativeStagingService`. Build 28 only consolidates production names/documentation and preserves the generated Build 27B artifacts byte-for-byte.

Preferred production command names for the validated reference composition:

```powershell
java -cp out Main build-shared-native-install-a-wc3 fr10 1234 ../dummy-saves/custom-design.wc3 ../dummy-saves/shared-native-a.wc3
java -cp out Main build-shared-native-install-b-wc3 fr10 1234 ../dummy-saves/custom-design.wc3 ../dummy-saves/shared-native-b.wc3
java -cp out Main build-shared-native-install-c-wc3 fr10 1234 ../dummy-saves/custom-design.wc3 ../dummy-saves/shared-native-c.wc3
java -cp out Main build-shared-native-runtime-wc3 fr10 1234 ../dummy-saves/custom-design.wc3 ../dummy-saves/shared-native-runtime.wc3
```

The historical `build-shared-dual-native-smoke-*` names remain accepted as compatibility aliases.


## Build 29: preset metadata catalog

Build 29 adds planner-facing `PresetDefinition` / `PresetCatalog` metadata for Seed Modifier, Repel, Party IV Viewer and Show Secret ID. It separates per-preset byte costs from shared infrastructure requirements and preserves simpler deployment paths.

Inspect metadata for a ROM profile:

```powershell
java -cp out Main preset-metadata fr10
```

See `docs/builds/BUILD-29-NOTES.md` and `docs/PRESET_CATALOG.md`. The next phase is the automatic composition/placement planner.

## Build 30 planning layer

`PresetCompositionPlanner` can now choose deployment modes and report memory/shared-infrastructure costs without changing runtime bytes. Use `plan-presets <rom> <preset-id>...` for a dry-run plan. See `docs/AUTOMATIC_COMPOSITION_PLANNER.md`.


## Build 31 planning milestone

`plan-presets` now reports both deployment/memory decisions and concrete default hotkeys + SB1/SB2 offsets. This remains dry-run planning; installation/WC generation from the plan is the next milestone.

## Build 33 — Automatic installation emission

`InstallationPlan` can now be emitted as real WC3 RamScripts using
`InstallationEmitter` / `CompositionArtifactBuilder`.

Example:

```text
java -cp out Main build-planned-installation-wc3 fr10 1234 input.wc3 output-prefix seed-modifier repel party-iv-viewer show-secret-id
```

For the validated four-preset layout, this currently generates:

- `output-prefix-install-1.wc3`
- `output-prefix-install-2.wc3`
- `output-prefix-runtime.wc3`

The persistent bytes and runtime are equivalent to the Build-27b/28 baseline;
the new two-installer grouping still requires in-game validation.


## Build 34 lifecycle support

Planner-generated persistent installations now carry a 32-byte `RSPK` manifest at `SB2+0x0F00`.
Use `build-toolkit-cleaner-wc3` to generate a conservative deliveryman Cleaner that only erases a recognized
manifest-tagged toolkit installation and refuses to run while the resident hotkey runtime is active.

See `docs/INSTALLATION_MANIFEST_AND_CLEANER.md` and `docs/PRESET_VALIDATION_MATRIX.md`.

## Build 36 — Run Anywhere current-map probe

Research command:

```powershell
java -cp out Main build-run-anywhere-probe-wc3 fr10 ../dummy-saves/custom-design.wc3 ../dummy-saves/run-anywhere-probe.wc3
```

The probe stages a 16-byte helper in EWRAM and sets only `gMapHeader.allowRunning` for the currently loaded map. It reserves no new IWRAM. This is intentionally a causal probe before choosing the final cross-map resident design. See `docs/builds/BUILD-36-NOTES.md`.

## Existing NPC bindings

Object-bound RamScripts are now exposed through semantic `ObjectEventTarget`
descriptors and `RamScriptBinding`. The first catalogued target is
`lavender-town-worker-m`; use `build-trade-evolution-object-wc3` rather than
embedding map/object ids in feature code. See `docs/OBJECT_EVENT_BINDINGS.md`.

## Trade Evolution v1 status

Trade Evolution v1 is closed for FR1.0 feature work: both Deliveryman and existing-object/NPC bindings are validated in-game. The existing-NPC path is generic through `ObjectEventTarget` / `ObjectEventRamScriptBinding`; Lavender Town Worker M is the first validated catalog target. Visual trade/trade-back animation is researched but deferred, and the preset remains `EXCLUSIVE` after a documented multipreset assessment. See `docs/TRADE_EVOLUTION.md`, `docs/OBJECT_EVENT_BINDINGS.md`, `docs/TRADE_ANIMATION_RESEARCH.md`, and `docs/TRADE_EVOLUTION_MULTIPRESET_ASSESSMENT.md`.
