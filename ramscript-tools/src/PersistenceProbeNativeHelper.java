/*
   Experimental persistence probe for SaveBlock1::unused_348C.

   The probe deliberately touches only the first 8 bytes of the 400-byte field.
   It always resolves gSaveBlock1Ptr at runtime; no SaveBlock1 EWRAM address is
   hard-coded because the game can relocate the save blocks.
*/
final class PersistenceProbeNativeHelper {
    static final long STAGING_ADDRESS = 0x03005310L;
    static final int STORAGE_OFFSET = 0x348C;
    static final int STORAGE_SIZE = 400;

    // Stored bytes: 43 48 50 59 01 00 5A A5 = "CHPY" + version 1 + marker A55A.
    private static final long MAGIC = 0x59504843L;
    private static final long VERSION_MARKER = 0xA55A0001L;

    private PersistenceProbeNativeHelper() {}

    static NativeHelper buildWriter(RomProfile rom) {
        byte[] code = new byte[] {
                0x04, 0x48,             // ldr r0, [pc,#16] -> gSaveBlock1Ptr
                0x00, 0x68,             // ldr r0, [r0]
                0x04, 0x4B,             // ldr r3, [pc,#16] -> 0x348C
                (byte)0xC0, 0x18,       // adds r0, r0, r3
                0x04, 0x49,             // ldr r1, [pc,#16] -> MAGIC
                0x01, 0x60,             // str r1, [r0]
                0x04, 0x49,             // ldr r1, [pc,#16] -> VERSION_MARKER
                0x41, 0x60,             // str r1, [r0,#4]
                0x70, 0x47,             // bx lr
                0x00, 0x00,             // alignment
                0, 0, 0, 0,             // gSaveBlock1Ptr
                0, 0, 0, 0,             // STORAGE_OFFSET
                0, 0, 0, 0,             // MAGIC
                0, 0, 0, 0              // VERSION_MARKER
        };
        putU32(code, 0x14, rom.saveBlock1Ptr);
        putU32(code, 0x18, STORAGE_OFFSET);
        putU32(code, 0x1C, MAGIC);
        putU32(code, 0x20, VERSION_MARKER);
        return new NativeHelper(STAGING_ADDRESS, code);
    }

    static NativeHelper buildChecker(RomProfile rom) {
        byte[] code = new byte[] {
                0x08, 0x48,             // ldr r0, [pc,#32] -> gSaveBlock1Ptr
                0x00, 0x68,             // ldr r0, [r0]
                0x08, 0x4B,             // ldr r3, [pc,#32] -> 0x348C
                (byte)0xC0, 0x18,       // adds r0, r0, r3
                0x08, 0x49,             // ldr r1, [pc,#32] -> MAGIC
                0x02, 0x68,             // ldr r2, [r0]
                (byte)0x8A, 0x42,       // cmp r2, r1
                0x05, (byte)0xD1,       // bne fail
                0x07, 0x49,             // ldr r1, [pc,#28] -> VERSION_MARKER
                0x42, 0x68,             // ldr r2, [r0,#4]
                (byte)0x8A, 0x42,       // cmp r2, r1
                0x01, (byte)0xD1,       // bne fail
                0x01, 0x20,             // movs r0,#1
                0x00, (byte)0xE0,       // b store
                0x00, 0x20,             // fail: movs r0,#0
                0x05, 0x49,             // store: ldr r1,[pc,#20] -> gSpecialVar_Result
                0x08, (byte)0x80,       // strh r0,[r1]
                0x70, 0x47,             // bx lr
                0, 0, 0, 0,             // gSaveBlock1Ptr
                0, 0, 0, 0,             // STORAGE_OFFSET
                0, 0, 0, 0,             // MAGIC
                0, 0, 0, 0,             // VERSION_MARKER
                0, 0, 0, 0              // gSpecialVar_Result
        };
        putU32(code, 0x24, rom.saveBlock1Ptr);
        putU32(code, 0x28, STORAGE_OFFSET);
        putU32(code, 0x2C, MAGIC);
        putU32(code, 0x30, VERSION_MARKER);
        putU32(code, 0x34, rom.specialVarResult);
        return new NativeHelper(STAGING_ADDRESS, code);
    }

    // Full-region probe pattern: byte[i] = i & 0xFF for all 400 bytes.
    // These helpers are staged in gStringVar4 scratch by PersistenceProbePreset;
    // unlike the historical 8-byte probe they are never placed in the 32-byte
    // UnusedVarNeededToMatch resident block.
    static NativeHelper buildFullWriterAt(RomProfile rom, long address) {
        byte[] code = new byte[] {
                0x05,0x48, 0x00,0x68, 0x05,0x4B, (byte)0xC0,0x18,
                0x00,0x21, 0x05,0x4A, 0x01,0x70, 0x01,0x30,
                0x01,0x31, 0x01,0x3A, (byte)0xFA,(byte)0xD1, 0x70,0x47,
                0,0,0,0, (byte)0x8C,0x34,0,0, (byte)0x90,0x01,0,0
        };
        putU32(code, 0x18, rom.saveBlock1Ptr);
        return new NativeHelper(address, code);
    }

    static NativeHelper buildFullCheckerAt(RomProfile rom, long address) {
        /*
           Verify byte[i] == (i & 0xFF) for all 400 bytes.

           r0 = SaveBlock1 + 0x348C
           r1 = expected byte (explicitly wrapped to 8 bits)
           r2 = remaining byte count
           r3 = actual byte

           The explicit wrap matters after byte 255. The previous experimental
           checker compared an ldrb result against an ever-growing r1, which
           would necessarily fail at byte 256 even if storage was perfect.
        */
        byte[] code = new byte[] {
                0x0A,0x48,             // 00 ldr r0,[pc,#40] -> +0x2C gSaveBlock1Ptr
                0x00,0x68,             // 02 ldr r0,[r0]
                0x0A,0x4B,             // 04 ldr r3,[pc,#40] -> +0x30 STORAGE_OFFSET
                (byte)0xC0,0x18,       // 06 adds r0,r0,r3
                0x00,0x21,             // 08 movs r1,#0
                0x0A,0x4A,             // 0A ldr r2,[pc,#40] -> +0x34 STORAGE_SIZE
                0x03,0x78,             // 0C loop: ldrb r3,[r0]
                (byte)0x8B,0x42,       // 0E cmp r3,r1
                0x07,(byte)0xD1,       // 10 bne fail (+14 bytes)
                0x01,0x30,             // 12 adds r0,#1
                0x01,0x31,             // 14 adds r1,#1
                0x09,0x06,             // 16 lsls r1,r1,#24
                0x09,0x0E,             // 18 lsrs r1,r1,#24 (wrap expected to u8)
                0x01,0x3A,             // 1A subs r2,#1
                (byte)0xF6,(byte)0xD1, // 1C bne loop (-20 bytes)
                0x01,0x20,             // 1E movs r0,#1
                0x00,(byte)0xE0,       // 20 b store
                0x00,0x20,             // 22 fail: movs r0,#0
                0x04,0x49,             // 24 store: ldr r1,[pc,#16] -> +0x38 result
                0x08,(byte)0x80,       // 26 strh r0,[r1]
                0x70,0x47,             // 28 bx lr
                (byte)0xC0,0x46,       // 2A nop/alignment
                0,0,0,0,               // 2C gSaveBlock1Ptr
                (byte)0x8C,0x34,0,0,  // 30 STORAGE_OFFSET
                (byte)0x90,0x01,0,0,  // 34 STORAGE_SIZE = 400
                0,0,0,0                // 38 gSpecialVar_Result
        };
        putU32(code, 0x2C, rom.saveBlock1Ptr);
        putU32(code, 0x38, rom.specialVarResult);
        return new NativeHelper(address, code);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
