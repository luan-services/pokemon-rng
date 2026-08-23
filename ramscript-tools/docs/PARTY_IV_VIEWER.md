# Party IV Viewer

Compact party IV viewer preset.

## Trigger

`HotkeyRuntimeV1` with a configurable two-button chord. The default remains `R + SELECT`.

Two validated trigger/deployment paths now exist:

- standalone `HotkeyRuntimeV1`, preserving the original single-preset path;
- shared persistent deployment (Build 24), where `R+A` runs Party IV alongside `R+B` Repel and `R+SELECT` Seed through one shared runtime.

The standalone path remains first-class for simple/single-preset use. The shared path amortizes hotkey infrastructure across multiple presets.

## Display flow

The current build constructs one continuous Gen III text stream for the whole
party and opens the field message box only once.

For each occupied party slot it shows:

```
PIKACHU IVs:
HP 31 ATK 20 DEF 17
```

The stock `CHAR_PROMPT_CLEAR` control (`0xFB`, represented by `\\p` in
`Gen3TextCodec`) then shows the normal down-arrow prompt. A/B clears the text
inside the existing box and continues with:

```
SPA 31 SPD 22 SPE 30
```

Another prompt-clear is inserted before the next Pokemon, so the same physical
message box remains open while the viewer walks through the party. Only after
the final Pokemon does the text stream reach EOS and close normally.

The name is the Pokemon's actual nickname from `MON_DATA_NICKNAME`, not just the
species name.

## Native helper

The helper now builds the complete party message itself at
`gStringVar4 + 0x280`.

For each Pokemon it performs only three stock `GetMonData` operations:

1. `MON_DATA_SPECIES` to detect the end of the occupied party;
2. `MON_DATA_NICKNAME` to copy the nickname;
3. `MON_DATA_IVS` to obtain all six IVs packed into one `u32`.

The packed IV value is decoded as the stock layout uses it:

- bits 0..4: HP
- bits 5..9: ATK
- bits 10..14: DEF
- bits 15..19: SPE
- bits 20..24: SPA
- bits 25..29: SPD

Because every IV is 0..31, the helper contains a tiny decimal formatter that
writes one or two Gen III digit characters directly into the dynamic text.
This removes the old Field Script loop and the need to repeatedly use the three
script string-variable buffers.

Stock FR/LG declares `gStringVar4` as 1000 bytes. Starting the source at +0x280
leaves 360 bytes; the calculated worst case for six 10-character nicknames is
342 bytes including the final EOS.

## Native helper installation

The preset uses `NativeHelperInstaller` in `AUTO` mode.

The current continuous-message helper is 296 bytes, so AUTO selects
`CPU_SET_BLOCK` rather than direct byte-by-byte `setptr` installation.

The compact copier:

1. reads `sAddressOffset` initialized by `setvaddress`;
2. resolves the physical address of the raw helper embedded in the RamScript;
3. invokes GBA BIOS `CpuSet` (`SVC 0x0B`) in 32-bit mode;
4. copies the helper to its EWRAM execution address;
5. returns to the Field Script.

The copier is staged at `gStringVar4 + 0x100`, the helper at
`gStringVar4 + 0x140`, and the continuous message source starts at
`gStringVar4 + 0x280`.

`HotkeyRuntimeV1` is not modified by this mechanism.

## Size history

Historical first working viewer using direct helper installation:

- payload: 521 bytes
- total with runtime: 889 / 995 bytes
- free: 106 bytes

First validated CpuSet compact viewer, before nickname display:

- payload: 421 bytes
- total: 789 / 995 bytes
- free: 206 bytes
- validated in-game on FireRed English 1.0

Nickname build validated on FireRed English 1.0:

- payload: 479 bytes
- total: 845 / 995 bytes
- free: 150 bytes

Current continuous-message build:

- payload: 495 bytes
- `HotkeyRuntimeV1` overhead: 366 bytes
- total: 861 / 995 bytes
- free: 134 bytes

The continuous UI costs only 16 bytes more than the validated nickname build,
while moving all party formatting into one native helper and removing repeated
Field Script message/open/close logic.

## Validation

Validated in-game so far:

- original direct-install Party IV Viewer: FireRed English 1.0
- compact CpuSet Party IV Viewer without nickname: FireRed English 1.0
- compact CpuSet Party IV Viewer with nickname: FireRed English 1.0
- continuous-message/down-arrow Party IV Viewer: FireRed English 1.0

The continuous-message/down-arrow build is validated in-game on FireRed English 1.0. It also passes Java structural/checksum tests and generates for `fr10`, `lg10`, `fr11`, and `lg11`. The non-default configurable-hotkey path is size-neutral and structurally tested, but individual custom chords still require an in-game smoke test.

## CLI

```cmd
java -cp out Main build-party-iv-viewer-bin fr10 ../dummy-saves/party-ivs.bin
java -cp out Main build-party-iv-viewer-bin fr10 ../dummy-saves/party-ivs-rb.bin --hotkey r-b
java -cp out Main build-party-iv-viewer-wc3 fr10 ../dummy-saves/custom-design.wc3 ../dummy-saves/party-ivs.wc3
java -cp out Main build-party-iv-viewer-wc3 fr10 ../dummy-saves/custom-design.wc3 ../dummy-saves/party-ivs-rb.wc3 --hotkey r-b
```

Supported ROM profiles: `fr10`, `lg10`, `fr11`, `lg11`.


## Shared persistent validation (Build 24)

The Party IV native body remains 296 bytes and is stored in the SB2 native catalog. The shared path uses a persistent Field Script bridge that stages the body at `gStringVar4+0x140`, returns to the Field Script, then invokes the stock `callnative` command.

Build 24 fixed a concrete `CPU_SET_BLOCK` alignment bug by moving the bridge from `SB2+0x0D5F` to word-aligned `SB2+0x0D60`. It was then validated in-game on FireRed English 1.0, including multiple captured Pokemon and complete IV display for the party.

Stress-composition memory (Party IV + Seed + Repel): SB2 1022/1024 B, SB1 30 B gateways, shared RamScript runtime 419/995 B. This is **not** the intrinsic Party IV size; it includes catalog, two other presets, the persistent bridge and one alignment byte.
