/*
   Generic object-bound RamScript continuation bridge.

   Unlike the Deliveryman form, an object-bound RamScript has real
   mapGroup/mapNum/objectId metadata, so GetSavedRamScriptIfValid() rejects it.
   The bridge instead calls stock GetRamScript(localId, NULL) after each
   ReturnToField relocation, then resumes the current physical RamScript.
*/
final class ObjectEventRamScriptContinuationRuntime {
    static final int CONTINUATION_IMMEDIATE_OFFSET = 0x0E;
    private ObjectEventRamScriptContinuationRuntime() {}

    static byte[] callback(RomProfile rom, int continuationOffset, int localId) {
        if (continuationOffset < 0 || continuationOffset > 0xFF)
            throw new IllegalArgumentException("Continuation offset must fit Thumb adds immediate");
        if (localId < 0 || localId > 0xFF)
            throw new IllegalArgumentException("localId must fit in one byte");

        byte[] out = new byte[TradeEvolutionContinuationRuntime.CALLBACK_SIZE];
        putU16(out, 0x00, 0xB500);              // push {lr}
        putU16(out, 0x02, 0x2000 | localId);    // r0 = localId
        putU16(out, 0x04, 0x2100);              // r1 = NULL original script
        putThumbBl(out, 0x06, 0x18);            // GetRamScript thunk
        putU16(out, 0x0A, 0x2800);              // cmp r0,#0
        putU16(out, 0x0C, 0xD002);              // beq return TRUE
        putU16(out, 0x0E, 0x3000 | continuationOffset);
        putThumbBl(out, 0x10, 0x1C);            // ScriptContext_SetupScript thunk
        putU16(out, 0x14, 0x2001);              // callback complete = TRUE
        putU16(out, 0x16, 0xBD00);
        putU16(out, 0x18, 0x4B0B);              // same audited thunk layout as production bridge
        putU16(out, 0x1A, 0x4718);
        putU16(out, 0x1C, 0x4B0B);
        putU16(out, 0x1E, 0x4718);
        return out;
    }

    static byte[] callbackLiterals(RomProfile rom) {
        byte[] out = new byte[8];
        // GetRamScript is 0xBC bytes before GetSavedRamScriptIfValid in all four
        // supported FR/LG profiles; this relationship is symbol-verified.
        putU32(out, 0, rom.getSavedRamScriptThumb - 0xBCL);
        putU32(out, 4, rom.scriptContextSetupThumb);
        return out;
    }

    private static void putThumbBl(byte[] data, int sourceOffset, int targetOffset) {
        int pc = sourceOffset + 4;
        int delta = targetOffset - pc;
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
