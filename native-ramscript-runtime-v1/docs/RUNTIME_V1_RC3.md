# Native RamScript Runtime v1 RC3

RC3 adds a hotkey safety gate to the validated RC2 visual runtime.

## Why RC3 exists

RC2 proved that R+SELECT can schedule a real Field Script, but a trigger at the
wrong moment can replace the game's current global ScriptContext.

Observed examples:

- final line of an NPC dialogue;
- a YES/NO prompt;
- a trainer's final line immediately before battle.

Overwriting that context can remove the original continuation, such as the
trainer battle start.

## Why `ScriptContext_IsEnabled()` is not the gate

FRLG has three global context states:

```text
CONTEXT_RUNNING
CONTEXT_WAITING
CONTEXT_SHUTDOWN
```

`ScriptContext_IsEnabled()` returns true only for RUNNING. A script can yield
to native wait code while still owning field controls.

The stronger state for this first safety test is:

```text
sLockFieldControls
```

`ScriptContext_SetupScript()` locks field controls, and normal script completion
unlocks them. RC3 therefore ignores the hotkey whenever this byte is nonzero.

FR10:

```text
sLockFieldControls = 03000F9C
```

## Wrapper change

The RC2 wrapper's final 4 reserved bytes now store:

```text
0300532C = 03000F9C
```

The old NOP at 0300531C becomes a PC-relative load into r2, and the trigger
branches to the safety gate.

## Safety gate

Stored in 10 bytes of the 12-byte alignment padding:

```text
03005434..0300543F
```

between `sIsInSaveFailedScreen` and `gHostRfuGameData`.

Pseudo-code:

```text
if (*sLockFieldControls != 0)
    tail-call CB1_Overworld;
else
    enter the validated RC2 bridge;
```

## Required tests

The ordinary overworld trigger must still display:

```text
Hello from the Wonder Card!
```

Then test all three regression scenarios.

### Normal NPC

Press R+SELECT throughout the conversation, including the final text.

Expected: ignored until the conversation has completely ended and player
control has returned.

### YES / NO prompt

Press R+SELECT while the prompt is visible and while confirming an answer.

Expected: ignored; the original prompt/script continues normally.

### Trainer pre-battle dialogue

Press R+SELECT during the trainer's final line and while dismissing it.

Expected: ignored; the battle must still start normally.

After the battle returns to the overworld, R+SELECT should work again.

## Other regression tests

Repeat:

- first-Options-in-deliveryman-room test;
- map transitions;
- Bag / Party;
- healing;
- multiple hotkey activations.

`03003F70..03003F93` remain untouched as in RC1/RC2.

## Status

FR10-only release candidate.

This gate intentionally starts conservative: if the game reports field
controls locked, the custom trigger does nothing.
