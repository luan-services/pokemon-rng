# ramscript-tools

Java 21 tools for inspecting, exporting, importing, assembling, and composing **normal FireRed/LeafGreen Field Script RamScripts** used by Wonder Cards.

This is the event-behavior layer of the project:

```text
wc3-builder
    card design / metadata

ramscript-tools
    normal Field Script event behavior

native-ramscript-tools
    native Thumb/ARM payloads

wc3-injector
    WC3 <-> save integration
```

The original v5 README is preserved unchanged in `docs/LEGACY_README.txt`.

## Core idea

The project is designed around two ways of building an event.

### Presets

Ready-made event recipes:

```text
Aurora Ticket
Mystic Ticket
Rare Candy test
one-time item gift (parametric)
```

These are useful both from the CLI and from a future GUI.

### Building blocks

`RamScriptBuilder` exposes game script-engine operations such as:

```text
lock
facePlayer
message
giveItem
checkFlag
setFlag
variables
conditional branches
sound
weather
wild battles
specials
...
```

Run:

```cmd
java -cp out Main commands
```

to list the currently exposed high-level blocks.

A future graphical editor can represent these as selectable actions while continuing to use the same Java builder underneath.

## Compile

```cmd
javac -encoding UTF-8 -d out src\*.java
```

## Inspect

Whole WC3:

```cmd
java -cp out Main inspect event.wc3
```

Standalone RamScript:

```cmd
java -cp out Main inspect-bin event.bin
```

Inspection shows the RamScript header/checksum and disassembles known executable control-flow paths.

Embedded text/data is intentionally not blindly interpreted as bytecode.

## Export only the event

```cmd
java -cp out Main extract-bin event.wc3 event.bin
```

The resulting file is exactly `0x3EC` bytes and contains only the RamScript.

This keeps:

```text
WC3 design
```

separate from:

```text
event behavior
```

## Inject an event into an existing WC3

```cmd
java -cp out Main inject-bin design.wc3 event.bin final.wc3
```

The card/design bytes before `0x1A0` are preserved.

The command is permissive: a nonstandard header or checksum generates warnings rather than blocking research/custom data.

## List presets

```cmd
java -cp out Main presets
```

## Build a named preset

Standalone event:

```cmd
java -cp out Main build-preset-bin aurora-ticket aurora.bin
```

Insert directly into an existing WC3:

```cmd
java -cp out Main build-preset-wc3 aurora-ticket design.wc3 final.wc3
```

Available names currently include:

```text
aurora-ticket
mystic-ticket
rare-candy-test
```

## Build a one-time item gift

This is the first generic high-level event intended for eventual GUI use.

Example: one Rare Candy, using unused receipt flag `0x2AA`:

```cmd
java -cp out Main build-item-gift-bin rare-candy.bin 0x44 1 0x2AA
```

Or inject it directly into a design:

```cmd
java -cp out Main build-item-gift-wc3 design.wc3 final.wc3 0x44 1 0x2AA
```

The generated event:

1. locks/faces the player;
2. checks the receipt flag;
3. shows an intro message;
4. checks bag space;
5. gives the requested item/amount using the game's normal obtain-item flow;
6. sets the receipt flag;
7. handles "already received" and "bag full" branches.

Custom messages are optional:

```cmd
java -cp out Main build-item-gift-bin gift.bin 0x44 1 0x2AA ^
  --intro "Hello, {PLAYER}!\nI have something for you." ^
  --success "Enjoy your gift!" ^
  --already "You already received this gift." ^
  --bag-full "Your BAG is full."
```

## Low-level / experimental areas

`giveEgg` and `giveMon` remain available in `RamScriptBuilder` because they are real script-engine commands.

They are **not** currently modeled as high-level "legitimate Pokémon generator" features. OT/ID/PID/met-data/IV legitimacy behavior has not been abstracted here.

Likewise:

```text
callNative
gotoNative
```

accept absolute function addresses and are therefore ROM-build dependent.

Normal custom events should prefer ordinary Field Script commands and `special`/`specialVar`.

## Architecture for a future graphical composer

The intended GUI flow is:

```text
New Event
   |
   +-- choose preset
   |      |
   |      +-- Give Item
   |      +-- Official reference
   |      +-- future recipes
   |
   `-- Custom Event
          |
          +-- Lock
          +-- Face Player
          +-- Message
          +-- Check Flag
          +-- Give Item
          +-- Set Flag
          +-- Branch
          +-- ...
                 |
                 v
          RamScriptBuilder
                 |
                 v
             event.bin
                 |
                 v
          inject into WC3
```

The GUI itself is not implemented yet. The cleaned v1 organizes the API so that adding it later does not require rewriting the bytecode assembler.

## Repeatable item gifts

A repeatable gift intentionally uses **no receipt flag inside the event script**:

```cmd
java -cp out Main build-repeatable-item-gift-bin repeatable.bin 0x44 1
```

or directly into an existing card design:

```cmd
java -cp out Main build-repeatable-item-gift-wc3 design.wc3 final.wc3 0x44 1
```

The generated executable path contains:

```text
lock
facePlayer
message
checkItemSpace
giveItem
success message
release
end
```

It contains **neither**:

```text
checkFlag
setFlag
```

This matters because FR/LG uses the Wonder Card's `flagId` to select a normal
received-gift flag. If the RamScript never sets that flag, the game continues
to consider the card's gift unreceived, so the deliveryman can remain available.

For example:

```text
Wonder Card flagId 1003
-> 1003 - WONDER_CARD_FLAG_OFFSET(1000) = index 3
-> sReceivedGiftFlags[3]
-> FLAG_WONDER_CARD_UNUSED_1 (0x2AA)
```

A repeatable event using a card with `flagId 1003` therefore must not set
`0x2AA` if it should remain repeatable.

This behavior should still be manually verified in emulator for each new
event recipe before calling that recipe stable.

## Clear a flag through an event

A tiny utility RamScript can clear one normal game flag:

```cmd
java -cp out Main build-clear-flag-bin clear.bin 0x2AA
```

Optional message:

```cmd
java -cp out Main build-clear-flag-bin clear.bin 0x2AA --message "Flag cleared."
```

Or inject directly:

```cmd
java -cp out Main build-clear-flag-wc3 design.wc3 output.wc3 0x2AA
```

This does **not** edit the `.sav` directly. The deliveryman executes the normal
Field Script `clearflag` command.

Be careful with arbitrary flags. Normal FR/LG flags control story progression,
NPCs, badges, items, and many other states. The command deliberately accepts
a raw flag because this is a research/custom-event tool, but the user is
responsible for choosing a safe target.

### Important deliveryman caveat

If the specific flag you want to clear is also the received-gift flag for the
currently installed Wonder Card and that flag is already set, the deliveryman
for that same card may no longer be available to run the clearing script.

In that situation, use a different card/available received-gift slot for the
clear-flag utility, or handle save editing separately in a future save tool.

