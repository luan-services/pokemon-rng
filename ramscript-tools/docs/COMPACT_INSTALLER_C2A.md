# Compact Installer Candidate 2a

C2a fixes the alignment defect in rejected Candidate 2.

## Root cause of C2 crash

The saved script begins at:

```text
SaveBlock1 + 0x361C  RamScript
+ 0x04               RamScriptData
+ 0x04               magic/mapGroup/mapNum/objectId
= SaveBlock1 + 0x3624
```

Therefore `script[]` is 4-byte aligned.

Rejected C2 placed the native blob at:

```text
script + 0x0A
```

which is `2 mod 4`.

The native installer used Thumb `ADR` instructions. Thumb ADR aligns PC down to
a 4-byte boundary before adding its immediate.

With the blob at `2 mod 4`, the encoded ADRs resolved as:

```text
table pointer = expected table + 2
data pointer  = expected data  - 2
```

The copier therefore interpreted the wrong bytes as destination/size metadata
and copied incorrect data to IWRAM.

## C2a fix

Two inert bytes are inserted between the Field Script header and native blob:

```text
+00..09  setvaddress + vgoto
+0A..0B  alignment padding
+0C..    native blob
```

Since the physical script base is 4-byte aligned, `script+0x0C` is also
4-byte aligned.

The existing ADR encodings now resolve exactly to:

```text
blob + 40  table
blob + 88  resident data
```

No hooks are installed. R+SELECT remains inactive.

## Size

The alignment fix costs only two bytes:

```text
C2:   419 / 995   (rejected)
C2a:  421 / 995
free: 574 bytes
```

The expected resident RAM image is otherwise identical to Candidate 2 and the
validated Runtime v1.
