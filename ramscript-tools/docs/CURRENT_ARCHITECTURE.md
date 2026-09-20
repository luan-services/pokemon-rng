# Current architecture

**Canonical production-state contract.** If another document conflicts with this file, this file wins unless a newer exact game-validation result is explicitly recorded.

## Project boundary

The toolkit targets original FireRed/LeafGreen cartridges / vanilla ROM behavior through Wonder Card/save-compatible mechanisms. **ROM modification is outside the project boundary.** Emerald/RSE behavior may be studied for ideas but is not automatically portable to FR/LG.

## Status vocabulary

- **GAME-VALIDATED** — exact recorded path exercised successfully.
- **BUILD-TESTED** — generation/tests pass; exact runtime path not exercised in-game.
- **SUPPORTED / NOT TESTED** — profile/implementation exists without exact validation.
- **FROZEN** — production mechanism; redesign only for a demonstrated blocking feature.
- **LEGACY / VALIDATED / SUPERSEDED** — known-working old implementation retained for reproduction, not selected for new builds.
- **RESEARCH / FROZEN** — useful investigation, not active production work.
- **REJECTED** — tested approach that must not return as production architecture.

## Frozen hotkey runtimes

```text
0 bindings -> no hotkey runtime
1 binding  -> HotkeyRuntimeV1
2..8       -> SharedHotkeyRuntime
```

Activation intent is explicit at the composition layer: `DELIVERYMAN` or `HOTKEY`. There is no cost-based AUTO trigger selection. `HOTKEY` chooses the runtime deterministically from the number of bindings; V1 versus Shared is an implementation detail, not a normal user-facing choice.

`HotkeyRuntimeV1` and `SharedHotkeyRuntime` are **FROZEN**. Their validated callback model is: detect/safety -> schedule Field Script -> return. Heavy feature logic never runs directly in the callback.

Critical IWRAM invariant:

```text
0x03005310..0x0300532F = 32-byte resident wrapper allocation
```

Do not grow into adjacent live IWRAM or invent replacement resident layouts without a blocking feature and new validation.

`MultiHotkeyRuntimeV1` is **LEGACY / VALIDATED / SUPERSEDED**, stored under `src/legacy/validated/` and never selected automatically.

### Shared composition is automatic, but capacity-bounded

For normal hotkey builds the user selects preset IDs; the planner selects the concrete deployment. There is no requirement to manually decide whether a payload belongs in Runtime RamScript, SB1/SB2, or the shared native catalog.

A requested combination is accepted when all selected presets are mutually compatible and the computed layout fits. Important limits remain:

- at most 8 Shared hotkey bindings;
- one common held modifier for a Shared runtime;
- ROM/deployment support must exist for every selected preset;
- RamScript, SB1, SB2, gateway reachability and alignment limits are hard constraints;
- exclusive presets such as Trade Evolution do not compose into Shared;
- fixed/session resident resources are declared as planner-owned resources; an `EXCLUSIVE` resource cannot have two owners (`run-anywhere` and `run-bike-anywhere` both claim `mobility-ewram-sidecar`);
- redundant same-family viewers are rejected before placement (`Party IV + Lead IV`, `Party EV + Lead EV`) with `PRESET_CONFLICT`;
- a supported preset does not imply that every maximal combination will fit.

The planner is expected to choose among valid deployment alternatives based on the **whole composition**. Example: `run-bike-anywhere` can stay local when that is cheapest, or become `SHARED_PERSISTENT_NATIVE` and reuse an already-required native staging service. The LG1.0 `BOX14 Seed + Repel + Party IV + Run + Bike` composition validates that automatic choice in-game.

Resource-aware fallback also works in the opposite direction: a preset may remain persistent while that preferred layout fits, then move into final Runtime RamScript after persistent capacity is exhausted, but only if it explicitly exposes a relocation-safe `SHARED_LOCAL_FIELD_SCRIPT`. Current explicit local alternatives are Show SID, Seed Modifier, Repel and Mute Music, plus the mobility presets' existing local modes. Party/Lead IV/EV viewers intentionally remain persistent-native. The planner chooses globally among advertised alternatives; after Seed/Repel/Mute gained local forms, the old five-hotkey FR1.0 `Mute + Party IV + Repel + Seed + SID` example now prefers moving Repel local rather than forcing SID local. Show SID's local path itself is GAME-VALIDATED on FR1.0 in `Mute + SID(local) + Party EV + Repel` after fixing its temporary scratch collision with the Shared wrapper. Seed local is also GAME-VALIDATED on FR1.0 in `Seed(local) + Lead IV + Party EV + Run + Bike` with `B5B1E7AD`, and Repel local is GAME-VALIDATED on FR1.0 in `Repel(local) + Party IV + Party EV`. Mute local remains BUILD-TESTED only because no current valid catalog composition naturally selects it.

## Fixed EWRAM sidecars and resource ownership

LG1.0 now has a GAME-VALIDATED session-resident EWRAM reservation at:

```text
0x02022B08..0x02022B4B = 68 B
```

It is the audited high tail of vanilla `sFlickerArray`. A sentinel survived the Brock defeat-text overflow on real hardware, and Run Anywhere code subsequently executed from this region across map transitions and after Brock.

Current owners:
- `run-anywhere`: 54 B sidecar;
- `run-bike-anywhere`: 63 B sidecar.

They are mutually exclusive. This is now enforced by generic resource ownership rather than a Run-specific planner branch: both concrete deployments claim the EXCLUSIVE `mobility-ewram-sidecar` resource. EWRAM is volatile across reset, so the same WC3/runtime activation installs both the IWRAM hotkey runtime and the selected EWRAM sidecar for the current session. See `reference/internals/FIXED_RUNTIME_RESOURCES.md`.

The IWRAM supervisor remains the lifecycle owner. The EWRAM sidecar runs only on the post-map return to `CB1_Overworld`; it is not a new continuous VBlank runtime. See `reference/features/RUN_AND_BIKE_ANYWHERE.md`.

## Persistent toolkit storage

```text
SB1 toolkit region: 400 B
SB2 toolkit region: 1024 B
manifest:           last 32 B of SB2 toolkit region
RamScript:          transport/runtime/local payload space
IWRAM:              resident listener/scheduler only
```

Persistent Field Script path:

```text
Shared runtime -> deferred scheduler -> SB1 gateway -> SB2 Field Script
```

Native/hybrid presets add the validated shared staging/catalog layer before stock `callnative`. Placement and alignment are correctness constraints, not optional optimization.

## Object-bound semantics

Production object binding uses `ObjectEventTarget` + `ObjectEventRamScriptBinding` and named catalog targets.

Keep two semantics separate:

1. **persistent object-bound** — NPC/object remains the owner, used by features such as Trade Evolution;
2. **self-detaching installer host** — NPC only installs a hotkey runtime, then the RamScript becomes no-object `FF/FF/FF` because the frozen runtime locator requires that form.

Do not replace the second case with a plain rebind.

## Early Oak Lab deployment

Professor Oak's Lab Aide 1 (`04/03/01`) is the special pre-Pokémon-Center installer host. Oak himself is intentionally not used.

LG1.0 GAME-VALIDATED Shared early-RNG composition:

```text
R+SELECT -> Seed Modifier

Seed Modifier accepts a full u32 PRNG state. Its user-facing hex value is fixed-width 8 digits (`00001234`, `B5B1E7AD`, etc.), keeping the Field Script payload size invariant for planner/Shared placement. The production Seed payload is 81 B and normalizes the Pokémon Center RFU state by executing stock `CloseLink` and then clearing `gWirelessCommType` before opening the prompt; this exact single-hotkey normalization path is GAME-VALIDATED on LG1.0. The full-u32 Shared path was GAME-VALIDATED on LG1.0 with `B5B1E7AD` in the Seed + SID + Party IV early-RNG composition after the fixed-width layout correction.

`seed-modifier-box14` is the reusable-seed variant, now source/build supported on all four English FR/LG 1.0/1.1 profiles; LG1.0 remains the only game-validated profile. It reads exactly eight uppercase hex characters from BOX 14 through a 72-byte native helper. The fixed preset remains available. BOX14 standalone is GAME-VALIDATED, and the Shared persistent-native `BOX14 + Party IV` composition is GAME-VALIDATED without changes to the frozen Shared runtime. Returning from Party/other non-overworld interfaces while on Pokémon Center 1F can re-arm `gWirelessCommType=01`/RNG 2x; Seed activation remains self-contained and normalizes RFU again each time.
R+START  -> Show Secret ID
R+A      -> Party IV Viewer
R+DOWN   -> Mute Music
```

Only the transient installer wrapper is special; Shared resident blocks remain those of the frozen production runtime.

Known accepted limitation: activation self-detaches to `33 FF FF FF`; saving afterward makes the stock Mystery Gift system expose the Deliveryman. The intended starter-RNG workflow soft-resets without saving after activation, so this is not a reason to rewrite the runtime.

## Canonical preset build interface

`build-preset-wc3` is the canonical CLI facade for production presets. Activation is explicit, while the hotkey runtime remains an internal composition decision.

```text
build-preset-wc3 <rom> deliveryman <input.wc3> <output.wc3> <preset-id> [--seed <hex>]
build-preset-wc3 <rom> hotkey <input.wc3> <output-or-prefix> <preset-id> [preset-id ...] [--seed <hex>]
```

For `HOTKEY`, one selected preset emits `HotkeyRuntimeV1`; 2..8 selected presets emit the staged `SharedHotkeyRuntime` installation. `seed-modifier` requires `--seed`, interpreted as a full hexadecimal u32 PRNG state (`00000000..FFFFFFFF`), not only a u16 boot seed. Deliveryman accepts exactly one preset per Wonder Card. Trade Evolution is a preset with a dedicated/exclusive runtime: Deliveryman is supported, hotkey composition is not.

The old `build-planned-installation-wc3` command has been removed; its production role is fully covered by `build-preset-wc3 ... hotkey`. Object-bound/early-Oak compositions use `build-preset-object-wc3`. Older feature-specific and research commands are removed from the production `Main` namespace. They remain reproducible only through the explicit `legacy <old-command> ...` compatibility namespace, implemented by `LegacyMain`.

Planning mirrors the same activation model: `plan-preset <rom> deliveryman <preset-id>` for a direct preset, or `plan-preset <rom> hotkey <preset-id> [preset-id ...]` for one or more hotkeys. Planning reports both composition and installation for either activation; hotkey plans may include persistent installer stages. The older `plan-presets` and `plan-installation` commands were removed as redundant.

Named gift/event-script references such as Aurora Ticket and Mystic Ticket are not `PresetCatalog` presets. Their historical reconstruction helpers use `build-gift-bin` / `build-gift-wc3`, keeping event-script references separate from the production preset abstraction.


## Mute Music (FR/LG English 1.0/1.1)

`mute-music` is a production hotkey preset with default `R+DOWN`. The standalone `HotkeyRuntimeV1` path is **GAME-VALIDATED on a real LeafGreen 1.0 cartridge**.

The functional payload is intentionally split between stock music state and one temporary native helper:

```text
toggle helper:
  ON  -> gDisableMusic = 1 -> m4aMPlayVolumeControl(BGM, ALL, 0)
  OFF -> gDisableMusic = 0 -> m4aMPlayVolumeControl(BGM, ALL, 256)
then for both branches:
  -> playbgm MUS_DUMMY, false
  -> special Overworld_PlaySpecialMapMusic
```

The `VolumeControl(0)` call silences the already-playing BGM immediately. `gDisableMusic` is the persistent session-level gate that prevents subsequently requested BGM from starting. The stock Field Script restart runs after either branch. While ON, `gDisableMusic=1` keeps the re-resolved request silent. On OFF, merely restoring volume is insufficient when the current logical map music was suppressed; the same restart resets the logical map-music selection and asks the overworld engine to resolve the correct map/surf/saved BGM immediately.

The helper uses an ARMv4T-safe local call veneer: `BL -> local bx r3`. Earlier experimental Stop/VolumeControl probes used `mov lr, pc; bx r3`; the target routine executed but returned with the wrong state bit and froze the game. That experimental caller bug is closed and must not be copied into future Thumb helpers.

**IWRAM invariant:** Mute Music adds **0 bytes of resident IWRAM**. The helper and its tiny `bx r3` veneer are copied to temporary EWRAM staging on activation; the frozen `0x03005310..0x0300532F` hotkey wrapper is unchanged. In Shared compositions the self-contained Field Script/helper payload is stored in SB2 behind the normal SB1 gateway; it does not require the shared persistent-native catalog/staging service. The corrected Shared/N-hotkey path is GAME-VALIDATED on real LG1.0 hardware in `Seed + SID + Party IV + Mute`; FR1.0/FR1.1/LG1.1 Shared paths remain supported/build-tested.

## Other frozen production baselines

- **Trade Evolution V1** — dedicated/exclusive; Deliveryman + existing-object paths validated on FR1.0.
- **Custom Trainer Runtime V2 + Compact Trainer Transport V1** — frozen production trainer architecture.
- **Gym Leader Rematches V1** — eight Kanto preset layer over Custom Trainer.
- **InstallationManifest + Toolkit Cleaner** — current ownership/uninstall model; default cleanup preserves authored progress.

## Frozen research

Automatic save-only hotkey bootstrap after Continue is **RESEARCH / FROZEN**. No acceptable stock-valid save-controlled automatic execution boundary was found under the project constraints. Do not resume broad bootstrap/IWRAM scanning unless explicitly requested.

## Development rule

New work happens **above** frozen runtime infrastructure. Prefer new presets, event content, deployment wrappers and authoring improvements. Reopen a frozen runtime only when a real feature is blocked and the change can be isolated, regression-tested and revalidated in game.

## Where to look next

- `PRESETS.md` — current feature catalog/validation overview.
- `HISTORY.md` — what was built and why important decisions exist.
- `ROADMAP.md` — future plans.
- `TESTING.md` — validation policy.
- `AUTHORING_AND_UI.md` — future editor/composer direction.
- `reference/` — implementation detail, not canonical entry point.
- `history/`, `research/`, `legacy/` — provenance only.


## Trigger-generalization rule

Seed, Repel, SID, Party IV/EV and Lead IV/EV expose Deliveryman direct, `HotkeyRuntimeV1`, and `SharedHotkeyRuntime` execution families. Mute Music is intentionally hotkey-only and supports FR1.0/FR1.1/LG1.0/LG1.1; it exposes standalone `HotkeyRuntimeV1` plus Shared persistent Field Script deployment. Only the standalone LG1.0 path is game-validated. Trigger wrappers reuse canonical functional payloads/modules; support does not imply exact in-game validation.

## Desktop/UI integration boundary

The production core remains in the unnamed Java package. API v1 deliberately
avoids a repository-wide package migration through frozen/validated code.
External UI clients consume `Main api ...`, a versioned JSON protocol documented
in `docs/INTEGRATION_API.md`.

The boundary is one-way: UI sends user intent/parameters; the existing catalog,
planner, installation planner and emitter remain authoritative. The UI must not
duplicate preset compatibility, placement, capacity or stage-count rules.


## Cross-version port boundary

The formerly LG1.0-only catalog entries `seed-modifier-box14`, `run-anywhere`, and `run-bike-anywhere` now support `fr10`, `lg10`, `fr11`, and `lg11`. The four supplied pret symbol maps agree on the globals/layout these ports rely on: `gWirelessCommType=03003F3C`, `gPokemonStoragePtr=03005010`, `sFlickerArray=02022AE8` with size `0x64`, `gMapHeader=02036DFC`, and `gMain=030030F0`; revision-specific ROM callbacks continue to come from `RomProfile`. `RomProfile.wirelessCommType` is now the Seed normalization source instead of feature-local RFU literals. This is SYMBOL/LAYOUT-VERIFIED + BUILD-TESTED support, not new gameplay validation.
