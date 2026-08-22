# ramscript-tools

Build, inspect, compose, import and export FireRed/LeafGreen Wonder Card RamScripts.

## Production architecture

Build 16 freezes the validated Build 15a Field Script path as the production baseline:

- simple/local presets may remain directly in RamScript;
- persistent hotkey Field Scripts use the validated deferred `MultiHotkeyRuntimeV1`;
- each persistent entry uses a 10-byte SaveBlock1 gateway (`setvaddress + vgoto`);
- the preset body lives in the validated 1024-byte SaveBlock2 region;
- hotkeys are ignored while another Field Script/dialogue owns field controls.

See `docs/ARCHITECTURE_BASELINE.md`. Rejected Build 11–14 implementations are preserved under `src/legacy/rejected/`, not deleted.

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

The validated persistent Field Script composition path is the Build 15a deferred multi-hotkey runtime with 10-byte SaveBlock1 gateways into SaveBlock2 payloads. Build 16 consolidates this as the baseline without changing runtime bytes. See `BUILD-16-NOTES.md`, `docs/ARCHITECTURE_BASELINE.md`, and `docs/PERSISTENT_HOTKEY_RUNTIME.md`.
