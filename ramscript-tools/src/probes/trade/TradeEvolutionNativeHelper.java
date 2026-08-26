/*
   One-shot/non-resident trade-evolution helper.

   Reads the party slot chosen by the stock ChoosePartyMon special from
   VAR_0x8004, calls the game's own GetEvolutionTargetSpecies with
   EVO_MODE_TRADE, and, when valid, launches the stock evolution scene on the
   original Pokemon.

   Important: GetEvolutionTargetSpecies itself handles Everstone and
   EVO_TRADE_ITEM. For a matching trade item it consumes the held item exactly
   as the stock trade-evolution path does.
*/
final class TradeEvolutionNativeHelper {
    static final int CODE_SIZE = 104;

    private static final int LIT_VAR_8004 = 0x4C;
    private static final int LIT_PLAYER_PARTY = 0x50;
    private static final int LIT_VAR_RESULT = 0x54;
    private static final int LIT_AFTER_EVO_SLOT = 0x58;
    private static final int LIT_RETURN_FIELD = 0x5C;
    private static final int LIT_GET_EVO = 0x60;
    private static final int LIT_BEGIN_EVO = 0x64;

    private TradeEvolutionNativeHelper() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        byte[] code = hex(
                "F0B5124C2588062D18D26421281C48430F4C2418201C0121002200F013F80D4E" +
                "308000280DD0071C0B490C4A0A60201C391C00222B1C00F007F802E000200549" +
                "0880F0BD064B1847064B18471111111122222222333333334444444455555555" +
                "6766666677777777"
        );
        putU32(code, LIT_VAR_8004, rom.specialVar8004);
        putU32(code, LIT_PLAYER_PARTY, rom.playerParty);
        putU32(code, LIT_VAR_RESULT, rom.specialVarResult);
        putU32(code, LIT_AFTER_EVO_SLOT, rom.cb2AfterEvolution);
        putU32(code, LIT_RETURN_FIELD, rom.cb2ReturnToFieldContinueScriptThumb);
        putU32(code, LIT_GET_EVO, rom.getEvolutionTargetSpeciesThumb);
        putU32(code, LIT_BEGIN_EVO, rom.beginEvolutionSceneThumb);

        if (code.length != CODE_SIZE)
            throw new IllegalStateException("trade evolution helper size mismatch");
        return new NativeHelper(stagingAddress, code);
    }

    private static byte[] hex(String value) {
        byte[] data = new byte[value.length() / 2];
        for (int i = 0; i < data.length; i++)
            data[i] = (byte)Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        return data;
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
