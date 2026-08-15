# Dispatcher Candidate 3

Candidate 3 combines the two phase-3 results already validated in the emulator:

```text
Candidate 1 -> R+SELECT works from normal callback context
Candidate 2d -> callback wrapper automatically re-arms after maps/battles/menus
```

## Runtime flow

```text
VBlank
  -> 03005311 supervisor
       if callback1 == CB1_Overworld:
           callback1 = 03003F71
       return directly to 08000725 original VBlank

normal main loop
  -> callback1 03003F71
       R held + SELECT newly pressed?
         no  -> CB1_Overworld
         yes -> [03003FA1] = 77
                CB1_Overworld
```

The VBlank supervisor does not inspect keys and does not execute the marker
payload. The trigger/action runs in normal callback context.

## New storage under test

The 16-byte detector is stored at `03003F70`, which the FireRed 1.0 symbol file
labels `gLinkTestBGInfo`. The adjacent `03003F94..03003F9F` bytes are the three
`gLinkFiller` words already used by earlier fixed-RAM work. The symbol table
places `gLastSendQueueCount` at `03003FA0`; Candidate 3 does not modify that
named byte, but writes its marker to the following padding byte `03003FA1`.

Therefore Candidate 3 is explicitly an **offline/non-link gameplay test**.
Do not test cable, wireless, Union Room, or link battles with this candidate.

## What success means

After installation:

```text
03003550 = 03005311
030030F0 = 03003F71
03003EC0 = 08056535
03003FA1 = usually 00 before first trigger
```

Pressing R+SELECT should set:

```text
03003FA1 = 77
```

Map transitions/battles may temporarily replace callback1. On returning to the
normal overworld, the supervisor should restore:

```text
030030F0 = 03003F71
```

and R+SELECT should continue setting the marker.

If this survives the same stress test as 2d, phase 3 has proven a persistent,
auto-rearming hotkey dispatcher whose action executes outside IRQ context.
