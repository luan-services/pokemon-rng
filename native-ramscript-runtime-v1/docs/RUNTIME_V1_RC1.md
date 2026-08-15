# Native RamScript Runtime v1 RC1

This is the first cleanup release candidate derived from the validated
Dispatcher Candidate 6.

It intentionally keeps the same trivial Field Script payload (`marker = 0x66`)
so that this test isolates the runtime memory-layout cleanup rather than adding
new functionality.

## What changed from Candidate 6

Removed persistent use of:

```text
03003F70..03003F7F  gLinkTestBGInfo
```

Removed temporary use of:

```text
02021D18..           gStringVar4
```

Still untouched:

```text
03003F80..03003F93  live link globals
```

## Two-phase use of 03005310..0300532F

`UnusedVarNeededToMatch` is 32 bytes.

During installation:

```text
callback1 = 0

03005310..1F = temporary installer
call installer
    ↓
atomic STR:
gMain.vblankCallback = 03003F43
```

The supervisor is now live, but it only rearms when:

```text
callback1 == 08056535
```

Since callback1 is still zero, it cannot install a half-written wrapper.

Then:

```text
03005310..2F = final wrapper
callback1 = 08056535
```

Only after the complete wrapper exists can a later VBlank replace callback1
with `03005311`.

This preserves the atomic VBlank-hook property without a separate staging
buffer.

## Persistent memory map

```text
03003EB4..B7  original-VBlank tail       gLinkFiller1
03003EC0..C3  original VBlank pointer    gLinkFiller2

03003F42..4F  VBlank supervisor          linker padding
03003F98..9B  ROM-call thunk             gLinkFiller4
03003F9C..9F  GetSaved function pointer  gLinkFiller5

03003FA1      test marker                linker padding
03003FA4..AF  supervisor literals        linker padding

03005032..3F  bridge stage 2             linker padding
03005082..8F  bridge stage 1             linker padding

03005310..2F  callback wrapper           UnusedVarNeededToMatch
```

The remaining `gLinkFiller*` symbols are declared as filler and no functional
reads/writes were found in `link.c`.

## Runtime bridge

```text
R + SELECT
→ stage 1
→ GetSavedRamScriptIfValid()
→ stage 2
→ script pointer + payload offset
→ ScriptContext_SetupScript()
→ Field Script engine
```

## RC1 test payload

```text
setptr 0x66, 0x03003FA1
end
```

## Required regression tests

1. First-Options test:

```text
load save
→ open Options for first time in deliveryman room
→ change Button Mode
→ close
→ talk to deliveryman immediately
→ R + SELECT
```

Expected: marker becomes `66`, no freeze.

2. Stress test:
   maps, buildings, NPCs, Bag, Party, battle, healing, then hotkey again.

3. Confirm these remain untouched before/after installation:

```text
03003F70..03003F7F
03003F80..03003F93
```

4. Confirm:

```text
03005310..0300532F
```

changes first during installation and finishes as the stable final wrapper.

## Status

FR10-only release candidate.

Cable/wireless/link behavior has not been validated yet. Do not advertise link
support until it has its own test pass.
