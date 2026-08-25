/* Build 47: minimal one-shot launcher for a KNOWN-VALID trade evolution. */
final class TradeEvolutionSceneProbeHelper {
    static final int CODE_SIZE = 76;
    private static final int LIT_VAR_8004 = 0x34;
    private static final int LIT_PLAYER_PARTY = 0x38;
    private static final int LIT_AFTER_EVO_SLOT = 0x3C;
    private static final int LIT_RETURN_FIELD = 0x40;
    private static final int LIT_GET_EVO = 0x44;
    private static final int LIT_BEGIN_EVO = 0x48;

    private TradeEvolutionSceneProbeHelper() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 47 scene probe currently supports fr10 only");
        byte[] code = hex(
                "70B50C4E3588642169430B4C641820000121002200F00AF80100084A084B1360" +
                "200000222B0000F003F870BD054B1847054B1847" +
                "111111112222222233333333444444445555555566666666");
        putU32(code, LIT_VAR_8004, rom.specialVar8004);
        putU32(code, LIT_PLAYER_PARTY, rom.playerParty);
        putU32(code, LIT_AFTER_EVO_SLOT, rom.cb2AfterEvolution);
        putU32(code, LIT_RETURN_FIELD, rom.cb2ReturnToFieldContinueScriptThumb);
        putU32(code, LIT_GET_EVO, rom.getEvolutionTargetSpeciesThumb);
        putU32(code, LIT_BEGIN_EVO, rom.beginEvolutionSceneThumb);
        if (code.length != CODE_SIZE) throw new IllegalStateException("Build 47 helper size mismatch");
        return new NativeHelper(stagingAddress, code);
    }

    private static byte[] hex(String value) {
        byte[] data = new byte[value.length()/2];
        for (int i=0;i<data.length;i++) data[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);
        return data;
    }
    private static void putU32(byte[] d,int o,long v) {
        d[o]=(byte)v; d[o+1]=(byte)(v>>>8); d[o+2]=(byte)(v>>>16); d[o+3]=(byte)(v>>>24);
    }
}
