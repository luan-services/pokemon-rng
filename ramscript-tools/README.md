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
