# Dispatcher Candidate 5

## Goal

Prove that the persistent normal-context dispatcher can make a normal ROM
function call, receive a return value, and store that return value for
inspection.

The action is:

```text
R + SELECT
    ↓
GetSavedRamScriptIfValid()
    ↓
r0 = current RamScript script pointer (or NULL)
    ↓
store r0 at 03003FA4
```

FireRed 1.0:

```text
GetSavedRamScriptIfValid = 08069E48
Thumb pointer             = 08069E49
```

## Expected pointer

`SaveBlock1` places `ramScript` at offset `0x361C`.

Inside `RamScript`:

```text
+0 checksum (4 bytes)
+4 RamScriptData header
   magic
   mapGroup
   mapNum
   objectId
+8 script[]
```

Therefore:

```text
returned script pointer
= *gSaveBlock1Ptr + 0x3624
```

`gSaveBlock1Ptr` itself is stored at:

```text
03005008
```

Example only:

```text
03005008 -> 02025534

expected result:
02025534 + 3624 = 02028B58
```

Use the actual pointer from your current run.

## Why this call is different from Candidate 4

Candidate 4 tail-called `PlaySE` and did not need the return value.

Candidate 5 needs `r0` after the ROM function returns. The trigger therefore
uses a small call/return trampoline:

```text
push incoming LR
set LR = local continuation|1
BX GetSavedRamScriptIfValid

continuation:
store r0
pop PC
```

This keeps the original callback return address intact.

## Trigger-frame behavior

Like Candidate 4, `CB1_Overworld` is skipped on the trigger frame. The normal
callback path resumes next frame.

## Safety scope

Offline experiment only.

The experimental runtime occupies globals associated with the link subsystem.
Do not test cable, wireless, Union Room, or link battles.
