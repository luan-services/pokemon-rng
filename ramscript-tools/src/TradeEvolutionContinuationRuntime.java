/*
   Dedicated relocation-safe continuation bridge used by TradeEvolutionPreset.

   This is intentionally NOT the shared hotkey runtime. The preset owns the
   validated 32-byte WRAPPER IWRAM slot for the duration of the interaction.
   It never installs a VBlank hook or callback1 listener.

   A ReturnToField transition can relocate SaveBlock1, invalidating a physical
   ScriptContext pointer into the saved RamScript. The callback therefore:
     1. resolves the current RamScript with GetSavedRamScriptIfValid();
     2. adds a logical continuation offset;
     3. starts that current address with ScriptContext_SetupScript().
*/
final class TradeEvolutionContinuationRuntime {
    static final long CALLBACK = RuntimeV1ResidentBlocks.WRAPPER; // validated 03005310..0300532F
    static final long LITERAL_GET_RAM_SCRIPT = 0x03005358L;
    static final long LITERAL_SETUP_SCRIPT = 0x0300535CL;

    static final int CALLBACK_SIZE = 32;
    static final int CONTINUATION_IMMEDIATE_OFFSET = 0x0A;
    static final int LAUNCHER_SIZE = 32;

    private TradeEvolutionContinuationRuntime() {}

    static byte[] callback(RomProfile rom, int continuationOffset) {
        if (continuationOffset < 0 || continuationOffset > 0xFF)
            throw new IllegalArgumentException("Continuation offset must fit Thumb adds immediate: " + continuationOffset);

        byte[] out = new byte[CALLBACK_SIZE];
        putU16(out, 0x00, 0xB500);              // push {lr}
        putThumbBl(out, 0x02, 0x18);            // GetSavedRamScriptIfValid thunk
        putU16(out, 0x06, 0x2800);              // cmp r0,#0
        putU16(out, 0x08, 0xD004);              // beq fail
        putU16(out, 0x0A, 0x3000 | continuationOffset);
        putThumbBl(out, 0x0C, 0x1C);            // ScriptContext_SetupScript thunk
        putU16(out, 0x10, 0x2001);              // callback complete = TRUE
        putU16(out, 0x12, 0xBD00);
        putU16(out, 0x14, 0x2001);              // fail safely
        putU16(out, 0x16, 0xBD00);
        putU16(out, 0x18, 0x4B0B);              // literal @ 03005358
        putU16(out, 0x1A, 0x4718);
        putU16(out, 0x1C, 0x4B0B);              // literal @ 0300535C
        putU16(out, 0x1E, 0x4718);
        return out;
    }

    static byte[] callbackLiterals(RomProfile rom) {
        byte[] out = new byte[8];
        putU32(out, 0, rom.getSavedRamScriptThumb);
        putU32(out, 4, rom.scriptContextSetupThumb);
        return out;
    }

    static byte[] launcher(RomProfile rom) {
        byte[] out = new byte[LAUNCHER_SIZE];
        putU16(out, 0x00, 0xB500); // push {lr}
        putU16(out, 0x02, 0x2003); // PARTY_MENU_TYPE_FIELD / single choose-and-close path
        putThumbBl(out, 0x04, 0x10);
        putU16(out, 0x08, 0x4903); // =gFieldCallback2
        putU16(out, 0x0A, 0x4804); // =CALLBACK|1
        putU16(out, 0x0C, 0x6008);
        putU16(out, 0x0E, 0xBD00);
        putU16(out, 0x10, 0x4B00); // ChoosePartyMonByMenuType thunk
        putU16(out, 0x12, 0x4718);
        putU32(out, 0x14, rom.choosePartyMonByMenuTypeThumb);
        putU32(out, 0x18, rom.fieldCallback2);
        putU32(out, 0x1C, CALLBACK | 1L);
        return out;
    }

    private static void putThumbBl(byte[] data, int sourceOffset, int targetOffset) {
        int pc = sourceOffset + 4;
        int delta = targetOffset - pc;
        if ((delta & 1) != 0 || delta < -(1 << 22) || delta >= (1 << 22))
            throw new IllegalArgumentException("Thumb BL target out of range/alignment");
        int hi = (delta >> 12) & 0x7FF;
        int lo = (delta >> 1) & 0x7FF;
        putU16(data, sourceOffset, 0xF000 | hi);
        putU16(data, sourceOffset + 2, 0xF800 | lo);
    }

    private static void putU16(byte[] d, int o, int v) {
        d[o] = (byte)v; d[o + 1] = (byte)(v >>> 8);
    }

    private static void putU32(byte[] d, int o, long v) {
        d[o] = (byte)v; d[o + 1] = (byte)(v >>> 8); d[o + 2] = (byte)(v >>> 16); d[o + 3] = (byte)(v >>> 24);
    }
}
