# Native RamScript Runtime v1 RC2

RC2 keeps the validated RC1 resident runtime byte-for-byte and replaces only
the debug marker payload with a visual Field Script.

## Hotkey flow

```text
R + SELECT
→ GetSavedRamScriptIfValid()
→ ScriptContext_SetupScript(script + 0x0A)
→ Field Script engine
```

## Visual payload

The payload starts with its own `setvaddress`, because the hotkey enters
directly at the payload entry point and must not depend on the deliveryman's
earlier virtual-address state.

```text
setvaddress 0x08010000
lockall
vmessage 0x0801000F
waitmessage
waitbuttonpress
releaseall
end

"Hello from the Wonder Card!"
```

Expected behavior:

1. R+SELECT opens the message.
2. Player controls are locked while the script owns the field context.
3. The message waits normally.
4. After input, the box closes.
5. Player control returns.
6. The trigger remains usable after maps, menus, battles, and healing.

`lockall`/`releaseall` are used because the trigger may be activated without
any selected NPC.

## Regression requirements

Repeat the RC1 first-Options test and normal stress tests.

The resident runtime areas deliberately removed in RC1 must remain untouched:

```text
03003F70..03003F7F
03003F80..03003F93
```

## Status

FR10-only release candidate.

Link/wireless behavior remains unvalidated.
