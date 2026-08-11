# WC3 format notes

The project treats a WC3 as exactly `0x58C` bytes.

```text
0x000                  card CRC storage
0x004                  WonderCard structure begins
0x150-ish              WonderCard metadata area
0x1A0                  RamScript begins
0x58C                  end of file
```

## WonderCard structure

`WonderCard.SIZE = 0x14C`.

The builder currently exposes:

```text
flagId
iconSpecies
idNumber
type
backgroundType
sendType
maxStamps
title
subtitle
body[4]
footerLine1
footerLine2
```

Each visible card text field occupies 40 encoded bytes.

## Card CRC

The CRC stored at the beginning of the WC3 covers the `WonderCard` structure, not the complete file.

Every `edit` and `create` operation recalculates it.

## Icon metadata mirror

The card contains `WonderCard.iconSpecies`, but the WC3 card metadata also stores an icon species value.

The builder updates both through `Wc3File.setIconSpecies()`.

## RamScript isolation

Existing WC3 files:

```text
edit
→ card fields change
→ card CRC changes
→ bytes from 0x1A0 onward remain untouched
```

A newly-created WC3 has no preexisting RamScript, so `create` inserts one built-in placeholder script.

That script is an implementation detail of the base template, not an attempt to merge wc3-builder with ramscript-tools.
