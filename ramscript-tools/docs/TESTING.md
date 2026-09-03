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

## Recent exact validation — Seed u32 Shared

LeafGreen English 1.0: Shared Seed Modifier + Show Secret ID + Party IV Viewer was rebuilt with `--seed B5B1E7AD` after the fixed-width Seed payload correction and confirmed working in-game. The prior failing build mixed Seed/SID/invalid textbox data because the Seed payload exceeded the planner's reserved SB2 interval. This exact path is now GAME-VALIDATED.


## LeafGreen 1.0 — Seed Modifier Pokémon Center RNG normalization

GAME-VALIDATED. Entering Pokémon Center 1F during the session changed `gWirelessCommType` (`0x03003F3C`) from `00` to `01` and changed observed RNG progression from 1 to 2 advances/frame; the state persisted after leaving. A CloseLink-only Seed build ran safely but did not change either condition. The production Seed payload now executes stock `special CloseLink` while the selector is still nonzero, then writes `00` to `gWirelessCommType` before opening the prompt. Frame-by-frame testing showed the selector become `00` and RNG return to 1 advance/frame before the textbox opened. Pressing A preserved 1x behavior and the Seed workflow remained correct. Re-entering Center 1F restored `01`/2x; invoking Seed again restored `00`/1x. Normal gameplay afterward showed no observed crash. Active link/wireless sessions are outside the supported Seed Modifier use case.


## LeafGreen 1.0 — Mute Music

**GAME-VALIDATED on real cartridge (standalone `HotkeyRuntimeV1`, default `R+DOWN`).** The final production mechanism toggles `gDisableMusic` and uses stock `m4aMPlayVolumeControl` only for immediate control of the already-playing BGM. ON (`flag=1`, BGM volume 0) silences music immediately without freezing. OFF (`flag=0`, BGM volume 256) is followed by stock Field Script `playbgm MUS_DUMMY, false` and `special Overworld_PlaySpecialMapMusic`; the correct BGM returns immediately without requiring a map change.

Earlier direct M4A probes that froze after successfully silencing the BGM were contaminated by an invalid Thumb indirect-call sequence (`mov lr, pc; bx r3`). The corrected helper uses `BL -> local bx r3`, preserving Thumb return state on ARMv4T. The corrected VolumeControl-only probe then ran without freeze; the final map-restart probe additionally fixed immediate OFF restoration. Therefore the old freezes are recorded as an experimental caller/ABI bug, not evidence that stock `m4aMPlayVolumeControl` is unsafe.

The native helper is temporary EWRAM staging only and consumes no new resident IWRAM. Shared/N-hotkey materialization is build-tested but remains **SUPPORTED_NOT_TESTED** until an exact multi-preset composition is exercised in-game. Recommended follow-up coverage includes map warps, Pokémon Center transitions, battle return, cries/fanfares, Surf/bike music and save/reset/session semantics.

## LeafGreen 1.0 — BOX 14 Seed Modifier standalone

GAME-VALIDATED. BOX 14 was named with an eight-digit uppercase hexadecimal seed and the experimental standalone R+SELECT preset applied it successfully. Frame-by-frame observation showed the seed change at the same visible textbox-close moment as the fixed Seed Modifier; no additional frame delay was observed. The 72-byte helper validates every character as `0-9` or `A-F`; invalid input returns without writing `gRngValue`.

The Shared persistent-native integration is also GAME-VALIDATED on LG1.0 in the `BOX14 + Party IV` composition. The same 72-byte helper is stored in the SB2 native catalog, staged by the existing shared service before the prompt, and entered through one stock `callnative` after A. Both BOX14 Seed and Party IV worked in-game. `BOX14 + Party IV + Repel` and `BOX14 + Party IV + Repel + SID` remain BUILD-TESTED until those exact larger compositions are exercised in-game.

Additional Pokémon Center observation: while on Center 1F, BOX14 Seed normalization can set `gWirelessCommType` to `00` / RNG 1x, but opening Party or another interface that leaves the overworld and then returning to Center 1F was observed to set the selector back to `01` and restore RNG 2x. This does not change the preset contract: each Seed activation normalizes RFU immediately before its prompt. The exact stock return-to-field routine responsible has not been source-traced yet.


## Mute Music cross-version build support

Production `build-preset-wc3` was exercised for `fr10`, `fr11`, `lg10`, and `lg11`; all four generated WC3 files with valid RamScript checksums and headers. This establishes build support only. Real-cart GAME-VALIDATED status remains limited to the standalone LG1.0 path.
