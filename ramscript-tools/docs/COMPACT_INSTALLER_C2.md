# REJECTED

Candidate 2 placed its native installer blob at `script+0x0A`. Because the saved `script[]` begins 4-byte aligned, this put the blob at an address congruent to 2 modulo 4. Its Thumb ADR instructions therefore resolved table/data addresses two bytes incorrectly and caused memory corruption. Use Candidate 2a.

# Compact Installer Candidate 2

Candidate 2 copies the COMPLETE resident Runtime v1 image into IWRAM using one
compact native installer, but intentionally does not activate any hooks.

## Layout

```text
RamScript + 0x00  setvaddress
RamScript + 0x05  vgoto Field Script installer
RamScript + 0x0A  native compact installer blob (211 bytes)
                  ├─ 40 bytes position-independent Thumb code
                  ├─ 48 bytes table (12 records)
                  └─ 123 bytes exact Runtime v1 resident data

after blob        Field Script installer
```

The Field Script installer only writes a 32-byte bootstrap at `03005310` and
calls it.

The bootstrap:

```text
GetSavedRamScriptIfValid()
→ reads installerOffset from the vgoto pointer at script+6
→ currentScript + installerOffset
→ subtracts 211
→ obtains current native-blob address
→ branches into the blob in Thumb state
```

Therefore the 211-byte installer executes directly from the relocated
RamScript in EWRAM. It never needs to be expanded byte-by-byte into IWRAM.

## Compact table

Each runtime block costs only four metadata bytes:

```text
u16 destination low halfword
u16 size
```

All current destinations are `0300xxxx`, so the installer constructs the
shared `03000000` base once.

The 123 resident bytes are stored once, contiguously.

## Important self-overwrite detail

The temporary bootstrap initially lives at:

```text
03005310..0300532F
```

which is also the final 32-byte Runtime v1 wrapper location.

This is safe in C2 because the bootstrap has already transferred execution to
the native blob inside the RamScript before the wrapper is copied. The wrapper
is deliberately the LAST block copied.

## Candidate 2 does NOT hook the game

After deliveryman execution:

```text
all Runtime v1 resident blocks → installed
gMain.callback1               → unchanged
gMain.vblankCallback          → unchanged
R+SELECT                      → inactive
```

This isolates the question:

> Can the compact installer reproduce the known-good resident memory image?

## Expected watch values for FR/LG 1.0

Key blocks:

```text
03003EB4 = 47184B02
03003EC0 = 08000725

03003F42 = CB07A318
03003F46 = 428B6803
03003F4A = 6002D1B3
03003F4E = E7B1

03003F98 = 47204C00
03003F9C = 08069E49

03005032 = 21D9300C
03005036 = 1A640089
0300503A = FFAEF7FE
0300503E = E026

03005082 = F7FEB510
03005086 = 2800FF88
0300508A = E18CD000
0300508E = BD10

03005310..0300532F = final 32-byte wrapper
030053A8..030053AF = format validator
03005434..0300543F = safety gate
```

Also verify:

```text
030030F0 = 08056535
03003550 = 08000725
```

for FR/LG 1.0. Those two MUST remain original in Candidate 2.

## Size

C2 is expected to be around 419 RamScript bytes total, versus roughly 900
bytes of overhead in the byte-at-a-time Runtime v1 installer.

Candidate 3 will add hook activation and the real hotkey payload only after C2
is confirmed byte-for-byte in RAM.
