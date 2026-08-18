# Custom payload execution

Basic deliveryman presets remain direct builders. They do not need the trigger
layer.

User-created Field Scripts and advanced presets can choose how they execute.

## Deliveryman

```text
custom Field Script
→ RamScript
→ talk to deliveryman
→ execute immediately
```

No HotkeyRuntimeV1 is added and no ROM-specific native runtime is required.

```powershell
java -cp out Main build-custom-payload-wc3 `
  deliveryman `
  fr10 `
  custom-payload.bin `
  card.wc3 `
  output.wc3
```

The ROM argument is kept for a uniform CLI/reporting shape. A pure deliveryman
Field Script does not use the RomProfile.

## Hotkey

```text
custom Field Script
→ HotkeyRuntimeV1
→ install by talking to deliveryman
→ execute later with R+SELECT
```

```powershell
java -cp out Main build-custom-payload-wc3 `
  hotkey `
  fr10 `
  custom-payload.bin `
  card.wc3 `
  output.wc3
```

## Payload contract

`custom-payload.bin` contains raw executable FR/LG **Field Script bytes**, not a
full 1004-byte RamScript binary.

If the payload contains pointers to its own strings/labels, it should normally
use `setvaddress` and relocatable `v*` commands, as emitted by
`RamScriptBuilder`.

The composition layer preserves those payload bytes exactly.

## Intended split

Direct deliveryman examples:

- Give Item
- Give Pokemon
- Show Secret ID
- one-shot custom scripts

Hotkey examples:

- Seed Modifier
- RNG tools
- runtime inspectors

A future multi-hotkey runtime can extend the trigger layer without changing the
custom Field Script payload contract.
