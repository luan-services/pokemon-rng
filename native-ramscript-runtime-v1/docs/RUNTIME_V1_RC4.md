# REJECTED

Do not use this build. Its format validator contains an incorrectly encoded LDRH and an out-of-range Thumb conditional branch. Use RC4a.

# Native RamScript Runtime v1 RC4

RC4 formalizes install/reinstall behavior and adds a RamScript format guard.

## Runtime format

The saved RamScript now has an explicit two-byte signature:

```text
+00..04  setvaddress
+05..09  vgoto installer
+0A..0B  A7 00          runtime-format signature
+0C..    hotkey payload
```

The deliveryman skips metadata and payload via `vgoto`.

The resident runtime executes the payload only if:

```text
GetSavedRamScriptIfValid() != NULL
and
*(u16 *)(script + 0x0A) == 0x00A7
```

This prevents an older resident runtime from blindly interpreting an unrelated
valid RamScript as `script + payloadOffset`.

## Validator

The validator occupies exactly the 8-byte padding:

```text
030053A8..030053AF
```

after `gGameContinueCallback` and before `gRamSaveSectorLocations`.

Pseudo-code:

```text
if (*(u16 *)(script + 0x0A) != 0x00A7)
    return to main loop;

ScriptContext_SetupScript(script + 0x0C);
```

## Install semantics

### First install

The installer writes a deterministic known runtime layout, atomically hooks
VBlank, writes the final callback wrapper, and restores CB1_Overworld.

### Reinstall same runtime

Safe and idempotent.

The installer never captures the current hook as an "original" function and
never chains through an already-installed wrapper. It always reconstructs:

```text
original VBlank literal = ROM VBlank
supervisor              = known runtime supervisor
callback tail           = ROM CB1_Overworld
```

Temporary `callback1 = 0` continues to prevent VBlank from arming a
half-written wrapper.

### Install another compatible RC4-format card

The resident code is rewritten deterministically. The hotkey resolves the
CURRENT saved RamScript every time and accepts it if it has `A7 00`.

Thus the runtime is not permanently bound to the card that originally
installed it.

### Current card is valid but incompatible

If a runtime remains resident while the current valid RamScript does not have
`A7 00`, R+SELECT is ignored.

This is the important new RC4 safety property.

### Reset / power cycle

The resident IWRAM runtime disappears normally. The Wonder Card/RamScript
remains in the save.

The runtime is session-only in v1: talk to the deliveryman again to install it.

## Required tests

1. First install -> R+SELECT message works.
2. Talk to deliveryman again in the same session -> no freeze/crash; final
   callback/VBlank values remain the same; R+SELECT still works.
3. Repeat installation several times.
4. Repeat RC3 dialogue/YES-NO/trainer safety tests.
5. Repeat first-Options regression path.
6. Stress maps/battle/Bag/Party/healing after reinstall.

## Status

FR10-only release candidate.

RC4 introduces runtime-format metadata but does not yet add an explicit
uninstall command.
