/*
   Compact installer for a small native helper embedded as raw bytes inside a
   relocatable Field Script payload.

   Instead of expanding every helper byte into a 6-byte setptr command, this
   installs a tiny 28-byte Thumb copier with setptr and lets the GBA BIOS
   CpuSet service copy the raw helper block in one operation.

   The copier resolves the physical source using the same sAddressOffset set by
   setvaddress that HotkeyRuntimeV1 already relies on:

       physicalSource = virtualSource - sAddressOffset

   Requirements:
     - source and destination are 4-byte aligned
     - helper length is a multiple of 4
     - helper destination is exactly copierAddress + 0x40

   The last constraint removes a destination literal from the copier and keeps
   the bootstrap at 28 bytes.
*/
final class CpuSetNativeHelperInstaller {
    static final int COPIER_SIZE = 28;
    static final int HELPER_DESTINATION_DELTA = 0x40;

    private static final long S_ADDRESS_OFFSET = 0x020370A8L;

    private CpuSetNativeHelperInstaller() {}

    static byte[] copierBytes(long copierAddress, long virtualSource, int byteLength) {
        long destination = copierAddress + HELPER_DESTINATION_DELTA;

        if ((copierAddress & 3L) != 0) {
            throw new IllegalArgumentException("copierAddress must be 4-byte aligned");
        }
        if ((virtualSource & 3L) != 0) {
            throw new IllegalArgumentException("virtualSource must be 4-byte aligned");
        }
        if ((destination & 3L) != 0) {
            throw new IllegalArgumentException("helper destination must be 4-byte aligned");
        }
        if (byteLength <= 0 || (byteLength & 3) != 0) {
            throw new IllegalArgumentException("CpuSet helper length must be a positive multiple of 4");
        }
        int wordCount = byteLength / 4;
        if (wordCount > 0xFF) {
            throw new IllegalArgumentException("compact installer supports at most 0xFF words");
        }

        /*
           ARM7TDMI Thumb-1, independently assembled for verification:

               ldr  r3, s_address_offset_ptr
               ldr  r3, [r3]
               ldr  r0, virtual_source
               subs r0, r0, r3
               adr  r1, copier + 0x40
               movs r2, #0x40
               lsls r2, r2, #20       ; 0x04000000 = CpuSet 32-bit mode
               adds r2, #wordCount
               svc  #0x0B             ; BIOS CpuSet
               bx   lr
               .align 2
               .word 0x020370A8
               .word virtualSource

           CpuSet control uses bit 26 for 32-bit transfers and the low 21 bits
           for the transfer count. The helper is copied as u32 words.
        */
        byte[] out = new byte[] {
                0x04,0x4B,             // ldr r3,[pc,#16] -> +0x14
                0x1B,0x68,             // ldr r3,[r3]
                0x04,0x48,             // ldr r0,[pc,#16] -> +0x18
                (byte)0xC0,0x1A,       // subs r0,r0,r3
                0x0D,(byte)0xA1,       // adr r1, copier+0x40
                0x40,0x22,             // movs r2,#0x40
                0x12,0x05,             // lsls r2,r2,#20
                (byte)(0x00 | (wordCount & 0xFF)),0x32, // adds r2,#wordCount (patched below)
                0x0B,(byte)0xDF,       // svc #0x0B
                0x70,0x47,             // bx lr
                0,0,0,0,               // sAddressOffset pointer
                0,0,0,0                // virtual raw-helper source
        };

        // Thumb encoding for adds r2,#imm8 is 0x32 imm8 in little endian.
        out[0x0E] = (byte)wordCount;
        out[0x0F] = 0x32;

        putU32(out, 0x14, S_ADDRESS_OFFSET);
        putU32(out, 0x18, virtualSource);

        if (out.length != COPIER_SIZE) {
            throw new IllegalStateException("compact CpuSet copier size mismatch");
        }
        return out;
    }

    static long helperDestination(long copierAddress) {
        return copierAddress + HELPER_DESTINATION_DELTA;
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
