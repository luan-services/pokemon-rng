# Dispatcher Candidate 5b

Candidate 5b is the clean-layout diagnostic for the C5 regression.

## What it preserves

- same persistent VBlank supervisor;
- same R + SELECT detector;
- same conceptual call/return mechanism:
  - save incoming LR;
  - manufacture a local Thumb return address;
  - call a ROM function;
  - return locally;
  - restore the original callback return.

The action remains the already-proven:

```text
PlaySE(SE_SELECT)
```

## What it removes

Candidate 5/C5a2 wrote executable code across:

```text
03003F80..03003F93
```

which contains live globals including `gLinkCallback`.

Candidate 5b writes NOTHING there.

## FR10 layout

```text
03003F70..7F  detector
03003F94..97  gLinkFiller3: heldKeysRaw pointer
03003F98..9B  gLinkFiller4: tiny function thunk
03003F9C..9F  gLinkFiller5: PlaySE|1 pointer

03003FA0      gLastSendQueueCount -- untouched
03003FA1      padding
03003FA2..A3  continuation: pop {pc}
03003FA4..AF  trigger wrapper in linker padding
03003FB0      gLink begins -- untouched
```

The symbol layout leaves `03003FA1..03003FAF` between the one-byte
`gLastSendQueueCount` and the aligned `gLink` object.

## Critical regression tests

### Test A

```text
load save
→ go to deliveryman room
→ open Options for the first time in the session
→ change Button Mode
→ close Options
→ DO NOT change map
→ talk to deliveryman
→ R + SELECT
```

Expected: SE_SELECT beep, no freeze.

### Test B

```text
load save
→ change/open Options
→ change map
→ return
→ talk to deliveryman
→ R + SELECT
```

Expected: SE_SELECT beep, no freeze.

## Interpretation

If both pass while C5a2 Test A freezes, the strongest explanation is that the
old `03003F80..03003F93` live-global overlap caused the regression.

This still does not make the whole runtime production-safe; `03003F70` is
link-test storage and this candidate remains offline-only.
