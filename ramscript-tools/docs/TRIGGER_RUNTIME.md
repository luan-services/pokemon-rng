# Trigger Runtime Integration

This revision introduces a trigger-composition layer without changing any
existing preset.

## Architecture

A Field Script payload is built independently of how it is entered:

```text
payload bytes
   |
   +-- DELIVERYMAN ----> RamScript directly
   |
   +-- HOTKEY_RUNTIME -> Runtime v1 wrapper -> R+SELECT -> payload
```

`TriggerComposer` is intentionally payload-agnostic. It does not know about
items, flags, gifts, RNG, or any other preset.

## Current first test

`TriggerTestPayloads.helloWonderCard()` uses the existing `RamScriptBuilder` to
build:

```text
setvaddress
lockall
vmessage
waitmessage
waitbuttonpress
releaseall
end
"Hello from the Wonder Card!"
```

The payload is 43 bytes.

## Runtime profiles

- `fr10`: runtime-validated
- `lg10`: runtime-validated
- `fr11`: symbol-verified, runtime-untested
- `lg11`: symbol-verified, runtime-untested

## Space accounting

Every trigger build reports:

```text
payload bytes
runtime overhead
total script bytes / 995
free bytes
```

The deliveryman path has zero trigger overhead. The hotkey path reports the
installer/runtime bytes separately from the event payload.

## Next step

Do not duplicate presets. Instead, expose payload-building methods from the
existing preset/event builders and pass those bytes to `TriggerComposer`.
Then ItemGiftPreset, RepeatableItemGiftPreset, etc. can choose a trigger without
knowing anything about native addresses.
