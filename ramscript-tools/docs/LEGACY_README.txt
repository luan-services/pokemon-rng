RamScript Tools v5

Compile:
    javac -encoding UTF-8 -d out src\*.java

Inspect:
    java -cp out Main inspect input.wc3
    java -cp out Main inspect-bin input.bin

Import / export:
    java -cp out Main extract-bin input.wc3 output.bin
    java -cp out Main inject-bin input.wc3 input.bin output.wc3

This makes the concepts explicit:
- WC3 = Wonder Card container/design + RamScript
- BIN = only the 0x3EC-byte RamScript

RamScriptBuilder v5 adds wrappers for:
- relocatable control flow: vGoto, vCall, vGotoIf, vCallIf
- variables: setVar, addVar, subVar, copyVar, compareVarToVar
- flags: setFlag, clearFlag, checkFlag
- items: addItem, removeItem, checkItem, checkItemSpace, giveItem
- Pokemon: giveEgg, giveMon, bufferSpeciesName, bufferItemName
- money: addMoney, removeMoney, checkMoney
- specials: special, specialVar
- native primitives: callNative, gotoNative
- sound/UI/field: delay, playSe, playFanfare, fadeScreen, weather
- scripted wild battles: setWildBattle, doWildBattle

Important:
callNative/gotoNative use absolute function addresses. Those addresses can depend
on game, language and ROM revision/build. Prefer normal script commands and
special/specialVar until working with a verified target ROM.

The existing Rare Candy custom event remains available:
    java -cp out Main build-custom-test-bin custom.bin
    java -cp out Main inject-bin design.wc3 custom.bin final.wc3
