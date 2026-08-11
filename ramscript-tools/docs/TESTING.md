# Testing

Compile and run:

```cmd
javac -encoding UTF-8 -d out src\*.java tests\TestRunner.java
java -cp out TestRunner
```

Automated tests cover:

- official Aurora/Mystic builders produce valid Wonder Card RamScripts;
- custom Rare Candy preset produces a valid RamScript;
- generic item-gift preset produces a valid RamScript;
- RamScript BIN write/read round-trip;
- WC3 `extract-bin`/`inject-bin` style replacement preserves the card/design region;
- custom text is encoded into generated item gifts;
- oversized script data is rejected;
- builder catalog is available for future UI discovery.

Recommended manual integration:

1. build `event.bin`;
2. inspect it;
3. inject it into a WC3 created/edited with `wc3-builder`;
4. inject WC3 into a save with `wc3-injector`;
5. test the deliveryman in emulator;
6. extract the event again and inspect/disassemble it.


## Additional v1 cases

- repeatable item-gift script contains no `checkflag` opcode;
- repeatable item-gift script contains no `setflag` opcode;
- repeatable gift still checks bag space and gives the item;
- clear-flag utility emits `clearflag`;
- clear-flag utility can optionally display a confirmation message.

Manual emulator test for repeatable gifts:

1. use a WC3 whose `flagId` maps to an unset Wonder Card received-gift flag;
2. attach a repeatable item gift;
3. inject into a save;
4. receive the item;
5. leave/re-enter the Pokémon Center or otherwise refresh the map;
6. confirm the deliveryman remains available;
7. receive the item again.
