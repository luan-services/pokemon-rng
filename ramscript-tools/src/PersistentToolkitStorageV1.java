import java.util.Arrays;

/*
   Build 4: first real persistent toolkit image.

   Physical storage for V1 is SaveBlock2::filler_B20 (offset 0x0B20).
   The image is intentionally tiny for the first execution proof:

     0x00  u32 magic       "CHPT"
     0x04  u8  version     1
     0x05  u8  flags       1 (installed)
     0x06  u8  kind        1 (Thumb payload)
     0x07  u8  reserved
     0x08  u16 payloadOff  0x0010
     0x0A  u16 payloadSize 0x000C
     0x0C  u16 checksum    additive byte checksum of payload
     0x0E  u16 reserved
     0x10  payload

   The payload itself is native Thumb code stored persistently in the save.
   It sets gSpecialVar_Result = 1 and returns directly to the caller.
*/
final class PersistentToolkitStorageV1 {
    static final PayloadStorageArea STORAGE_AREA = PayloadStorageArea.SAVE_BLOCK2;
    static final int STORAGE_OFFSET = STORAGE_AREA.offset();
    static final int STORAGE_CAPACITY = STORAGE_AREA.capacity();
    static final int HEADER_SIZE = 0x10;
    static final int VERSION = 1;
    static final int PAYLOAD_KIND_THUMB = 1;
    static final int TEST_PAYLOAD_SIZE = 12;
    static final int IMAGE_SIZE = HEADER_SIZE + TEST_PAYLOAD_SIZE;
    static final long MAGIC = 0x54504843L; // bytes: 43 48 50 54 = "CHPT"

    private PersistentToolkitStorageV1() {}

    static byte[] buildExecutionProofImage(RomProfile rom) {
        byte[] image = new byte[IMAGE_SIZE];
        putU32(image, 0x00, MAGIC);
        image[0x04] = (byte) VERSION;
        image[0x05] = 1;
        image[0x06] = (byte) PAYLOAD_KIND_THUMB;
        image[0x07] = 0;
        putU16(image, 0x08, HEADER_SIZE);
        putU16(image, 0x0A, TEST_PAYLOAD_SIZE);

        byte[] payload = buildTestPayload(rom);
        System.arraycopy(payload, 0, image, HEADER_SIZE, payload.length);
        putU16(image, 0x0C, checksum16(payload));
        putU16(image, 0x0E, 0);
        return image;
    }

    static byte[] buildTestPayload(RomProfile rom) {
        // movs r0,#1; ldr r1,[pc,#4]; strh r0,[r1]; bx lr; .word gSpecialVar_Result
        byte[] payload = new byte[] {
                0x01,0x20,
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

    static boolean isExecutionProofImage(byte[] image, RomProfile rom) {
        return Arrays.equals(image, buildExecutionProofImage(rom));
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
