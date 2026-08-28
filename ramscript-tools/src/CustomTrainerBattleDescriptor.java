import java.util.Arrays;

/* Persistent descriptor consumed by CustomTrainerBattleRuntimeV1.
   Stored in the validated SB2 auxiliary region; all offsets are relative to the live SaveBlock2 pointer. */
final class CustomTrainerBattleDescriptor {
    static final int MAGIC = 0x31445443; // "CTD1" little-endian
    static final int VERSION = 1;
    static final int HEADER_SIZE = 0x20;
    static final int DEFEAT_TEXT_OFFSET = 0x20;
    static final int DEFEAT_TEXT_CAPACITY = 0x40;
    static final int PARTY_OFFSET = 0x60;
    static final int MAX_PARTY = 6;
    static final int MAX_SIZE = PARTY_OFFSET + MAX_PARTY * EReaderTrainerData.MON_SIZE; // 360 B

    static final int OFF_MAGIC = 0x00;
    static final int OFF_VERSION = 0x04;
    static final int OFF_SIZE = 0x06;
    static final int OFF_TRAINER_ID = 0x08;
    static final int OFF_BGM = 0x0A;
    static final int OFF_COMPLETION_FLAG = 0x0C;
    static final int OFF_LOCAL_ID = 0x0E;
    static final int OFF_PARTY_COUNT = 0x0F;
    static final int OFF_AFTER_BATTLE = 0x10;

    private CustomTrainerBattleDescriptor() {}

    static byte[] encode(CustomTrainerBattleSpec spec, int localId, int afterBattleOffset) {
        if (localId < 0 || localId > 0xFF) throw new IllegalArgumentException("localId must fit u8");
        if (afterBattleOffset < 0 || afterBattleOffset > 0xFFFF) throw new IllegalArgumentException("afterBattleOffset must fit u16");
        int size = PARTY_OFFSET + spec.party().size() * EReaderTrainerData.MON_SIZE;
        byte[] out = new byte[size];
        putU32(out, OFF_MAGIC, MAGIC);
        putU16(out, OFF_VERSION, VERSION);
        putU16(out, OFF_SIZE, size);
        putU16(out, OFF_TRAINER_ID, spec.identity().trainerId());
        putU16(out, OFF_BGM, spec.battleMusic().songId());
        putU16(out, OFF_COMPLETION_FLAG, spec.completionFlag().eventFlag());
        out[OFF_LOCAL_ID] = (byte)localId;
        out[OFF_PARTY_COUNT] = (byte)spec.party().size();
        putU16(out, OFF_AFTER_BATTLE, afterBattleOffset);

        Arrays.fill(out, DEFEAT_TEXT_OFFSET, DEFEAT_TEXT_OFFSET + DEFEAT_TEXT_CAPACITY, (byte)0xFF);
        byte[] defeat = Gen3TextCodec.encodeString(spec.defeatText());
        if (defeat.length > DEFEAT_TEXT_CAPACITY)
            throw new IllegalArgumentException("defeat text exceeds " + DEFEAT_TEXT_CAPACITY + " encoded bytes");
        System.arraycopy(defeat, 0, out, DEFEAT_TEXT_OFFSET, defeat.length);

        for (int i = 0; i < spec.party().size(); i++) {
            byte[] mon = EReaderTrainerData.encodeMon(spec.party().get(i));
            System.arraycopy(mon, 0, out, PARTY_OFFSET + i * EReaderTrainerData.MON_SIZE, mon.length);
        }
        return out;
    }

    private static void putU16(byte[] d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);}
    private static void putU32(byte[] d,int o,long v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);d[o+2]=(byte)(v>>>16);d[o+3]=(byte)(v>>>24);}
}
