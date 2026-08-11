# Composer design

This document records the intended direction for a future graphical event editor.

## Two abstraction levels

### High-level recipes

Examples:

```text
Give one-time item
Display message only
Official event reference
future: set flag + message
future: scripted battle
```

A recipe exposes user-friendly parameters and internally expands to multiple Field Script commands.

`ItemGiftPreset` is the first concrete example.

### Low-level actions

These correspond closely to methods on `RamScriptBuilder`.

Examples:

```text
lock
facePlayer
checkFlag
vGotoIf
vMessage
checkItemSpace
giveItem
setFlag
release
end
```

The GUI should not expose raw opcode bytes for normal usage.

## Important separation

A graphical composer should generate a standalone `event.bin` first.

Attaching it to a visual Wonder Card is a separate operation:

```text
composer -> event.bin
wc3-builder -> design.wc3
inject-bin -> final.wc3
```

This prevents event logic from becoming entangled with card appearance.

## Native code

`callNative` and `gotoNative` are low-level primitives, but large/custom native payloads belong to `native-ramscript-tools`, not to the normal composer.

## Pokémon commands

`giveEgg`/`giveMon` should remain in an advanced/experimental category until their full generated-Pokémon semantics are deliberately modeled.


## Repeatability as a first-class GUI option

For item gifts, the GUI should expose:

```text
Gift behavior

(●) One time
    receipt flag: [0x2AA]

( ) Repeatable
    no receipt flag used
```

"Repeatable" must change the generated script itself rather than merely hiding
a flag field: no `checkFlag` and no `setFlag` should be emitted.

A low-level `Clear Flag` action also remains available for advanced custom events.
