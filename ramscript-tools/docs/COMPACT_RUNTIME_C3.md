# Compact Runtime Candidate 3

C3 combines the validated C2a compact resident copy with the real Runtime v1
activation path and the known-good visual hotkey payload.

## Script layout for the Hello test

```text
+0x00..09  setvaddress + vgoto installer
+0x0A..0B  A7 00 Runtime v1 signature
+0x0C..36  43-byte self-contained Hello Field Script payload
+0x37      1 alignment byte
+0x38..    227-byte aligned compact native installer blob
+after     Field Script bootstrap installer
```

The native blob is aligned to 4 bytes, preserving the C2a ADR fix.

## Native installer

The 227-byte blob consists of:

```text
56 bytes   native copier + VBlank activation + literals
48 bytes   12 compact block records
123 bytes  exact validated Runtime v1 resident image
```

After all 123 resident bytes have been copied, the native code performs one
atomic 32-bit write:

```text
gMain.vblankCallback = 03003F43
```

It intentionally does NOT write callback1 directly.

The validated supervisor sees the normal `CB1_Overworld` pointer on a later
VBlank and changes it to:

```text
03005311
```

This preserves the important Runtime v1 rule that the final wrapper is fully
present before callback1 can point to it.

## Expected FR/LG 1.0 state

After talking to the deliveryman and allowing at least one VBlank:

```text
03003550 = 03003F43
030030F0 = 03005311
```

Resident blocks must remain byte-identical to C2a.

Then normal overworld:

```text
R + SELECT
→ Hello from the Wonder Card!
```

The RC3 safety gate remains part of the copied resident image, therefore the
hotkey must still be ignored during NPC dialogue, YES/NO prompts and trainer
pre-battle dialogue.

## Size

For the 43-byte Hello payload:

```text
payload             43 bytes
runtime overhead    438 bytes
total               481 / 995
free                514 bytes
```

This is the first apples-to-apples comparison with the old trigger-v1 build:

```text
old byte-at-a-time runtime: 943 / 995 for Hello
compact C3:                 481 / 995 for Hello
```

The next optimization, only after C3 runtime validation, is reducing the
198-byte Field Script cost of installing the 32-byte bootstrap.
