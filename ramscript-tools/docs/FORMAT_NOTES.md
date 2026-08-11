# RamScript format notes

A Wonder Card WC3 is `0x58C` bytes.

The RamScript occupies:

```text
WC3 offset 0x1A0
size       0x3EC
```

Internally:

```text
0x000  u32 checksum
0x004  magic
0x005  mapGroup
0x006  mapNum
0x007  objectId
0x008  script[995]
...
```

For Wonder Card delivery scripts the standard header is:

```text
33 FF FF FF
```

The checksum covers the `RamScriptData` bytes after the checksum word.

## Relocatable Field Scripts

Saved Wonder Card scripts cannot safely depend on the physical address where SaveBlock1 happens to be allocated.

`RamScriptBuilder` therefore uses:

```text
setvaddress
v* branch/message commands
virtual labels
pointer fixups
```

for relocatable embedded code/data.

The virtual base is an address namespace used by the script engine; it is not the physical runtime SaveBlock address.

## BIN versus WC3

```text
.bin
= exactly one 0x3EC-byte RamScript

.wc3
= card/design block + RamScript
```

`extract-bin` and `inject-bin` intentionally make this boundary explicit.
