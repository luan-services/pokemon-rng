# Dispatcher Candidate 2b

Candidate 2b replaces the rejected Candidate 2.

## Why Candidate 2 was rejected

Two concrete problems were found after the first map-transition crash.

1. The VBlank supervisor used `r4` without preserving it before tail-chaining
   into the original VBlank handler. `r4` is callee-saved.
2. The supervisor used only `callback2 == CB2_Overworld` as its safety gate.
   During a transition the game may set `callback1 = NULL` before changing
   callback2, allowing the supervisor to incorrectly reinstall the wrapper in
   a window where the game deliberately disabled callback1.

Candidate 2 should not be used again.

## Candidate 2b rule

The VBlank supervisor uses only caller-saved `r0-r3`.

It touches callback1 only when:

```text
gMain.callback1 == 08056535   // CB1_Overworld|1
```

Then:

```text
gMain.callback1 = 03003F95
```

For every other value:

```text
NULL
battle callback
menu callback
transition callback
already-installed wrapper
anything else
```

the supervisor does nothing.

This means the game owns callback transitions completely. The supervisor waits
until the game itself has restored the exact normal overworld callback.

## Wrapper

The normal-context wrapper remains intentionally trivial:

```text
0201C100 <- 0x77 (u8)
tail-chain -> CB1_Overworld
```

There is still no hotkey in this candidate. Candidate 1 already established that
normal-context hotkey execution works; 2b tests only safe automatic re-arming.

## Expected transition

```text
normal overworld:
callback1 = 03003F95

warp begins:
callback1 = 00000000 / another callback
supervisor DOES NOTHING

map loads:
callback1 continues under game control

game restores:
callback1 = 08056535

next VBlank:
callback1 = 03003F95
```
