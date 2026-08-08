RamScript Tools v2

Java 21, no Maven required.

Compile from the project root:

    javac -encoding UTF-8 -d out src\*.java

Inspect a WC3:

    java -cp out Main inspect ..\dummy-saves\example-wc.wc3

Inspect an extracted 0x3EC-byte RamScript:

    java -cp out Main inspect-bin path\ramscript.bin

Version 2 additions:
- Resolves item ids to ITEM_* names.
- Resolves known flags to FLAG_* names.
- Resolves variable ids to VAR_* names.
- Resolves callstd ids to STD_* / MSGBOX_* names.
- Decodes embedded Gen III text referenced by vmessage.
- Keeps raw bytes and virtual pointer offsets visible for analysis.

The disassembler intentionally follows known code paths instead of decoding all
995 script bytes linearly, because official RamScripts contain embedded text/data
after and between executable paths.
