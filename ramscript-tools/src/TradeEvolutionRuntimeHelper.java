/*
   Native helper for the production Trade Evolution preset.

   Responsibilities:
   - read the selected party slot from VAR_8004;
   - ask the stock evolution table for EVO_MODE_TRADE;
   - leave target=0 untouched (no scene / no corruption);
   - rearm the same dedicated continuation callback for the second ReturnToField;
   - set gCB2_AfterEvolution to stock CB2_ReturnToField;
   - call BeginEvolutionScene while preserving r3=partyId.
*/
final class TradeEvolutionRuntimeHelper {
    static final int CODE_SIZE = 104;

    private static final int POST_EVO_OFFSET_IMMEDIATE = 0x22;
    private static final int LIT_VAR_8004 = 0x48;
    private static final int LIT_PLAYER_PARTY = 0x4C;
    private static final int LIT_CALLBACK_BASE = 0x50;
    private static final int LIT_FIELD_CALLBACK2 = 0x54;
    private static final int LIT_AFTER_EVO_SLOT = 0x58;
    private static final int LIT_RETURN_TO_FIELD = 0x5C;
    private static final int LIT_GET_EVO = 0x60;
    private static final int LIT_BEGIN_EVO = 0x64;

    private TradeEvolutionRuntimeHelper() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress, int postEvolutionOffset) {
        if (postEvolutionOffset < 0 || postEvolutionOffset > 0xFF)
            throw new IllegalArgumentException("Post-evolution offset must fit callback Thumb immediate");

        byte[] code = hex(
                "F0B5114E358864216943104C641820000121002200F014F8F08000280ED00100" +
                "0B4AAA23937201320A4B1A600A4A0B4B1360200000222B0000F004F8F0BDC046" +
                "074F3847074F3847" +
                "1111111122222222333333334444444455555555666666667777777788888888");

        code[POST_EVO_OFFSET_IMMEDIATE] = (byte)postEvolutionOffset;
        putU32(code, LIT_VAR_8004, rom.specialVar8004);
        putU32(code, LIT_PLAYER_PARTY, rom.playerParty);
        putU32(code, LIT_CALLBACK_BASE, TradeEvolutionContinuationRuntime.CALLBACK);
        putU32(code, LIT_FIELD_CALLBACK2, rom.fieldCallback2);
        putU32(code, LIT_AFTER_EVO_SLOT, rom.cb2AfterEvolution);
        putU32(code, LIT_RETURN_TO_FIELD, rom.cb2ReturnToFieldThumb);
        putU32(code, LIT_GET_EVO, rom.getEvolutionTargetSpeciesThumb);
        putU32(code, LIT_BEGIN_EVO, rom.beginEvolutionSceneThumb);

        if (code.length != CODE_SIZE)
            throw new IllegalStateException("Trade evolution helper size mismatch: " + code.length);
        return new NativeHelper(stagingAddress, code);
    }

    private static byte[] hex(String value) {
        byte[] data = new byte[value.length() / 2];
        for (int i = 0; i < data.length; i++)
            data[i] = (byte)Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        return data;
    }

    private static void putU32(byte[] d, int o, long v) {
        d[o] = (byte)v; d[o + 1] = (byte)(v >>> 8); d[o + 2] = (byte)(v >>> 16); d[o + 3] = (byte)(v >>> 24);
    }
}
