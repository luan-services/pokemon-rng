# Dispatcher Candidate 2 - automatic overworld re-arm

## Question

Candidate 1 proved that `gMain.callback1` can execute our native wrapper in
normal main-loop context, but the game correctly replaces callback1 during map
transitions and battles.

Candidate 2 asks only:

> Can a tiny persistent VBlank supervisor leave those temporary callbacks alone
> and automatically restore our callback1 wrapper when the normal overworld
> returns?

## Deliberately no hotkey

Candidate 2 does **not** test R+SELECT. Candidate 1 already proved that part.
Mixing the hotkey into this test would add an unnecessary variable.

While installed, the overworld wrapper only writes one byte:

```text
0201C100 = 77
```

and then calls the original `CB1_Overworld`.

## Runtime layout

```text
03005310..0300532F  32 bytes  VBlank supervisor
03003F94..03003F9F  12 bytes  normal-context callback wrapper
03003EB4..03003EB7   4 bytes  CB1 tail stub
03003EC0..03003EC3   4 bytes  CB1_Overworld|1 literal
```

No new fixed-RAM region is introduced.

## Supervisor rule

On every VBlank:

```text
if gMain.callback2 == 080565B5 (CB2_Overworld):
    gMain.callback1 = 03003F95

jump to original VBlankIntr
```

If callback2 is anything else, callback1 is left completely untouched.

This is important during loading/battle states where the game owns callback1.

## Expected observations

Before deliveryman, normal overworld:

```text
030030F0 = 08056535
030030F4 = 080565B5
03003550 = 08000725
```

After installer and one VBlank:

```text
030030F0 = 03003F95
030030F4 = 080565B5
03003550 = 03005311
0201C100 low byte = 77
```

During a map transition or battle:

```text
callback1/callback2 may become NULL or other game callbacks
```

Candidate 2 must not fight that state.

After normal overworld returns:

```text
030030F4 = 080565B5
```

and on the next VBlank:

```text
030030F0 = 03003F95
```

again automatically.

## Test abuse

Try:

- stairs / doors repeatedly;
- enter and leave buildings;
- open Party and Bag;
- heal;
- speak to NPCs;
- wild/trainer battle;
- return from battle;
- save if desired.

A temporary callback change is expected. A crash, black screen, frozen controls,
or callback2 being prevented from changing is a failure.

`0201C100` is only a temporary debug address and other game systems are known to
overwrite it. For this test watch it as an **8-bit** value; seeing something
other than `77` outside normal overworld is not itself a failure.
