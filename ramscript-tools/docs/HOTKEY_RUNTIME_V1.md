# Hotkey Runtime v1

Production configurable two-button trigger runtime for FireRed/LeafGreen RamScripts.

The implementation remains the validated Candidate 5a architecture: 20-byte bootstrap, 48-byte aligned block table, 227-byte native installer blob, stock safety/format guards, callback rearm and VBlank supervisor.

## Default compatibility

The historical default remains `R + SELECT`.

For that default chord, the resident wrapper uses the exact original Candidate 5a bytes. Existing default builds therefore remain byte-for-byte compatible with the runtime that was already stress-tested in-game.

With the 43-byte Hello payload on FR1.0 the complete script still uses 409 / 995 bytes, leaving 586 bytes free.

## Configurable hotkey

Hotkeys are represented as an ordered pair:

`held button + newly pressed button`

Examples:

- `r-select`: hold R, press SELECT
- `r-b`: hold R, press B
- `l-start`: hold L, press START

Supported button names:

`a`, `b`, `select`, `start`, `right`, `left`, `up`, `down`, `r`, `l`

The two buttons must be different.

The custom wrapper keeps the same 32-byte resident size. It loads the adjacent `heldKeysRaw` / `newKeysRaw` values in one 32-bit word and uses Thumb carry tests for the configured bit positions. This means selecting another two-button chord does not increase RamScript size.

Custom chords are structurally tested and generate valid builds for all four ROM profiles, but each new chord should still receive an in-game smoke test before being called runtime-validated. The default `R + SELECT` path retains the previous validation status.

Arbitrary payload lengths are supported: the bootstrap target is generated from the payload's actual aligned native-blob offset.
