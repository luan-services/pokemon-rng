# Dispatcher Candidate 2c

Candidate 2 and Candidate 2b are rejected.

## What went wrong

### Candidate 2

- clobbered `r4` from VBlank without preserving it;
- could reinstall callback1 during a transition because it gated only on
  `callback2 == CB2_Overworld`.

### Candidate 2b

The architectural fixes were correct, but two hand-encoded Thumb instructions
were wrong.

The supervisor starts at:

```text
03005310
```

Its literal table starts at:

```text
03005324
```

The original instruction was:

```text
03 A3    adr r3, ...
```

For a Thumb ADR at `03005310`, PC is `03005314`. `imm=3` adds 12 bytes:

```text
03005314 + 0x0C = 03005320
```

That lands four bytes before the literal table, in the NOP padding. On the
first VBlank the code then treated `46C046C0` as a pointer and dereferenced it,
which explains the immediate reset after the deliveryman installed the hook.

Candidate 2c uses:

```text
04 A3
```

giving:

```text
03005314 + 0x10 = 03005324
```

exactly the literal table.

The callback wrapper also used:

```text
8A E7
```

which branches from `03003F9A` to `03003EB2`.

The fixed tail stub begins at:

```text
03003EB4
```

so Candidate 2c uses:

```text
8B E7
```

which resolves exactly to `03003EB4`.

## Safety gate

The supervisor uses only `r0-r3` and touches callback1 only when:

```text
gMain.callback1 == 08056535
```

Then:

```text
gMain.callback1 = 03003F95
```

Every other callback1 value is left untouched.

## What this candidate tests

No hotkey and no external payload yet.

The callback wrapper merely:

```text
0201C100 <- 0x77 (u8)
tail-chain -> CB1_Overworld
```

The test is solely whether the game can transition/battle normally and the
supervisor can re-arm the wrapper after the game itself restores CB1_Overworld.
