/* Build 46: one-shot helper that ONLY queries the stock trade-evolution target. */
final class TradeEvolutionTargetProbeHelper {
    static final int CODE_SIZE = 56;

    private static final int LIT_VAR_8004 = 0x28;
    private static final int LIT_PLAYER_PARTY = 0x2C;
    private static final int LIT_GET_EVO = 0x30;
    private static final int LIT_VAR_RESULT = 0x34;

    private TradeEvolutionTargetProbeHelper() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 46 target probe currently supports fr10 only");

        byte[] code = hex(
                "10B5094B1888062808D264214843074908180121002200F005F800E00020054B188010BD024B1847" +
                "11111111222222223333333344444444"
        );
        putU32(code, LIT_VAR_8004, rom.specialVar8004);
        putU32(code, LIT_PLAYER_PARTY, rom.playerParty);
        putU32(code, LIT_GET_EVO, rom.getEvolutionTargetSpeciesThumb);
        putU32(code, LIT_VAR_RESULT, rom.specialVarResult);

        if (code.length != CODE_SIZE)
            throw new IllegalStateException("Build 46 target helper size mismatch");
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
