# Show Secret ID preset

`ShowSecretIdPreset` is the first production example of a hybrid RamScript that
does **not** need a hotkey.

Execution:

```text
talk to deliveryman
→ Field Script stages a 20-byte Thumb helper
→ callnative helper
→ helper reads Secret ID
→ helper stores u16 in VAR_RESULT
→ Field Script buffers VAR_RESULT as decimal text
→ message box
```

## FR/LG data used

`SaveBlock2.playerTrainerId` begins at offset `0x0A` and is four bytes long.

The preset reads the upper halfword:

```text
SaveBlock2 + 0x0C
```

and treats it as the Secret ID.

The native helper writes the value to:

```text
gSpecialVar_Result / VAR_RESULT
```

Then the stock Field Script command `buffernumberstring` formats that u16 into
`STR_VAR_1`.

## Native helper

Temporary staging:

```text
03005310
```

Thumb entry:

```text
03005311
```

Size:

```text
20 bytes
```

The helper installs no callback, VBlank handler, or persistent trigger.

## Supported profiles

The currently tracked symbols are the same in:

```text
FR 1.0
LG 1.0
FR 1.1
LG 1.1
```

for:

```text
gSaveBlock2Ptr       0300500C
gSpecialVar_Result   020370D0
```

The preset still receives a `RomProfile` so future profile differences do not
require changing its public API.

## CLI

Binary:

```powershell
java -cp out Main build-show-secret-id-bin `
  fr10 `
  ../dummy-saves/show-secret-id.bin
```

WC3:

```powershell
java -cp out Main build-show-secret-id-wc3 `
  fr10 `
  ../dummy-saves/custom-design.wc3 `
  ../dummy-saves/show-secret-id.wc3
```

Expected in game:

```text
Your Secret ID is <value>.
```

`buffernumberstring` does not zero-pad to five digits. For example, Secret ID
`00123` is displayed as `123`. This can be improved later if fixed-width SID
formatting is desired.
