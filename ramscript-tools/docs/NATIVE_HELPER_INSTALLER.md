# Native Helper Installer

`NativeHelperInstaller` centralizes how small Thumb helpers are staged from a
RamScript payload into writable RAM.

It does **not** own triggers or hotkeys. A preset supplies a `NativeHelper` and
the installer only chooses how to materialize the helper bytes in RAM.

## Modes

### DIRECT_SET_PTR

Each helper byte is emitted as one stock Field Script `setptr` command.

- 1 helper byte -> 6 RamScript bytes
- no secondary copier is required
- best for small helpers

This is the original helper-install method and remains supported.

### CPU_SET_BLOCK

The helper is stored as raw inert bytes inside the relocatable RamScript. A
`vgoto` skips over the raw block during Field Script execution. A 28-byte Thumb
copier is installed with `setptr`, and that copier uses GBA BIOS `CpuSet`
(`SVC 0x0B`) in 32-bit mode to copy the whole helper block into RAM.

This pays a fixed bootstrap cost but avoids the 6x expansion of larger helpers.

The CpuSet copier was validated in-game on FireRed English 1.0 with the Party IV
Viewer on 2026-08-19. The later nickname-enabled and continuous-message viewers were also validated on FR1.0; those builds use the same `NativeHelperInstaller` / CpuSet transport.

### AUTO

`AUTO` calculates the exact encoding cost at the current builder position and
selects the smaller valid representation.

The current decision includes:

- direct: `helperSize * 6`
- CpuSet: `vgoto + alignment + padded raw helper + 28-byte copier encoded via setptr + callnative copier`

The caller does not need to hard-code a byte threshold. Small helpers naturally
remain direct; larger helpers can switch to CpuSet when it saves space.

Tests explicitly verify that a 20-byte helper selects `DIRECT_SET_PTR` while the
Party IV Viewer's larger helper selects `CPU_SET_BLOCK`. The current continuous-message
Party IV helper is 296 bytes and remains on the block-copy path.

## Why keep both?

CpuSet is not automatically smaller for every helper. The copier itself must be
bootstrapped first, so using it for very small helpers can cost more than direct
`setptr` installation. Keeping both modes lets presets use the simpler encoding
when it is already optimal.

## Safety

The helper's behavior is unchanged by the installation mode. `HotkeyRuntimeV1`
is also unchanged. The installer only changes how native bytes move from the
RamScript into the helper's staging address.

## Persistent-placement alignment

`CPU_SET_BLOCK` has an additional requirement when the containing Field Script is moved out of the normally aligned RamScript area: the **Field Script base itself must be word-aligned**. The installer aligns the embedded raw helper relative to the script, but final physical alignment is `scriptBase + rawOffset`.

`NativeHelperInstaller.Plan.requiredScriptBaseAlignment()` reports this requirement (`4` for `CPU_SET_BLOCK`, `1` for `DIRECT_SET_PTR`). Persistent placement must honor it. Build 24 validated this rule in-game with the shared Party IV Viewer after the previous bridge at `SB2+0x0D5F` produced a misaligned CpuSet source.
