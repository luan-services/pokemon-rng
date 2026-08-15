# Dispatcher Candidate 5c

Candidate 5c returns to `GetSavedRamScriptIfValid()` using the clean C5b layout.

## Why only one result byte?

The safe linker padding after `gLastSendQueueCount` is only:

```text
03003FA1..03003FAF
```

and `gLink` starts at `03003FB0`.

A permanent 32-bit debug slot plus the repeatable call/return wrapper would not
fit there without consuming another live/global region.

So C5c stores only:

```text
low8(returned pointer)
```

at:

```text
03003FA1
```

This is enough for the experiment because the full expected pointer is known:

```text
expected = *gSaveBlock1Ptr + 0x3624
```

Therefore compare:

```text
03003FA1 == expected & 0xFF
```

Example:

```text
gSaveBlock1Ptr = 0202557C
expected       = 02028BA0
marker         = A0
```

The marker is repeatable: every R+SELECT call updates it.

## Clean memory layout

```text
03003F80..03003F93  untouched

03003F94..97  heldKeysRaw pointer
03003F98..9B  BX thunk
03003F9C..9F  GetSavedRamScriptIfValid|1

03003FA0      gLastSendQueueCount untouched
03003FA1      result low byte
03003FA2..AF  call wrapper
03003FB0      gLink begins untouched
```

## Critical regression test

Repeat the previously failing path:

```text
load save
→ first Options opening in deliveryman room
→ change Button Mode
→ close Options
→ talk to deliveryman immediately
→ R+SELECT
```

It must not freeze.

Then compare `03003FA1` with the low byte of
`(*gSaveBlock1Ptr + 0x3624)`.

After map transitions, repeat R+SELECT and verify the marker follows the current
relocated SaveBlock1 pointer.
