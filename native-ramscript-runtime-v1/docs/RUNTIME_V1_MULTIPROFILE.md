# Native RamScript Runtime v1 — Multi-profile

This package extracts the corrected RC4a architecture into a profile-driven
runtime.

## Profiles

| ID | ROM | Status |
|---|---|---|
| `fr10` | FireRed English 1.0 | runtime-validated |
| `lg10` | LeafGreen English 1.0 | symbol-verified; runtime test pending |
| `fr11` | FireRed English 1.1 | symbol-verified; ROM runtime untested |
| `lg11` | LeafGreen English 1.1 | symbol-verified; ROM runtime untested |

## Symbol-derived ROM differences

Revision 1.0:

```text
VBlankIntr                   08000724 -> Thumb 08000725
CB1_Overworld                08056534 -> Thumb 08056535
ScriptContext_SetupScript    08069AE4 -> Thumb 08069AE5
GetSavedRamScriptIfValid     08069E48 -> Thumb 08069E49
```

Revision 1.1:

```text
VBlankIntr                   08000738 -> Thumb 08000739
CB1_Overworld                08056548 -> Thumb 08056549
ScriptContext_SetupScript    08069AF8 -> Thumb 08069AF9
GetSavedRamScriptIfValid     08069E5C -> Thumb 08069E5D
```

The compact bridge remains valid because:

```text
GetSavedRamScriptIfValid - ScriptContext_SetupScript = 0x364
```

for all four supplied symbol maps.

## Shared RAM layout

The supplied FR/LG 1.0 and 1.1 `.sym` files agree on the IWRAM layout used by
the runtime, including:

```text
sLockFieldControls          03000F9C
gLinkFiller1                03003EB4
gLinkFiller2                03003EC0
gLinkFiller4                03003F98
gLinkFiller5                03003F9C
UnusedVarNeededToMatch      03005310..0300532F
format-validator padding    030053A8..030053AF
safety-gate padding         03005434..0300543F
```

## Commands

```text
java -cp out Main build-runtime-v1 fr10 output.bin
java -cp out Main build-runtime-v1 lg10 output.bin
java -cp out Main build-runtime-v1 fr11 output.bin
java -cp out Main build-runtime-v1 lg11 output.bin
```

FR10/LG10 intentionally build byte-identically.
FR11/LG11 intentionally build byte-identically.

## Validation policy

Do not advertise the 1.1 profiles as tested merely because the symbols match.
They are included because the port is mechanical and statically audited, but
they still need a real ROM/emulator runtime pass.

LeafGreen 1.0 should use the same regression suite already validated on FR10:
installation, reinstall, first-Options path, NPC/YES-NO/trainer safety gate,
maps, Bag, Party, battle, healing, and repeated R+SELECT.
