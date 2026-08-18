# Compact Runtime Candidate 4

Candidate 4 keeps the validated C3 resident runtime and compact 12-block
installer, but reduces the Field Script bootstrap from 32 native bytes to 20.

## Why a bootstrap still exists

The event engine has:

```text
callnative <absolute address>
```

but it has no relocatable `vcallnative`.

`setvaddress` only makes the `v*` script-control-flow commands relocatable.
Therefore Field Script still needs a tiny native entry point at a fixed IWRAM
address before execution can move into native code stored inside the relocated
Wonder Card RamScript.

## C3 bootstrap

C3 used 32 bytes to:

```text
GetSavedRamScriptIfValid()
→ recover current script pointer
→ decode installer offset
→ find native blob
→ BX native blob
```

## C4 observation

`setvaddress` already did the relocation work.

The game stores:

```text
sAddressOffset = virtualBase - physicalScriptBase
```

and `vgoto/vcall` resolve saved-script pointers by subtracting that value.

For the supported layouts:

```text
sAddressOffset = 020370A8
```

So C4 only needs:

```text
offset = *sAddressOffset
target = virtualNativeBlobThumb - offset
BX target
```

The bootstrap is 20 bytes:

```text
ldr  r0, =020370A8
ldr  r0, [r0]
ldr  r1, =virtualNativeBlobThumb
subs r1, r1, r0
bx   r1
nop
<two 32-bit literals>
```

Because `BX` preserves LR, the native installer no longer relies on a bootstrap
`push {lr}`. It finishes with `BX LR`, returning directly to `callnative`.

## Size

With the same 43-byte Hello payload:

```text
C3:
  bootstrap native bytes   32
  Field Script installer  198
  total                   481 / 995
  free                    514

C4:
  bootstrap native bytes   20
  Field Script installer  126
  total                   409 / 995
  free                    586
```

Runtime overhead falls from 438 to 366 bytes.

## Expected behavior

Identical to validated C3:

```text
deliveryman installs runtime
callback1 = 03005311
VBlank    = 03003F43

R+SELECT
→ Hello from the Wonder Card!
```

The C3 stress/safety/reinstall suite should remain unchanged.
