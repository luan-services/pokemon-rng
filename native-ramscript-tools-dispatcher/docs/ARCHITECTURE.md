# Architecture

## Separation of responsibilities

The native toolchain has five concepts.

### ROM profile

`RomProfile` contains game-build-dependent symbols/addresses.

Current profile:

```text
fr10 = FireRed English 1.0
```

A profile existing in Java does not automatically mean an effect is validated
for that build.

### Native effect

`NativeEffect` is the user-facing effect abstraction.

Current implementation:

```text
SeedHotkeyEffect
```

It delegates to the frozen `FixedRamSeedModifier`.

### Trigger

`Hotkey` describes the high-level trigger independently from the effect.

Current seed trigger:

```text
held:    R
pressed: SELECT
```

The current machine code is still specialized for this combination; `Hotkey`
is framework metadata for future work, not a generic assembler yet.

### Runtime layout

`NativeRuntimeLayout` describes known runtime/staging regions.

It is documentation/inspection metadata. It does not grant arbitrary payloads
permission to reuse those addresses.

### Installer

The Wonder Card RamScript remains the delivery mechanism.

Its job is to copy the fixed runtime bytes and redirect the VBlank slot last.

After that, the Wonder Card's physical SaveBlock address is irrelevant to the
known-good runtime.

## Frozen implementation boundary

The following classes are considered part of the known-good byte-generation
path:

```text
FixedRamSeedModifier
FieldScriptWriter
RamScript
Crc16
RngMath
RomProfile
Binary
```

Refactoring them is allowed only when the golden payload remains byte-identical
or when the change is intentionally promoted to a new manually-tested candidate.
