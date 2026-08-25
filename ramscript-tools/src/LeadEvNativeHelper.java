/* Compact EV viewer for party slot 0 only.
   Physically removes the six-slot loop and inter-Pokemon logic while keeping
   the same six EV fields and decimal formatting as the validated viewer. */
final class LeadEvNativeHelper {
    static final int CODE_SIZE = 284;
    static final int DYNAMIC_MESSAGE_DELTA = 0x280;
    private static final int PLAYER_PARTY_LITERAL_OFFSET = 0x110;
    private static final int DYNAMIC_MESSAGE_LITERAL_OFFSET = 0x114;
    private static final int GET_MON_DATA_LITERAL_OFFSET = 0x118;
    private LeadEvNativeHelper() {}
    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        byte[] code = hex("30B5434D434C28000B21002200F03EF8002830D028000221220000F037F82078FF2801D00134FAE72DA000F031F82DA000F02EF81A2100F021F82BA000F028F81B2100F01BF82AA000F022F81C2100F015F829A000F01CF81E2100F00FF828A000F016F81F2100F009F827A000F010F81D2100F003F8FF20207030BD02B52800002200F003F800F00BF802BD224B18470178FF2903D0217001300134F8E770470EB5642809D30021643801316428FBD2A13121700134012200E0002200210A2802D30A380131FAE7002A01D1002902D0A13121700134A130207001340EBDC046FEFFC046C2CA00FF00BBCEC500FFC04600BEBFC000FFC046FBCDCABB00FFC04600CDCABE00FFC04600CDCABF00FFC046111111112222222233333333");
        putU32(code, PLAYER_PARTY_LITERAL_OFFSET, rom.playerParty);
        putU32(code, DYNAMIC_MESSAGE_LITERAL_OFFSET, dynamicMessageAddress(rom));
        putU32(code, GET_MON_DATA_LITERAL_OFFSET, rom.getMonData3Thumb);
        if (code.length != CODE_SIZE) throw new IllegalStateException("lead EV helper size mismatch");
        if (stagingAddress + code.length > dynamicMessageAddress(rom))
            throw new IllegalStateException("lead EV helper overlaps dynamic message buffer");
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
