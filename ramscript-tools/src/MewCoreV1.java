import java.util.Arrays;

/* Mew event persistent core, first integration build.
   Stores the Probe-24 battle transaction + exact MYSTRY reward as one
   relocatable Thumb blob in SaveBlock2. Execution entry offsets are recorded
   in the header; callers must resolve the live SaveBlock2 pointer each time. */
final class MewCoreV1 {
    static final int STORAGE_OFFSET = PayloadStorageArea.SAVE_BLOCK2.offset(); // 0x0B20
    static final int CAPACITY = PayloadStorageArea.SAVE_BLOCK2.capacity();
    static final int HEADER_SIZE = 0x20;
    static final long MAGIC = 0x3157454DL; // "MEW1"
    static final int VERSION = 1;
    static final int KIND = 1;

    record Image(byte[] bytes, int prepareOffset, int restoreOffset, int launchOffset, int rewardOffset, int nativeSize) {}

    private MewCoreV1() {}

    static Image build(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10) throw new IllegalArgumentException("Mew Core V1 is FR1.0-only");
        // Machine code is position-independent with respect to its own base:
        // internal ADR/branches are relative and external engine calls are literals.
        MewBossBattleTransactionNative.Layout tx = MewBossBattleTransactionNative.buildAt(rom, 0);
        byte[] nativeCode = tx.helper().codeCopy();
        int total = HEADER_SIZE + nativeCode.length;
        if (total > CAPACITY) throw new IllegalStateException("Mew Core exceeds SB2 storage: " + total + "/" + CAPACITY);
        // Probe-24 ball scratch starts at SB2+0xEC0 == storage-relative 0x3A0.
        int scratchRelative = MewBossBattleTransactionNative.BACKUP_OFFSET - STORAGE_OFFSET;
        if (total > scratchRelative) throw new IllegalStateException("Mew Core overlaps Probe-24 ball scratch: " + total + " > " + scratchRelative);

        byte[] out = new byte[total];
        putU32(out, 0x00, MAGIC);
        out[0x04] = (byte) VERSION;
        out[0x05] = 1; // installed
        out[0x06] = (byte) KIND;
        out[0x07] = 0;
        putU16(out, 0x08, total);
        putU16(out, 0x0A, HEADER_SIZE); // prepare/backup+clear entry
        putU16(out, 0x0C, HEADER_SIZE + tx.restoreOffset());
        putU16(out, 0x0E, HEADER_SIZE + tx.launchOffset());
        putU16(out, 0x10, HEADER_SIZE + tx.rewardOffset());
        putU16(out, 0x12, nativeCode.length);
        putU16(out, 0x14, checksum16(nativeCode));
        putU16(out, 0x16, 0); // event-state flags reserved
        putU32(out, 0x18, 0);
        putU32(out, 0x1C, 0);
        System.arraycopy(nativeCode, 0, out, HEADER_SIZE, nativeCode.length);
        return new Image(out, HEADER_SIZE, HEADER_SIZE + tx.restoreOffset(), HEADER_SIZE + tx.launchOffset(), HEADER_SIZE + tx.rewardOffset(), nativeCode.length);
    }

    static int checksum16(byte[] data) { int s=0; for(byte b:data) s=(s+Byte.toUnsignedInt(b))&0xFFFF; return s; }
    static void putU16(byte[] d,int o,int v){ d[o]=(byte)v; d[o+1]=(byte)(v>>>8); }
    static void putU32(byte[] d,int o,long v){ d[o]=(byte)v; d[o+1]=(byte)(v>>>8); d[o+2]=(byte)(v>>>16); d[o+3]=(byte)(v>>>24); }
}
