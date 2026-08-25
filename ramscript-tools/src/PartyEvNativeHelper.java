/* Deliveryman-first Party EV Viewer native helper.
   Mirrors the validated Party IV viewer presentation but reads the six EV
   fields (MON_DATA_*_EV = 26..31) through stock GetMonData. */
final class PartyEvNativeHelper {
    static final int CODE_SIZE = 336;
    static final int DYNAMIC_MESSAGE_DELTA = 0x280;
    private static final int PLAYER_PARTY_LITERAL_OFFSET = 0x100;
    private static final int DYNAMIC_MESSAGE_LITERAL_OFFSET = 0x104;
    private static final int GET_MON_DATA_LITERAL_OFFSET = 0x108;
    private static final int EVS_TEXT_OFFSET = 0x110;
    private static final int HP_TEXT_OFFSET = 0x120;
    private static final int ATK_TEXT_OFFSET = 0x128;
    private static final int DEF_TEXT_OFFSET = 0x130;
    private static final int SPA_TEXT_OFFSET = 0x138;
    private static final int SPD_TEXT_OFFSET = 0x140;
    private static final int SPE_TEXT_OFFSET = 0x148;

    private PartyEvNativeHelper() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        byte[] code = hex(
                "F0B53F4D3F4C002628000B21002200F047F8002835D0002E02D03CA000F048F828000221220000F03BF800F03BF838A000F03EF83AA000F03BF81A21" +
                "00F028F839A000F035F81B2100F022F838A000F02FF81C2100F01CF82CA000F029F836A000F026F81E2100F013F835A000F020F81F2100F00DF834A0" +
                "00F01AF81D2100F007F864350136062EBED3FF202070F0BD02B52800002200F003F800F011F802BD194B18472078FF2801D00134FAE770470178FF29" +
                "03D0217001300134F8E770470EB5642809D30021643801316428FBD2A13121700134012200E0002200210A2802D30A380131FAE7002A01D1002902D0" +
                "A13121700134A130207001340EBDC046111111112222222233333333FBFFC046FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
        );
        putU32(code, PLAYER_PARTY_LITERAL_OFFSET, rom.playerParty);
        putU32(code, DYNAMIC_MESSAGE_LITERAL_OFFSET, dynamicMessageAddress(rom));
        putU32(code, GET_MON_DATA_LITERAL_OFFSET, rom.getMonData3Thumb);
        putText(code, EVS_TEXT_OFFSET, 16, "\\n");
        putText(code, HP_TEXT_OFFSET, 8, "HP ");
        putText(code, ATK_TEXT_OFFSET, 8, " ATK ");
        putText(code, DEF_TEXT_OFFSET, 8, " DEF ");
        putText(code, SPA_TEXT_OFFSET, 8, "SPA ");
        putText(code, SPD_TEXT_OFFSET, 8, " SPD ");
        putText(code, SPE_TEXT_OFFSET, 8, " SPE ");
        if (code.length != CODE_SIZE) throw new IllegalStateException("party EV helper size mismatch");
        if (stagingAddress + code.length > dynamicMessageAddress(rom))
            throw new IllegalStateException("party EV helper overlaps dynamic message buffer");
        return new NativeHelper(stagingAddress, code);
    }

    static long dynamicMessageAddress(RomProfile rom) { return rom.stringVar4 + DYNAMIC_MESSAGE_DELTA; }

    static NativeHelper buildCompactAt(RomProfile rom, long stagingAddress) {
        NativeHelper full = buildAt(rom, 0x02000000L);
        byte[] code = java.util.Arrays.copyOf(full.codeCopy(), 0x140);
        patchAdr(code,0x2E,0x110); patchAdr(code,0x34,0x114);
        patchAdr(code,0x40,0x118); patchAdr(code,0x4C,0x120);
        patchAdr(code,0x5E,0x128); patchAdr(code,0x6A,0x130);
        patchAdr(code,0x76,0x138);
        putText(code,0x110,4,"\\n"); putText(code,0x114,4,"HP ");
        putText(code,0x118,8," ATK "); putText(code,0x120,8," DEF ");
        putText(code,0x128,8,"SPA "); putText(code,0x130,8," SPD ");
        putText(code,0x138,8," SPE ");
        if (stagingAddress + code.length > dynamicMessageAddress(rom))
            throw new IllegalStateException("compact party EV helper overlaps dynamic message buffer");
        return new NativeHelper(stagingAddress, code);
    }
    private static void patchAdr(byte[] code,int off,int target) {
        int pc=(off+4)&~3, delta=target-pc;
        if(delta<0||(delta&3)!=0||delta/4>0xFF) throw new IllegalArgumentException("ADR target out of range");
        putU16(code,off,0xA000|(delta/4));
    }
    private static void putU16(byte[] d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);}

    private static void putText(byte[] dst, int off, int cap, String text) {
        byte[] encoded = Gen3TextCodec.encodeString(text);
        if (encoded.length > cap) throw new IllegalArgumentException("encoded EV label too large");
        java.util.Arrays.fill(dst, off, off + cap, (byte)0xFF);
        System.arraycopy(encoded, 0, dst, off, encoded.length);
    }
    private static byte[] hex(String value) {
        byte[] data = new byte[value.length()/2];
        for (int i=0;i<data.length;i++) data[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);
        return data;
    }
    private static void putU32(byte[] data,int offset,long value) {
        data[offset]=(byte)value; data[offset+1]=(byte)(value>>>8); data[offset+2]=(byte)(value>>>16); data[offset+3]=(byte)(value>>>24);
    }
}
