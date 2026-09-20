# Presets — current catalog and validation

This is the canonical human/LLM overview of production presets. Detailed implementation notes live under `reference/features/`; exact historical experiments remain outside the active documentation set.

A **preset** is a ready-to-build feature, not a synonym for hotkey compatibility. The QoL/viewer presets can use Deliveryman or hotkey activation; Trade Evolution is also a preset but uses its own dedicated/exclusive runtime and does not participate in Shared hotkey composition. Trainer battles belong to the separate **Battle Events** family, and future quests/world modifications belong to **Events**.

Canonical CLI examples:

```text
java -cp out Main build-preset-wc3 fr10 deliveryman input.wc3 output.wc3 party-iv-viewer
java -cp out Main build-preset-wc3 fr10 hotkey input.wc3 output.wc3 party-iv-viewer
java -cp out Main build-preset-wc3 fr10 hotkey input.wc3 output-prefix seed-modifier show-secret-id party-iv-viewer --seed 1234
```

The last command selects three hotkey presets, so the builder emits Shared installation stages automatically. The user does not select V1 versus Shared.

Validation labels are deliberately conservative: **GAME-VALIDATED** means the exact recorded path was exercised; **BUILD-TESTED** means code/profile generation exists but that exact path is not recorded as an in-game test.

## Hotkey/QoL presets

The established hotkey-capable features expose the standard execution families at the API/metadata level. The BOX 14 Seed variant is intentionally hotkey-only for now: standalone V1 plus Shared persistent-native.

```text
functional preset payload
├── Deliveryman direct
├── HotkeyRuntimeV1 (single hotkey)
└── SharedHotkeyRuntime (2..8 hotkeys)
```

This is trigger generalization, not a requirement to combine every preset. The functional payload remains the feature; Deliveryman/V1/Shared are execution choices around it.

| Preset | Deliveryman | Hotkey V1 | Shared / Hotkey N | Notes |
|---|---|---|---|---|
| Seed Modifier | SUPPORTED / NOT YET RECORDED IN-GAME | GAME-VALIDATED FR1.0 baseline; GAME-VALIDATED LG1.0 normalized RNG path | GAME-VALIDATED FR1.0 baseline; LG1.0 pre-normalization full-u32 Shared confirmed with `B5B1E7AD` | `--seed` is full u32; production payload runs `CloseLink`, clears `gWirelessCommType`, then prompts; 81 B feature payload |
| Seed Modifier (BOX 14) | — | **GAME-VALIDATED LG1.0; SUPPORTED FR1.0/FR1.1/LG1.1** | **GAME-VALIDATED LG1.0 with Party IV; SUPPORTED FR1.0/FR1.1/LG1.1** | Reads exactly 8 uppercase hex chars from BOX 14; 72 B native helper; standalone 677/995 B on LG1.0 baseline; Shared uses existing native staging service |
| Repel | SUPPORTED / NOT YET RECORDED IN-GAME | GAME-VALIDATED FR1.0 metadata/history | GAME-VALIDATED FR1.0 | Pure Field Script; current V1 has no resident watcher/EWRAM owner; modern-style expiry prompt remains future work |
| Mute Music | — | **GAME-VALIDATED LG1.0; SUPPORTED FR1.0/FR1.1/LG1.1** | **GAME-VALIDATED FR1.0/LG1.0; SUPPORTED FR1.1/LG1.1** | `R+DOWN`; all four English FR/LG profiles; ON uses `gDisableMusic=1` + BGM `VolumeControl(0)`; the Field Script then restarts logical map music with `playbgm MUS_DUMMY` + `Overworld_PlaySpecialMapMusic` (ON stays silent via the flag; OFF resumes immediately); helper is temporary EWRAM only |
| Show Secret ID | GAME-VALIDATED FR1.0 | SUPPORTED / NOT YET RECORDED IN-GAME | GAME-VALIDATED FR1.0/LG1.0 histories; **Shared-local fallback GAME-VALIDATED FR1.0** | Persistent-native and relocation-safe Shared-local are distinct deployment paths |
| Party IV Viewer | GAME-VALIDATED FR1.0 | GAME-VALIDATED FR1.0 | GAME-VALIDATED FR1.0/LG1.0 histories | Hybrid/native |
| Party EV Viewer | GAME-VALIDATED FR1.0 | IMPLEMENTED; exact path remains conservative | GAME-VALIDATED FR1.0 | Hybrid/native |
| Lead IV Viewer | GAME-VALIDATED FR1.0 | IMPLEMENTED; exact path remains conservative | IMPLEMENTED; exact path remains conservative | Hybrid/native |
| Lead EV Viewer | GAME-VALIDATED FR1.0 | IMPLEMENTED; exact path remains conservative | IMPLEMENTED; exact path remains conservative | Hybrid/native |

Generalized paths are promoted independently only after exact in-game confirmation. Seed Deliveryman, Repel Deliveryman, and SID HotkeyRuntimeV1 remain supported/build-tested until exercised in-game. Full-u32 fixed Seed via Shared is separately confirmed on LG1.0. BOX 14 Seed Shared is GAME-VALIDATED on LG1.0 in the `BOX14 + Party IV` composition; larger Shared packs remain BUILD-TESTED until exercised in-game. Mute Music is GAME-VALIDATED on LG1.0 standalone and Shared, and on FR1.0 Shared in the corrected `Mute + SID(local) + Party EV + Repel` composition; FR1.1/LG1.1 and FR1.0 standalone remain supported/build-tested. Show SID Shared-local is GAME-VALIDATED on that same FR1.0 composition. Seed Shared-local is GAME-VALIDATED on FR1.0 in `Seed(local) + Lead IV + Party EV + Run + Bike` with `B5B1E7AD`; Repel Shared-local is GAME-VALIDATED on FR1.0 in `Repel(local) + Party IV + Party EV`. Mute Shared-local remains BUILD-TESTED only.

### Early starter-RNG pack

Special-purpose LG1.0 flow hosted by Professor Oak's Lab Aide 1:

```text
R+SELECT -> Seed Modifier
R+START  -> Show Secret ID
R+A      -> Party IV Viewer
R+DOWN   -> Mute Music
```

**GAME-VALIDATED on LG1.0.** The final Shared runtime is installed through a self-detaching object-bound wrapper; the frozen Shared resident blocks are not rewritten.

Known behavior: after activation the final RamScript becomes `33 FF FF FF`. If that state is saved, the stock Mystery Gift system exposes the Deliveryman. This is accepted for the intended starter-RNG loop, which uses soft reset without saving after runtime activation.

The Party IV viewer final-page change (`waitMessage -> waitButtonPressStrict -> releaseAll`) is build-tested and intended to keep the last textbox open until A/B; it is not a runtime change.

## Dedicated/exclusive presets

### Trade Evolution V1

Production-complete dedicated preset. It opens the party selector, resolves stock trade-evolution targets, handles National Dex restrictions, runs the evolution scene and resumes safely. It remains **exclusive/dedicated**, rather than being forced into Shared composition.

- Deliveryman: GAME-VALIDATED FR1.0.
- Existing-object binding: GAME-VALIDATED FR1.0 using Lavender Town Worker M.
- FR1.1/LG1.0/LG1.1: symbol/profile supported, exact in-game validation remains separate.

## Battle Events

### Custom Trainer Runtime V2 + Compact Trainer Transport V1

Frozen production trainer architecture. Supports authored trainer identity, party/battle data, dialogue, completion state, stock battle lifecycle and reusable persistent storage. Brock and Misty plus boundary/sequence cases established the compact transport production baseline on FR1.0. Do not reopen runtime-size experiments without a blocking authoring requirement.

### Gym Leader Rematches V1

Eight Kanto Gym Leader rematch presets built on the Custom Trainer production runtime:

```text
Brock, Misty, Lt. Surge, Erika, Koga, Sabrina, Blaine, Giovanni
```

All eight are implemented/build-tested. Exact in-game validation remains recorded per preset rather than inferred globally. Giovanni's Five Island flow is part of the validated production history.

## Toolkit Cleaner

Production uninstall/cleanup infrastructure backed by the InstallationManifest. Default cleanup removes toolkit-owned infrastructure while preserving authored progress. `WIPE_PROGRESS` is explicit and applies only to toolkit-owned progress flags/catalog entries.

## Composition policy

Current automatic runtime choice:

```text
0 hotkeys -> no hotkey runtime
1 hotkey  -> HotkeyRuntimeV1
2..8      -> SharedHotkeyRuntime
```

`MultiHotkeyRuntimeV1` is archived and never selected for new builds.

A composition being supported does **not** imply every preset should be packed together. Shared composition is a capability, not a product requirement. The planner must still enforce RamScript/SB1/SB2/alignment/runtime constraints.

## Detailed references

- `reference/features/` — per-preset behavior and historical sizing/validation notes.
- `reference/internals/PRESET_VALIDATION_MATRIX.md` — older conservative matrix retained as detailed metadata provenance.
- `reference/internals/PRESET_CATALOG.md` — metadata/cost model.


## Run Anywhere / Run + Bike Anywhere

| Preset | Deliveryman | Hotkey V1 | Shared / Hotkey N | Notes |
|---|---|---|---|---|
| Run Anywhere | — | **GAME-VALIDATED LG1.0; SUPPORTED FR1.0/FR1.1/LG1.1** | **GAME-VALIDATED LG1.0; SUPPORTED FR1.0/FR1.1/LG1.1** | `R+RIGHT`, click `0x66`; 54 B fixed-EWRAM sidecar; running only |
| Run + Bike Anywhere | — | **BUILD-TESTED all four English revisions** | **GAME-VALIDATED LG1.0; SUPPORTED FR1.0/FR1.1/LG1.1** | `R+RIGHT`, click `0x66`; 63 B fixed-EWRAM sidecar; restores stock bike+run permissions on OFF |

Both are session-only and mutually exclusive because they reserve `0x02022B08..0x02022B4B`.

The planner chooses Run+Bike placement from the whole composition. In the **GAME-VALIDATED LG1.0** `seed-modifier-box14 + repel + party-iv-viewer + run-bike-anywhere` pack, it reused the existing Shared native staging service and selected `SHARED_PERSISTENT_NATIVE` for Run+Bike automatically.

See `reference/features/RUN_AND_BIKE_ANYWHERE.md`.
