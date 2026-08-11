# wc3-injector

Small Java 21 command-line tool for Pokémon FireRed/LeafGreen Gen III save files.

It can:

- inject a complete `.wc3` into the active FR/LG save slot;
- extract the currently saved Wonder Card + RamScript back into a standard `0x58C`-byte `.wc3`;
- inspect both save slots and identify the active valid slot;
- validate a `.wc3` before injection.

No Maven, Gradle, or Java packages are used.

## Project layout

```text
wc3-injector/
├── src/
│   ├── Main.java
│   ├── Binary.java
│   ├── Crc16.java
│   ├── FireRedLeafGreenSave.java
│   ├── SectorChecksum.java
│   ├── Wc3File.java
│   └── WonderCard.java
├── tests/
│   └── TestRunner.java
├── docs/
│   ├── FORMAT_NOTES.md
│   ├── TESTING.md
│   └── LEGACY_README.txt
├── compile.bat
├── test.bat
└── README.md
```

`docs/LEGACY_README.txt` preserves the original README text unchanged.

## Compile

From the project root:

```cmd
javac -encoding UTF-8 -d out src\*.java
```

## Inject

```cmd
java -cp out Main inject input.sav event.wc3 output.sav
```

The old three-argument syntax is still accepted for compatibility:

```cmd
java -cp out Main input.sav event.wc3 output.sav
```

Injection:

1. reads the `.wc3` and reports content warnings without blocking custom/research cards;
2. validates both FR/LG save slots, including per-sector checksums;
3. selects the newest valid slot by unsigned save counter;
4. locates logical sector 4 even when physical sectors are rotated;
5. preserves the questionnaire bytes already stored in the save;
6. copies the Wonder Card block;
7. mirrors `WonderCard.iconSpecies` into `WonderCardMetadata.iconSpecies`;
8. copies the full RamScript block;
9. recalculates logical sector 4's save checksum.

Always work on a copy of the original save.

## Extract

```cmd
java -cp out Main extract input.sav extracted.wc3
```

Extraction selects the newest fully valid save slot and reconstructs:

```text
0x000..0x19F  card block
0x1A0..0x58B  RamScript block
```

The questionnaire bytes in an extracted WC3 are the bytes currently stored in the save. This is expected: injection deliberately preserves the save's questionnaire rather than replacing it with the incoming WC3's copy.

Extraction is intentionally permissive. It reconstructs the bytes present in the save even if `flagId` is zero or the card/RamScript checksums do not match.

The command reports the extracted flag ID, card CRC status, RamScript checksum status, and any warnings, but still writes the `.wc3`. This is useful for custom cards, experiments, and inspection of unusual save data.

## Inspect a save

```cmd
java -cp out Main inspect-save firered.sav
```

Example:

```text
Slot 1: INVALID - invalid signature or logical sector IDs
Slot 2: VALID (counter 2) - ok
Active slot: 2 (counter 2)
```

A slot is considered valid only when all 14 sectors have:

- the expected signature;
- unique logical IDs `0..13`;
- one shared save counter;
- the correct checksum for the amount of structure data stored in that logical sector.

## Verify a WC3

```cmd
java -cp out Main verify-wc3 event.wc3
```

This reports:

- flag ID;
- icon species;
- stored/calculated card CRC;
- stored/calculated RamScript checksum.

Injection does **not** reject a WC3 only because `flagId`, card CRC, icon metadata, or RamScript checksum are unusual. These are reported as warnings so custom/research cards can still be injected.

## Run tests

The tests generate synthetic save/WC3 data in temporary files. No personal save is bundled with the project.

See [`docs/TESTING.md`](docs/TESTING.md) for the covered cases.

## Scope

This tool handles the 128 KiB FireRed/LeafGreen Gen III save layout used by the project.

It does not edit Wonder Card text/design and does not build RamScripts. Those responsibilities belong to the separate `wc3-builder`, `ramscript-tools`, and `native-ramscript-tools` projects.
