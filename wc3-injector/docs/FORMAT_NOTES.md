# Format notes

These notes document only the pieces of the FR/LG save and WC3 layout used by this tool.

## Save slots and sectors

The 128 KiB `.sav` contains two game-save slots.

Each slot contains 14 physical sectors. Logical sector IDs are stored in the footer, so logical sector 4 is **not assumed to be at a fixed physical sector**.

Footer offsets inside a 0x1000-byte sector:

```text
0xFF4  logical sector ID (u16)
0xFF6  sector checksum (u16)
0xFF8  signature (u32) = 0x08012025
0xFFC  save counter (u32)
```

The game rotates physical sectors when writing saves. The injector therefore searches the active slot for the sector whose logical ID is `4`.

## Checksum sizes

The game checksums only the amount of real structure data stored in each logical sector.

```text
logical 0       0xF24
logical 1..3    0xF80
logical 4       0xEE8
logical 5..12   0xF80
logical 13      0x7D0
```

Logical sector 4 is the final SaveBlock1 chunk, which is why the original injector already used `0xEE8` when recalculating its checksum.

## WC3 layout

A `.wc3` used by this project is `0x58C` bytes:

```text
0x000..0x19F  Wonder Card/card metadata block (0x1A0 bytes)
0x1A0..0x58B  RamScript (0x3EC bytes)
```

The first four bytes of the card block store its CRC. The actual `WonderCard` struct begins at `0x004`.

The RamScript block has its own checksum in its first four bytes.

## Save destinations inside logical sector 4

```text
card block destination       0x460
RamScript destination        0x79C
```

## Questionnaire preservation

Offsets `0x178..0x17F` relative to the card block overlap the save's questionnaire words.

Injection intentionally does not overwrite these bytes.

Therefore a later extraction returns the questionnaire bytes from the save, which may differ from the `.wc3` that was originally injected.

## Icon metadata

The game mirrors `WonderCard.iconSpecies` into `WonderCardMetadata.iconSpecies`.

The injector reproduces that behavior during injection. This also means an extracted WC3 reflects the normalized metadata stored by the game/save, not necessarily a deliberately inconsistent input file.
