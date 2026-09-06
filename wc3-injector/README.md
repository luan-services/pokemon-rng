# wc3-injector

Small Java 21 tool for Pokémon FireRed/LeafGreen Gen III save files.

It provides both:

- a human-oriented command-line interface;
- a stable machine-facing JSON API v1 intended for the desktop application and other integrations.

The core can:

- inject a complete `.wc3` into the active FR/LG save slot;
- extract the currently saved Wonder Card + RamScript back into a standard `0x58C`-byte `.wc3`;
- inspect both save slots and identify the active valid slot;
- validate a `.wc3` before injection.

There is intentionally no separate "extract RamScript `.bin`" operation. The supported extraction unit is the full WC3 file.

No Maven, Gradle, external JSON library, or Java package migration is required.

## Project layout

```text
wc3-injector/
├── src/
│   ├── Main.java
│   ├── ApiV1.java
│   ├── Json.java
│   ├── Binary.java
│   ├── Crc16.java
│   ├── FireRedLeafGreenSave.java
│   ├── SectorChecksum.java
│   ├── Wc3File.java
│   └── WonderCard.java
├── tests/
│   └── TestRunner.java
├── docs/
│   ├── API_V1.md
│   ├── DESKTOP_INTEGRATION.md
│   ├── FORMAT_NOTES.md
│   ├── TESTING.md
│   └── LEGACY_README.txt
├── compile.bat
├── test.bat
├── build-api-jar.bat
└── README.md
```

`docs/LEGACY_README.txt` preserves the original README text unchanged.

## Compile

From the project root:

```cmd
javac -encoding UTF-8 -d out src\*.java
```

PowerShell equivalent:

```powershell
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue; New-Item -ItemType Directory out | Out-Null; javac -encoding UTF-8 -d out (Get-ChildItem src -Filter *.java).FullName
```

## Human CLI

### Inject

```cmd
java -cp out Main inject input.sav event.wc3 output.sav
```

The old three-argument syntax remains accepted:

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

### Extract

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

### Inspect a save

```cmd
java -cp out Main inspect-save firered.sav
```

### Verify a WC3

```cmd
java -cp out Main verify-wc3 event.wc3
```

Content anomalies such as CRC/checksum mismatches are warnings rather than automatic rejection. That behavior is deliberate because the project handles custom and research Wonder Cards.

## Machine JSON API v1

Build or use the executable `wc3-injector-api-v1.jar`, then call:

```cmd
java -jar wc3-injector-api-v1.jar api version
java -jar wc3-injector-api-v1.jar api inspect-save --input input.sav
java -jar wc3-injector-api-v1.jar api verify-wc3 --input event.wc3
java -jar wc3-injector-api-v1.jar api inject --input-save input.sav --wc3 event.wc3 --output output.sav
java -jar wc3-injector-api-v1.jar api extract --input-save input.sav --output extracted.wc3
```

The API contract is documented in [`docs/API_V1.md`](docs/API_V1.md). Desktop integration guidance is in [`docs/DESKTOP_INTEGRATION.md`](docs/DESKTOP_INTEGRATION.md).

The important contract rule is: **the desktop application must not parse human CLI output**. It should invoke only `Main api ...` and parse the JSON object returned on stdout.

## Tests

```cmd
javac -encoding UTF-8 -d out src\*.java tests\TestRunner.java
java -cp out TestRunner
```

The current suite covers core injection/extraction behavior and the JSON API contract. See [`docs/TESTING.md`](docs/TESTING.md).

## Scope

This tool handles the 128 KiB FireRed/LeafGreen Gen III save layout used by the project.

It does not edit Wonder Card text/design and does not build RamScripts. Those responsibilities belong to the separate `wc3-builder`, `ramscript-tools`, and related projects.

Future transports such as Celio-GB or distribution ROM generation should remain separate transport implementations behind the desktop application's `Wonder Card Transporter` UI. They should not be faked by adding save-file-specific logic to the JavaFX layer.
