/*
   Build-36 probe helper.

   FR/LG IsRunningDisallowed() first checks gMapHeader.allowRunning. The bit is
   bit 1 of the byte at MapHeader+0x19. This helper preserves every other flag
   and ORs only that bit.

   It stages in EWRAM (gStringVar4+0x140), deliberately avoiding any new IWRAM
   reservation. The probe changes only the currently loaded map header; a map
   load is expected to restore the header from ROM.
*/
final class RunAnywhereNativeHelper {
    static final int ALLOW_RUNNING_OFFSET = 0x19;
    static final int ALLOW_RUNNING_MASK = 0x02;

    private RunAnywhereNativeHelper() {}

    static NativeHelper build(RomProfile rom) {
        long staging = rom.stringVar4 + 0x140;
        long flagsAddress = rom.mapHeader + ALLOW_RUNNING_OFFSET;
        byte[] code = new byte[] {
                0x02, 0x48,             // ldr  r0, [pc,#8] -> flags byte
                0x01, 0x78,             // ldrb r1, [r0]
                0x02, 0x22,             // movs r2, #2
                0x11, 0x43,             // orrs r1, r2
                0x01, 0x70,             // strb r1, [r0]
                0x70, 0x47,             // bx   lr
                0, 0, 0, 0              // flags address
        };
        putU32(code, 0x0C, flagsAddress);
        return new NativeHelper(staging, code);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
