# Seed Modifier preset

Status: implemented; Java tests pass; FireRed English 1.0 in-game validated.

## Goal

Install the validated `HotkeyRuntimeV1` and use `R + SELECT` to open a small
Field Script payload. The payload displays:

    Press A to set 1234 as seed.

After A (or B, because the stock `waitbuttonpress` command accepts either), the
script writes the 32-bit predecessor of the requested seed directly to
`gRngValue`. The normal VBlank `Random()` step then advances the RNG to the
requested seed.

The first implementation intentionally uses the normal Field Script wait path.
It does not add another native hook or resident RAM block. A roughly one-frame
script-resume delay is acceptable for the intended timer/calibration workflow
and should be measured during emulator testing.

## RNG math

FR/LG uses:

    next = 1103515245 * state + 24691 (mod 2^32)

The predecessor is computed with multiplier inverse `0xEEB9EB65`.

For desired seed `0x1234`:

    predecessor = 0x9A864C25
    next(predecessor) = 0x00001234

## Payload layout

Current payload size for a 4-digit seed prompt: 68 bytes.

- relocatable setup / flow: 8 bytes excluding message-related commands
- message text: 29 bytes including terminator
- message/wait commands: 7 bytes
- four direct `setptr` writes to `gRngValue`: 24 bytes

With `HotkeyRuntimeV1` and this payload on the current build:

    payload:          68 bytes
    runtime overhead: 367 bytes
    total:            435 / 995
    free:             560 bytes

## CLI

    java -cp out Main build-seed-modifier-bin fr10 1234 output.bin
    java -cp out Main build-seed-modifier-wc3 fr10 1234 input.wc3 output.wc3

The seed argument is hexadecimal, with or without `0x`, and currently follows
the old tool's u16 input contract (`0000` through `FFFF`).

## Test expectation

1. Talk to the deliveryman once to install the runtime.
2. Return to normal overworld control.
3. Press `R + SELECT`.
4. Confirm the message includes the selected seed, e.g. `Press A to set 1234 as seed.`
5. Press A at the intended timing point.
6. Watch `gRngValue` at `0x03005000` while validating timing.

Note: the stock FR/LG `waitbuttonpress` command accepts A or B. The UI text says
A because A is the intended workflow; restricting it to A only would require a
custom wait implementation and is intentionally deferred unless testing shows
it is needed.


## In-game validation

FireRed English 1.0 was validated in emulator. With the prompt visible, holding A and advancing frame-by-frame caused the seed write on the same visible frame in which the message box closed (third manual frame advance in that test). This confirms the simple stock wait path is sufficiently responsive for the intended calibrated RNG-abuse workflow.
