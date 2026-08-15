# Dispatcher Candidate 1

This is a deliberately small Phase-3 experiment. It does **not** replace the
known-good seed modifier and does not try to survive callback changes yet.

## Question being tested

Can a fixed-RAM hotkey wrapper run as `gMain.callback1`, i.e. in normal
main-loop context after `ReadKeys()`, and then safely tail-chain into the
original `CB1_Overworld`?

If yes, larger/deferred native effects do not need to execute inside VBlank.

## FireRed English 1.0 values

```text
gMain                  030030F0
gMain.callback1        030030F0
heldKeysRaw/newKeysRaw 03003118
CB1_Overworld          08056534 (Thumb 08056535)
fixed wrapper          03005310..0300532F
debug extension        03003F94..03003F9F
tail stub              03003EB4..03003EB7
CB1 literal            03003EC0..03003EC3
```

The fixed RAM regions are the same small regions already exercised by the
known-good seed modifier. Their contents are different in this candidate.

## Runtime

```text
main loop
  ReadKeys()
  callback1 -> fixed wrapper
                 |
                 +-- no R+SELECT -> CB1_Overworld
                 |
                 `-- R+SELECT -> 0201C100 = 12345678
                                   -> CB1_Overworld
  callback2
  ...
```

There is no VBlank hook in this candidate.

## Expected limitation

Map/menu/other transitions are allowed to replace `gMain.callback1`. When that
happens the hotkey will stop working. Candidate 1 intentionally has no
maintenance/reinstaller yet.

That result means "normal-context dispatch works, persistence remains to solve".
It is not considered a candidate failure.

A crash, broken input, stuck player, or corrupted game state is a failure.

## Build

```cmd
java -cp out Main build-dispatcher-candidate-1 fr10 dispatcher-c1.bin
```

Attach it to a WC3 with `ramscript-tools inject-bin`, then inject the WC3 into a
copy of a save with `wc3-injector`.

## Watch addresses

Watch as 32-bit little endian where applicable:

```text
0201C100  debug output
030030F0  gMain.callback1
030030F4  gMain.callback2
03003118  heldKeysRaw + newKeysRaw packed word
03003550  VBlank slot (should remain the game's original value in this candidate)
```

After the deliveryman installs the candidate:

```text
030030F0 should become 03005311
```

Pressing R+SELECT should make:

```text
0201C100 = 12345678
```

Then test normal movement and interactions. Finally change map/open menus and
observe whether the game simply restores its own callback1. The hotkey being
lost after that is expected for Candidate 1.
