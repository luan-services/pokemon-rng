# Native RamScript Runtime v1 RC4a

RC4a is the corrected version of the rejected RC4 format-guard experiment.

## Why RC4 froze

RC4 introduced an 8-byte validator, but two encodings were wrong.

### Wrong LDRH

The intended instruction was:

```text
ldrh r1,[r0,#10]
```

RC4 emitted `0x8881`, which does not address `r0 + 10`.

RC4a emits:

```text
0x8941
```

which encodes the required halfword offset.

### Out-of-range conditional branch

RC4 attempted:

```text
030053AC  bne 0300508E
```

Thumb-1 conditional branches use an 8-bit signed displacement and cannot span
that distance. The emitted instruction actually targeted a completely
different address, causing the freeze.

RC4a keeps the validator at 8 bytes:

```text
030053A8  ldrh r1,[r0,#10]
030053AA  cmp  r1,#0xA7
030053AC  bne 0300543E
030053AE  b    03005032
```

`0300543E` is now a nearby local reject tail:

```text
pop {r4,pc}
```

It occupies the final 2 bytes that were unused in the RC3 safety-gate padding.

## Test-harness fix

The older TestRunner contained later runtime audit methods but did not invoke
them from `main()`. Therefore "All tests passed" did not mean the RC4 validator
audit had actually run.

RC4a explicitly runs:

```text
testRuntimeV1Rc1StaticAudit()
testRuntimeV1Rc2StaticAudit()
testRuntimeV1Rc3StaticAudit()
testRuntimeV1Rc4aStaticAudit()
```

The rejected RC4 audit is kept only as historical source and is not part of the
validated release-candidate chain.

## Runtime format

Unchanged from the intended RC4 design:

```text
+00..09  deliveryman entry
+0A..0B  A7 00 runtime-format signature
+0C..    hotkey payload
```

## Required tests

First do only:

```text
install
→ normal overworld
→ R+SELECT
```

Expected: `Hello from the Wonder Card!`.

Then:

1. reinstall by talking to the deliveryman again;
2. trigger again;
3. repeat RC3 NPC / YES-NO / trainer safety tests;
4. first-Options regression;
5. maps, battle, Bag, Party and healing.

## Status

RC4 is rejected.
RC4a is the corrected FR10 release candidate.
