/* Compact IV viewer for party slot 0 only.
   Unlike PartyMonDataNativeHelper this physically removes the six-slot loop,
   inter-Pokemon separators and loop state. */
final class LeadIvNativeHelper {
    static final int CODE_SIZE = 272;
    static final int DYNAMIC_MESSAGE_DELTA = 0x280;
    private static final int PLAYER_PARTY_LITERAL_OFFSET = 0x104;
    private static final int DYNAMIC_MESSAGE_LITERAL_OFFSET = 0x108;
    private static final int GET_MON_DATA_LITERAL_OFFSET = 0x10C;
    private LeadIvNativeHelper() {}
    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        byte[] code = hex("F0B5404D404C28000B21002200F060F8002844D028000221220000F059F82078FF2801D00134FAE72AA000F049F828004221002200F04CF8070038001F21084000F030F825A000F03BF8380040091F21084000F027F823A000F032F83800800A1F21084000F01EF820A000F029F83800000D1F21084000F015F81EA000F020F83800400E1F21084000F00CF81BA000F017F83800C00B1F21084000F003F8FF202070F0BD0A2807D300210A3801310A28FBD2A13121700134A1302070013470470178FF2903D0217001300134F8E770470E4B184700C3D0E7F0FEFF0000BBCEC500FFC04600BEBFC000FFC046FBCDCABB00FFC04600CDCABE00FFC04600CDCABF00FFC046111111112222222233333333");
        putU32(code, PLAYER_PARTY_LITERAL_OFFSET, rom.playerParty);
        putU32(code, DYNAMIC_MESSAGE_LITERAL_OFFSET, dynamicMessageAddress(rom));
        putU32(code, GET_MON_DATA_LITERAL_OFFSET, rom.getMonData3Thumb);
        if (code.length != CODE_SIZE) throw new IllegalStateException("lead IV helper size mismatch");
        if (stagingAddress + code.length > dynamicMessageAddress(rom))
            throw new IllegalStateException("lead IV helper overlaps dynamic message buffer");
        return new NativeHelper(stagingAddress, code);
    }
    static long dynamicMessageAddress(RomProfile rom) { return rom.stringVar4 + DYNAMIC_MESSAGE_DELTA; }
    private static byte[] hex(String v) {
        byte[] d=new byte[v.length()/2];
        for(int i=0;i<d.length;i++) d[i]=(byte)Integer.parseInt(v.substring(i*2,i*2+2),16);
        return d;
    }
    private static void putU32(byte[] d,int o,long v) {
        d[o]=(byte)v; d[o+1]=(byte)(v>>>8); d[o+2]=(byte)(v>>>16); d[o+3]=(byte)(v>>>24);
    }
}
