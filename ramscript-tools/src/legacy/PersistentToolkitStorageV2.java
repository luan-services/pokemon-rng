import java.util.Arrays;

/* Build 6: first table-based persistent module image.

   V2 intentionally starts with two tiny Thumb proof modules in SaveBlock2.
   The important change is structural: launchers resolve a module-table entry
   by ID instead of assuming one payload at a fixed +0x10 address.

   Header (0x10 bytes):
     00 u32 magic "CHPT"
     04 u8  version = 2
     05 u8  moduleCount
     06 u16 tableOffset = 0x10
     08 u16 imageSize
     0A u16 reserved
     0C u32 reserved

   Entry (0x10 bytes each):
     00 u16 moduleId
     02 u8  kind (1 = Thumb)
     03 u8  area (2 = SaveBlock2 in V2)
     04 u16 payloadOffset
     06 u16 payloadSize
     08 u16 checksum16
     0A u16 flags
     0C u32 reserved
*/
final class PersistentToolkitStorageV2 {
    static final long MAGIC = 0x54504843L;
    static final int VERSION = 2;
    static final int HEADER_SIZE = 0x10;
    static final int ENTRY_SIZE = 0x10;
    static final int MODULE_A = 1;
    static final int MODULE_B = 2;
    static final int AREA_SAVE_BLOCK2 = 2;

    private PersistentToolkitStorageV2() {}

    static PersistentModule moduleA(RomProfile rom) {
        return new PersistentModule(MODULE_A, PersistentModule.KIND_THUMB,
                PayloadStorageArea.SAVE_BLOCK2, resultPayload(rom, 1));
    }

    static PersistentModule moduleB(RomProfile rom) {
        return new PersistentModule(MODULE_B, PersistentModule.KIND_THUMB,
                PayloadStorageArea.SAVE_BLOCK2, resultPayload(rom, 2));
    }

    static byte[] buildProofImage(RomProfile rom) {
        PersistentModule[] modules = { moduleA(rom), moduleB(rom) };
        int payloadStart = HEADER_SIZE + modules.length * ENTRY_SIZE;
        int imageSize = payloadStart;
        for (PersistentModule module : modules) imageSize += module.payload().length;
        byte[] image = new byte[imageSize];
        putU32(image, 0x00, MAGIC);
        image[0x04] = (byte) VERSION;
        image[0x05] = (byte) modules.length;
        putU16(image, 0x06, HEADER_SIZE);
        putU16(image, 0x08, imageSize);

        int payloadOffset = payloadStart;
        for (int i = 0; i < modules.length; i++) {
            PersistentModule module = modules[i];
            byte[] payload = module.payload();
            int entry = HEADER_SIZE + i * ENTRY_SIZE;
            putU16(image, entry, module.id());
            image[entry + 2] = (byte) module.kind();
            image[entry + 3] = (byte) AREA_SAVE_BLOCK2;
            putU16(image, entry + 4, payloadOffset);
            putU16(image, entry + 6, payload.length);
            putU16(image, entry + 8, checksum16(payload));
            putU16(image, entry + 0x0A, 1); // installed/enabled
            System.arraycopy(payload, 0, image, payloadOffset, payload.length);
            payloadOffset += payload.length;
        }
        return image;
    }

    static int entryOffsetForModule(int moduleId) {
        return switch (moduleId) {
            case MODULE_A -> HEADER_SIZE;
            case MODULE_B -> HEADER_SIZE + ENTRY_SIZE;
            default -> throw new IllegalArgumentException("unknown V2 proof module id: " + moduleId);
        };
    }

    static byte[] resultPayload(RomProfile rom, int value) {
        if (value < 0 || value > 255) throw new IllegalArgumentException("proof result must fit movs immediate");
        byte[] payload = new byte[] {
                (byte)value,0x20,
                0x01,0x49,
                0x08,(byte)0x80,
                0x70,0x47,
                0,0,0,0
        };
        putU32(payload, 0x08, rom.specialVarResult);
        return payload;
    }

    static int checksum16(byte[] data) {
        int sum = 0;
        for (byte b : data) sum = (sum + Byte.toUnsignedInt(b)) & 0xFFFF;
        return sum;
    }

    static boolean imageMatches(byte[] image, RomProfile rom) {
        return Arrays.equals(image, buildProofImage(rom));
    }

    static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
    }

    static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
