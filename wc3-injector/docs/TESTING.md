# Testing

The project includes a small no-framework Java test runner.

Run:

```cmd
javac -encoding UTF-8 -d out src\*.java tests\TestRunner.java
java -cp out TestRunner
```

## Automated cases

### Injection → extraction round trip

A synthetic two-slot FR/LG save and a valid synthetic WC3 are generated.

The test verifies that:

- the newest valid slot is used;
- Wonder Card and RamScript bytes survive the round trip;
- questionnaire bytes from the save are preserved;
- `WonderCardMetadata.iconSpecies` is normalized from `WonderCard.iconSpecies`;
- the extracted card CRC is valid;
- the extracted RamScript checksum is valid.

### Corrupted newer slot fallback

Two valid slots are generated and the newer slot is then corrupted without recalculating its sector checksum.

The test verifies that:

- the corrupted newer slot is rejected;
- the older valid slot becomes the active slot.

### Nonstandard / invalid-checksum WC3 remains injectable

A valid WC3 is modified without updating its card CRC.

The test verifies that:

- the mismatch is reported through validation warnings;
- injection remains allowed for custom/research use.

### Empty / nonstandard Wonder Card extraction remains possible

A structurally valid synthetic save is generated without a standard valid Wonder Card/RamScript in logical sector 4.

The test verifies that:

- extraction still reconstructs the raw WC3 layout;
- warnings are available to the caller instead of blocking extraction.

## Recommended manual checks

Before publishing a release, also test with copies of real FR/LG saves:

1. `inspect-save` on a known-good save.
2. `extract`, then inspect the resulting WC3 using `wc3-builder` / RamScript tools.
3. Inject that extracted WC3 into another copy of the same save.
4. Boot in emulator and confirm the save loads normally.
5. Inject a known official/custom WC3 and verify the deliveryman flow.

Never use the only copy of a real save during testing.
