import java.io.ByteArrayOutputStream;

/* Variable-length overworld dialogue stored in the validated 400-byte SaveBlock1
   auxiliary area. Battle-only defeat speech intentionally stays in the SB2
   descriptor because the battle engine needs a stable pointer while the battle
   scene is active. */
final class CustomTrainerFieldTextStorage {
    static final int OFFSET = PayloadStorageArea.SAVE_BLOCK1.offset();
    static final int CAPACITY = PayloadStorageArea.SAVE_BLOCK1.capacity();
    static final int MAGIC = 0x54585443; // "CTXT"
    static final int VERSION = 1;
    static final int HEADER_SIZE = 0x10;

    static final int PRE_BATTLE = 0;
    static final int POST_VICTORY = 1;
    static final int ALREADY_COMPLETED = 2;

    static final int OFF_PRE_BATTLE = 0x08;
    static final int OFF_POST_VICTORY = 0x0A;
    static final int OFF_ALREADY_COMPLETED = 0x0C;

    private CustomTrainerFieldTextStorage() {}

    static byte[] encode(CustomTrainerBattleSpec spec) {
        ByteArrayOutputStream pool = new ByteArrayOutputStream();
        int pre = append(pool, spec.preBattleText());
        int post = append(pool, spec.postVictoryText());
        // Production policy: talking again after victory reuses the post-victory
        // line. The persistent CTXT layout is unchanged; both offsets simply
        // point at the same encoded string.
        int already = post;

        int size = HEADER_SIZE + pool.size();
        if (size > CAPACITY) {
            throw new IllegalArgumentException("trainer overworld text exceeds SB1 text area: " + size + "/" + CAPACITY);
        }

        byte[] out = new byte[size];
        putU32(out, 0x00, MAGIC);
        putU16(out, 0x04, VERSION);
        putU16(out, 0x06, size);
        putU16(out, OFF_PRE_BATTLE, HEADER_SIZE + pre);
        putU16(out, OFF_POST_VICTORY, HEADER_SIZE + post);
        putU16(out, OFF_ALREADY_COMPLETED, HEADER_SIZE + already);
        putU16(out, 0x0E, 0);
        System.arraycopy(pool.toByteArray(), 0, out, HEADER_SIZE, pool.size());
        return out;
    }

    private static int append(ByteArrayOutputStream out, String text) {
        int offset = out.size();
        out.writeBytes(Gen3TextCodec.encodeString(text));
        return offset;
    }

    private static void putU16(byte[] d, int o, int v) { d[o]=(byte)v; d[o+1]=(byte)(v>>>8); }
    private static void putU32(byte[] d, int o, long v) { d[o]=(byte)v; d[o+1]=(byte)(v>>>8); d[o+2]=(byte)(v>>>16); d[o+3]=(byte)(v>>>24); }
}
