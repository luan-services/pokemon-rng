# ramscript-tools

Build, inspect, compose, import and export FireRed/LeafGreen Wonder Card RamScripts.

## Production architecture

- `RamScriptBuilder` / `FieldScriptWriter`: stock Field Script composition.
- `EventTrigger` / `TriggerComposer`: choose classic deliveryman or hotkey execution.
- `HotkeyRuntimeV1`: validated compact R+SELECT runtime promoted from Candidate 5a.
- `RuntimeV1ResidentBlocks`: validated resident IWRAM image used by the hotkey runtime.
- presets such as `ItemGiftPreset` and `RepeatableItemGiftPreset`: reusable event payloads.
- `RomProfile`: ROM-specific symbols and validation status.

The historical `native-ramscript-runtime-v1-multipart` repository remains a research/reference implementation and is not a runtime dependency.

## Build

```powershell
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
New-Item -ItemType Directory out
javac -encoding UTF-8 -d out src/*.java
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
