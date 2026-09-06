# Testing

The project includes a dependency-free Java test runner.

Run:

```cmd
javac -encoding UTF-8 -d out src\*.java tests\TestRunner.java
java -cp out TestRunner
```

Current expected result:

```text
PASS: 45 tests
```

## Core coverage

### Injection → extraction round trip

A synthetic two-slot FR/LG save and a valid synthetic WC3 are generated.

The tests verify that:

- the newest valid slot is used;
- Wonder Card and RamScript bytes survive the round trip;
- questionnaire bytes from the save are preserved;
- `WonderCardMetadata.iconSpecies` is normalized from `WonderCard.iconSpecies`;
- the extracted card CRC is valid;
- the extracted RamScript checksum is valid.

### Corrupted newer slot fallback

The newer slot is corrupted without recalculating its sector checksum.

The tests verify that:

- the corrupted newer slot is rejected;
- the older valid slot becomes active.

### Nonstandard / invalid-checksum WC3 remains injectable

A valid WC3 is modified without updating its card CRC.

The tests verify that:

- the mismatch is reported through validation warnings;
- injection remains allowed for custom/research use.

### Empty / nonstandard Wonder Card extraction remains possible

A structurally valid synthetic save is generated without a normal Wonder Card/RamScript payload.

The tests verify that raw WC3 reconstruction still occurs and warnings remain available.

## API v1 contract coverage

The tests also verify:

- protocol `wc3-injector-json`;
- numeric API version `1`;
- version capabilities;
- exactly one JSON line on stdout;
- structured `inspect-save` data;
- structured WC3 verification;
- injection output artifact metadata;
- extraction output artifact metadata;
- structured error codes for missing commands, unknown commands, missing files, and invalid WC3 size.

## Recommended manual checks

Before publishing a release, also test with copies of real FR/LG saves:

1. `inspect-save` on a known-good save.
2. `extract`, then inspect the resulting WC3 using the other project tools.
3. inject that extracted WC3 into another copy of the same save.
4. boot in emulator and confirm the save loads normally.
5. inject a known official/custom WC3 and verify the deliveryman flow.
6. invoke the executable JAR through `api` and confirm the desktop process wrapper parses the response correctly.

Never use the only copy of a real save during testing.
