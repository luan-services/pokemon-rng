/*
   Experimental fixed-EWRAM persistence probe for the unused tail of
   sFlickerArray in vanilla English FR/LG.

   Candidate region: 0x02022B08..0x02022B4B (68 bytes).

   Single-preset state machine:
   - exact A5^i pattern -> result 1 (survived)
   - all zeroes         -> install pattern, result 0
   - anything else      -> result 2 (corrupted), do not repair

   The helper is assembled for ARM7TDMI Thumb-1 and intentionally uses only
   caller-saved r0-r3, so returning through callnative cannot corrupt the
   Field Script interpreter's callee-saved registers.
*/
final class FlickerRegionProbeNativeHelper {
    static final long REGION_START = 0x02022B08L;
    static final int REGION_SIZE = 68;
    static final int PATTERN_XOR = 0xA5;

    static final int RESULT_INSTALLED = 0;
    static final int RESULT_OK = 1;
    static final int RESULT_FAILED = 2;

    private FlickerRegionProbeNativeHelper() {}

    private static long copierAddress(RomProfile rom) {
        return rom.stringVar4 + 0x100L;
    }

    static long stagingAddress(RomProfile rom) {
        return CpuSetNativeHelperInstaller.helperDestination(copierAddress(rom));
    }

    static long copierStagingAddress(RomProfile rom) {
        return copierAddress(rom);
    }

    static NativeHelper build(RomProfile rom) {
        // Assembled/disassembled as ARM7TDMI Thumb-1. Literal slots are patched
        // below so the same code works with every RomProfile.
        byte[] code = new byte[] {
                0x12,0x48, 0x00,0x21, (byte)0xA5,0x22, 0x03,0x78,
                0x53,0x40, (byte)0x8B,0x42, 0x05,(byte)0xD1, 0x01,0x30,
                0x01,0x31, 0x44,0x29, (byte)0xF7,(byte)0xD1, 0x01,0x20,
                0x15,(byte)0xE0, 0x0C,0x48, 0x00,0x21, 0x03,0x78,
                0x00,0x2B, 0x0F,(byte)0xD1, 0x01,0x30, 0x01,0x31,
                0x44,0x29, (byte)0xF8,(byte)0xD1, 0x07,0x48, 0x00,0x21,
                (byte)0xA5,0x22, 0x0B,0x00, 0x53,0x40, 0x03,0x70,
                0x01,0x30, 0x01,0x31, 0x44,0x29, (byte)0xF8,(byte)0xD1,
                0x00,0x20, 0x00,(byte)0xE0, 0x02,0x20, 0x02,0x49,
                0x08,(byte)0x80, 0x70,0x47,
                0,0,0,0, // 0x4C REGION_START
                0,0,0,0  // 0x50 gSpecialVar_Result
        };
        putU32(code, 0x4C, REGION_START);
        putU32(code, 0x50, rom.specialVarResult);
        return new NativeHelper(stagingAddress(rom), code);
    }

    static int expectedByte(int index) {
        if (index < 0 || index >= REGION_SIZE) throw new IllegalArgumentException("index out of range");
        return PATTERN_XOR ^ index;
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
