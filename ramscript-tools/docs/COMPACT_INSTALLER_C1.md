# Compact Installer Candidate 1

This candidate tests only the compact transport mechanism. It does not install
the R+SELECT runtime.

The RamScript contains the validated 14-byte supervisor as raw data at
`script + 0x0A`. The deliveryman installs a temporary 40-byte Thumb copier at
`03005310`, calls it, and the copier resolves the CURRENT RamScript with
`GetSavedRamScriptIfValid()`.

It then copies:

```text
source:      current RamScript + 0x0A
destination: 03003F42
size:        14 bytes
```

After talking to the deliveryman, RAM must contain:

```text
03003F42  18 A3 07 CB
03003F46  03 68 8B 42
03003F4A  B3 D1 02 60
03003F4E  B1 E7
```

Contiguous:

```text
18 A3 07 CB 03 68 8B 42 B3 D1 02 60 B1 E7
```

No VBlank hook or callback hook is installed, so R+SELECT intentionally does
nothing in Candidate 1.

C1 is not expected to be smaller than direct setptr for one tiny block because
the 40-byte copier itself is still installed byte-by-byte. Its cost is meant to
be amortized in Candidate 2 when a single copier installs all resident runtime
blocks from a compact table.
