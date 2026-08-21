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

## Native helper installer / Party IV Viewer

Automated tests also cover:

- `NativeHelperInstaller.AUTO` chooses direct `setptr` for a representative
  20-byte helper;
- AUTO chooses CpuSet block installation for the Party IV helper;
- raw helper data is skipped by relocatable `vgoto` and embedded word-aligned;
- CpuSet copier word count and relocatable source literal are correct;
- Party IV helper literals resolve to the expected FR/LG symbols;
- Party IV Viewer uses the dynamic nickname message source;
- all four ROM profiles fit inside the 995-byte RamScript limit.

Manual validations:

- direct-install Party IV Viewer: FireRed English 1.0, passed;
- CpuSet compact Party IV Viewer: FireRed English 1.0, passed;
- nickname-enabled Party IV Viewer: pending emulator test.

## 2026-08-19 Party IV Viewer V1 closeout

Validated in-game on FireRed English 1.0:

- CpuSet block installer transport;
- nickname display;
- continuous field message box across the whole party;
- stock prompt-clear/down-arrow pagination;
- final close only after the last occupied party slot.

Current continuous build: 495-byte payload, 861 / 995 total with default HotkeyRuntimeV1, 134 bytes free.

## Configurable hotkey checks

The default `R + SELECT` API path remains byte-for-byte identical to the previously validated C5a runtime. Tests also verify custom two-button wrappers, parsing, checksum validity, and size neutrality.

Custom chord semantics are ordered: first button held, second button newly pressed. Example: `r-b` means hold R and press B.

Non-default chords still require an in-game smoke test before being marked runtime-validated.


## 2026-08-21 Multi-hotkey V1 closeout

Validated in-game on FireRed English 1.0:

- one shared runtime;
- `R + SELECT` -> Seed Modifier;
- `R + B` -> Repel Hotkey;
- both independent payloads execute correctly;
- normal movement, Options and map transitions remain stable.

The earlier 72-byte wrapper is rejected because it crossed the exact
`UnusedVarNeededToMatch[8]` boundary at `03005330` and overwrote live
`gCanvas*` state. Two generic RamScript-native dispatcher experiments are also
rejected after input swallowing / freeze failures.

The compact MultiHotkeyRuntimeV1 remains the validated two-binding path and is
not to be replaced automatically by a future generic implementation.
