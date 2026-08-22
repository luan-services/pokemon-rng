import java.util.Arrays;

/* Build 7: incremental persistent module layout.

   The table has fixed slots and payloads use fixed proof slots. This is
   deliberately simple: installer A initializes the storage with module 1;
   installer B patches only module 2 + moduleCount, preserving module 1 bytes.
*/
final class PersistentToolkitStorageV3 {
    static final long MAGIC = 0x54504843L; // CHPT
    static final int VERSION = 3;
    static final int HEADER_SIZE = 0x10;
    static final int ENTRY_SIZE = 0x10;
    static final int MODULE_A = 1;
    static final int MODULE_B = 2;
    static final int ENTRY_A = 0x10;
    static final int ENTRY_B = 0x20;
    static final int PAYLOAD_A = 0x40;
    static final int PAYLOAD_B = 0x30; // contiguous with entry B for sparse patch
    static final int INITIAL_IMAGE_SIZE = PAYLOAD_A + 12;

    private PersistentToolkitStorageV3() {}

    static byte[] buildInitialImage(RomProfile rom) {
        byte[] image = new byte[INITIAL_IMAGE_SIZE];
        PersistentToolkitStorageV2.putU32(image, 0, MAGIC);
        image[4] = VERSION;
        image[5] = 1;
        PersistentToolkitStorageV2.putU16(image, 6, HEADER_SIZE);
        PersistentToolkitStorageV2.putU16(image, 8, INITIAL_IMAGE_SIZE);
        writeEntry(image, ENTRY_A, MODULE_A, PAYLOAD_A, PersistentToolkitStorageV2.resultPayload(rom, 1));
        System.arraycopy(PersistentToolkitStorageV2.resultPayload(rom, 1), 0, image, PAYLOAD_A, 12);
        return image;
    }

    /* 28-byte sparse patch: entry B immediately followed by payload B. */
    static byte[] buildModuleBPatch(RomProfile rom) {
        byte[] payload = PersistentToolkitStorageV2.resultPayload(rom, 2);
        byte[] patch = new byte[ENTRY_SIZE + payload.length];
        writeEntry(patch, 0, MODULE_B, PAYLOAD_B, payload);
        System.arraycopy(payload, 0, patch, ENTRY_SIZE, payload.length);
        return patch;
    }

    static void writeEntry(byte[] out, int at, int id, int payloadOffset, byte[] payload) {
        PersistentToolkitStorageV2.putU16(out, at, id);
        out[at + 2] = PersistentModule.KIND_THUMB;
        out[at + 3] = 2; // SaveBlock2
        PersistentToolkitStorageV2.putU16(out, at + 4, payloadOffset);
        PersistentToolkitStorageV2.putU16(out, at + 6, payload.length);
        PersistentToolkitStorageV2.putU16(out, at + 8, PersistentToolkitStorageV2.checksum16(payload));
        PersistentToolkitStorageV2.putU16(out, at + 0x0A, 1);
    }

    static int entryOffsetForModule(int id) {
        return switch (id) { case 1 -> ENTRY_A; case 2 -> ENTRY_B; default -> throw new IllegalArgumentException("module id must be 1 or 2"); };
    }

    static boolean moduleARegionUnchanged(byte[] before, byte[] after) {
        return Arrays.equals(Arrays.copyOfRange(before, ENTRY_A, ENTRY_A + ENTRY_SIZE), Arrays.copyOfRange(after, ENTRY_A, ENTRY_A + ENTRY_SIZE))
            && Arrays.equals(Arrays.copyOfRange(before, PAYLOAD_A, PAYLOAD_A + 12), Arrays.copyOfRange(after, PAYLOAD_A, PAYLOAD_A + 12));
    }
}
